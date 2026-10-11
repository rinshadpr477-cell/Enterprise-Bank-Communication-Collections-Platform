package com.ford.bank.campaign.dto;

import java.util.List;
import java.util.UUID;

public record CsvUploadResult(
        int totalRows,
        int validRows,
        int rejectedRows,
        UUID campaignId,
        String campaignName,
        int jobsQueued,
        int casesCreatedOrUpdated,
        List<SampleMessagePreview> samplePreviews,
        List<String> errors
) {}
