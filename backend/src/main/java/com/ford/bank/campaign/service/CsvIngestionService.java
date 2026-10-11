package com.ford.bank.campaign.service;

import com.ford.bank.audit.service.AuditService;
import com.ford.bank.campaign.domain.Campaign;
import com.ford.bank.campaign.domain.CampaignStatus;
import com.ford.bank.campaign.domain.CampaignType;
import com.ford.bank.campaign.dto.CsvUploadResult;
import com.ford.bank.campaign.dto.SampleMessagePreview;
import com.ford.bank.campaign.repository.CampaignRepository;
import com.ford.bank.collections.domain.CollectionCase;
import com.ford.bank.collections.domain.CollectionCaseStatus;
import com.ford.bank.collections.domain.Priority;
import com.ford.bank.collections.repository.CollectionCaseRepository;
import com.ford.bank.communication.domain.CommunicationJob;
import com.ford.bank.communication.domain.JobStatus;
import com.ford.bank.communication.repository.CommunicationJobRepository;
import com.ford.bank.customer.domain.CommunicationChannel;
import com.ford.bank.customer.domain.Customer;
import com.ford.bank.customer.domain.CustomerStatus;
import com.ford.bank.customer.repository.CustomerRepository;
import com.ford.bank.kafka.outbox.OutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class CsvIngestionService {

    private final CustomerRepository customerRepository;
    private final CollectionCaseRepository collectionCaseRepository;
    private final CampaignRepository campaignRepository;
    private final CommunicationJobRepository communicationJobRepository;
    private final OutboxService outboxService;
    private final AuditService auditService;

    private static final String DEFAULT_PAYMENT_URL = "https://pay.fordbank.com/quick/";

    @Transactional
    public CsvUploadResult processDelinquencyCsv(
            MultipartFile file,
            String promptTemplate,
            String channelPreference,
            String toneStrategy,
            String campaignName,
            boolean previewOnly,
            String actorId
    ) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("CSV file must not be empty");
        }

        if (promptTemplate == null || promptTemplate.isBlank()) {
            promptTemplate = "Dear {{customer_name}}, your account {{account_number}} has an overdue balance of INR {{overdue_amount}} due on {{due_date}}. Pay at {{payment_link}} to prevent additional charges.";
        }

        if (campaignName == null || campaignName.isBlank()) {
            campaignName = "Delinquency Batch " + LocalDate.now();
        }

        List<String[]> rows;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            rows = parseCsvLines(reader);
        } catch (Exception e) {
            log.error("Failed to parse CSV file: {}", e.getMessage());
            throw new IllegalArgumentException("Unable to read CSV file: " + e.getMessage());
        }

        if (rows.isEmpty()) {
            throw new IllegalArgumentException("CSV file contains no data rows");
        }

        String[] headers = rows.get(0);
        Map<String, Integer> headerMap = buildHeaderMap(headers);

        List<SampleMessagePreview> samplePreviews = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        int validCount = 0;
        int rejectedCount = 0;

        Campaign campaign = null;
        if (!previewOnly) {
            campaign = new Campaign();
            campaign.setName(campaignName);
            campaign.setCampaignType(CampaignType.COLLECTIONS);
            campaign.setDescription("Automated bulk ingestion via CSV: " + file.getOriginalFilename() + " | Strategy: " + toneStrategy);
            campaign.setStatus(CampaignStatus.RUNNING);
            campaign.setCreatedBy(actorId);
            campaign = campaignRepository.save(campaign);
        }

        int jobsQueued = 0;
        int casesUpdated = 0;

        for (int i = 1; i < rows.size(); i++) {
            String[] row = rows.get(i);
            if (row.length == 0 || (row.length == 1 && row[0].trim().isEmpty())) {
                continue; // Skip empty line
            }

            try {
                ParsedRow data = parseRow(row, headerMap, i + 1);
                validCount++;

                CommunicationChannel selectedChannel = resolveChannel(channelPreference, data.daysPastDue, data.priority);
                String renderedMessage = interpolateTemplate(promptTemplate, data);

                if (samplePreviews.size() < 10 || previewOnly) {
                    samplePreviews.add(new SampleMessagePreview(
                            data.accountNumber,
                            data.customerName,
                            data.phoneNumber,
                            selectedChannel.name(),
                            renderedMessage,
                            data.overdueAmount,
                            data.priority.name()
                    ));
                }

                if (!previewOnly) {
                    // 1. Upsert Customer
                    String custRef = "CUST-" + data.accountNumber.replaceAll("[^a-zA-Z0-9]", "");
                    Customer customer = customerRepository.findByCustomerReference(custRef)
                            .orElseGet(() -> {
                                Customer c = new Customer();
                                c.setCustomerReference(custRef);
                                c.setFullName(data.customerName);
                                c.setPhoneNumber(data.phoneNumber);
                                c.setEmail(data.email != null && !data.email.isBlank() ? data.email : "user." + data.accountNumber + "@example.com");
                                c.setPreferredLanguage("en");
                                c.setStatus(CustomerStatus.ACTIVE);
                                return customerRepository.save(c);
                            });

                    // 2. Upsert Collection Case
                    Optional<CollectionCase> existingCaseOpt = collectionCaseRepository.findByExternalAccountReference(data.accountNumber);
                    CollectionCase collectionCase;
                    if (existingCaseOpt.isPresent()) {
                        collectionCase = existingCaseOpt.get();
                        collectionCase.setOutstandingAmount(data.overdueAmount);
                        collectionCase.setDueDate(data.dueDate);
                        collectionCase.setPriority(data.priority);
                    } else {
                        collectionCase = new CollectionCase();
                        collectionCase.setCustomerId(customer.getId());
                        collectionCase.setExternalAccountReference(data.accountNumber);
                        collectionCase.setOutstandingAmount(data.overdueAmount);
                        collectionCase.setCurrency("INR");
                        collectionCase.setDueDate(data.dueDate);
                        collectionCase.setStatus(CollectionCaseStatus.OPEN);
                        collectionCase.setPriority(data.priority);
                    }
                    collectionCaseRepository.save(collectionCase);
                    casesUpdated++;

                    // 3. Create Communication Job & Outbox Record
                    String idempotencyKey = "CSV-" + campaign.getId() + "-" + data.accountNumber;
                    if (communicationJobRepository.findByIdempotencyKey(idempotencyKey).isEmpty()) {
                        CommunicationJob job = new CommunicationJob();
                        job.setCustomerId(customer.getId());
                        job.setCampaignId(campaign.getId());
                        job.setChannel(selectedChannel);
                        job.setStatus(JobStatus.PENDING);
                        job.setIdempotencyKey(idempotencyKey);
                        job.setScheduledAt(OffsetDateTime.now());
                        job = communicationJobRepository.save(job);
                        jobsQueued++;

                        // Outbox Event -> publishes to Kafka
                        Map<String, Object> payload = new HashMap<>();
                        payload.put("jobId", job.getId().toString());
                        payload.put("customerId", customer.getId().toString());
                        payload.put("channel", selectedChannel.name());
                        payload.put("campaignId", campaign.getId().toString());
                        payload.put("renderedMessage", renderedMessage);
                        payload.put("accountNumber", data.accountNumber);
                        payload.put("overdueAmount", data.overdueAmount.toString());

                        outboxService.publishEvent(
                                "CommunicationJob",
                                job.getId().toString(),
                                "COMMUNICATION_JOB_CREATED",
                                payload
                        );
                    }
                }
            } catch (Exception e) {
                rejectedCount++;
                errors.add("Row " + (i + 1) + ": " + e.getMessage());
            }
        }

        if (!previewOnly && campaign != null) {
            auditService.record(actorId, "CSV_CAMPAIGN_DISPATCHED", "Campaign", campaign.getId().toString(),
                    Map.of("totalRows", validCount + rejectedCount, "validRows", validCount, "jobsQueued", jobsQueued));
        }

        return new CsvUploadResult(
                validCount + rejectedCount,
                validCount,
                rejectedCount,
                campaign != null ? campaign.getId() : null,
                campaignName,
                jobsQueued,
                casesUpdated,
                samplePreviews,
                errors
        );
    }

    private CommunicationChannel resolveChannel(String channelPreference, int daysPastDue, Priority priority) {
        if ("VOICE".equalsIgnoreCase(channelPreference)) {
            return CommunicationChannel.VOICE;
        }
        if ("EMAIL".equalsIgnoreCase(channelPreference)) {
            return CommunicationChannel.EMAIL;
        }
        if ("SMS".equalsIgnoreCase(channelPreference)) {
            return CommunicationChannel.SMS;
        }

        // AUTO SMART ROUTING LOGIC:
        // High severity / DPD > 45 receives Automated Voice Call
        if (daysPastDue > 45 || priority == Priority.HIGH || priority == Priority.CRITICAL) {
            return CommunicationChannel.VOICE;
        }
        return CommunicationChannel.SMS;
    }

    private String interpolateTemplate(String template, ParsedRow data) {
        String paymentLink = DEFAULT_PAYMENT_URL + data.accountNumber;
        return template
                .replace("{{customer_name}}", data.customerName)
                .replace("{{account_number}}", data.accountNumber)
                .replace("{{overdue_amount}}", data.overdueAmount.toPlainString())
                .replace("{{due_date}}", data.dueDate != null ? data.dueDate.toString() : "immediately")
                .replace("{{days_past_due}}", String.valueOf(data.daysPastDue))
                .replace("{{payment_link}}", paymentLink);
    }

    private Map<String, Integer> buildHeaderMap(String[] headers) {
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < headers.length; i++) {
            String norm = normalizeHeader(headers[i]);
            map.put(norm, i);
        }
        return map;
    }

    private String normalizeHeader(String h) {
        if (h == null) return "";
        return h.trim().toLowerCase().replaceAll("[^a-z0-9]", "");
    }

    private ParsedRow parseRow(String[] row, Map<String, Integer> headers, int rowNum) {
        String account = getField(row, headers, "accountnumber", "accountno", "account");
        if (account == null || account.isBlank()) {
            throw new IllegalArgumentException("Account number is required");
        }

        String name = getField(row, headers, "customername", "name", "fullname");
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Customer name is required");
        }

        String phone = getField(row, headers, "phonenumber", "phone", "mobile", "contact");
        if (phone == null || phone.isBlank()) {
            phone = "+919876543210"; // Fallback default demo phone
        }

        String email = getField(row, headers, "email", "emailaddress");

        String amountStr = getField(row, headers, "overdueamount", "amount", "outstandingamount", "balance");
        if (amountStr == null || amountStr.isBlank()) {
            throw new IllegalArgumentException("Overdue amount is required");
        }
        BigDecimal amount;
        try {
            amount = new BigDecimal(amountStr.replaceAll("[^0-9.]", ""));
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid amount format: " + amountStr);
        }

        String dueDateStr = getField(row, headers, "duedate", "due");
        LocalDate dueDate = parseDate(dueDateStr);

        String dpdStr = getField(row, headers, "dayspastdue", "dpd", "overduedays");
        int dpd = 0;
        if (dpdStr != null && !dpdStr.isBlank()) {
            try {
                dpd = Integer.parseInt(dpdStr.replaceAll("[^0-9]", ""));
            } catch (Exception ignored) {}
        }

        String riskTierStr = getField(row, headers, "risktier", "risk", "priority");
        Priority priority = resolvePriority(riskTierStr, dpd);

        return new ParsedRow(account.trim(), name.trim(), phone.trim(), email != null ? email.trim() : null, amount, dueDate, dpd, priority);
    }

    private Priority resolvePriority(String riskTier, int dpd) {
        if (riskTier != null) {
            String u = riskTier.toUpperCase();
            if (u.contains("CRIT")) return Priority.CRITICAL;
            if (u.contains("HIGH")) return Priority.HIGH;
            if (u.contains("LOW")) return Priority.LOW;
        }
        if (dpd > 60) return Priority.CRITICAL;
        if (dpd > 30) return Priority.HIGH;
        if (dpd > 10) return Priority.MEDIUM;
        return Priority.LOW;
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return LocalDate.now().minusDays(5);
        }
        try {
            return LocalDate.parse(dateStr.trim());
        } catch (Exception ignored) {}
        try {
            return LocalDate.parse(dateStr.trim(), DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        } catch (Exception ignored) {}
        try {
            return LocalDate.parse(dateStr.trim(), DateTimeFormatter.ofPattern("MM/dd/yyyy"));
        } catch (Exception ignored) {}
        return LocalDate.now().minusDays(7);
    }

    private String getField(String[] row, Map<String, Integer> map, String... possibleKeys) {
        for (String k : possibleKeys) {
            Integer idx = map.get(k);
            if (idx != null && idx < row.length) {
                return row[idx];
            }
        }
        return null;
    }

    private List<String[]> parseCsvLines(BufferedReader reader) throws Exception {
        List<String[]> list = new ArrayList<>();
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.trim().isEmpty()) continue;
            list.add(parseLine(line));
        }
        return list;
    }

    private String[] parseLine(String line) {
        List<String> tokens = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '\"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                tokens.add(sb.toString().trim());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        tokens.add(sb.toString().trim());
        return tokens.toArray(new String[0]);
    }

    private record ParsedRow(
            String accountNumber,
            String customerName,
            String phoneNumber,
            String email,
            BigDecimal overdueAmount,
            LocalDate dueDate,
            int daysPastDue,
            Priority priority
    ) {}
}
