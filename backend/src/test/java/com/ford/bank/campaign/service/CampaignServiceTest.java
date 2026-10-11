package com.ford.bank.campaign.service;

import com.ford.bank.audit.service.AuditService;
import com.ford.bank.campaign.domain.Campaign;
import com.ford.bank.campaign.domain.CampaignStatus;
import com.ford.bank.campaign.domain.CampaignType;
import com.ford.bank.campaign.dto.CampaignResponse;
import com.ford.bank.campaign.dto.CreateCampaignRequest;
import com.ford.bank.campaign.repository.CampaignRepository;
import com.ford.bank.campaign.repository.CampaignTargetRepository;
import com.ford.bank.common.exception.InvalidStateTransitionException;
import com.ford.bank.customer.repository.CustomerRepository;
import com.ford.bank.kafka.outbox.OutboxService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CampaignServiceTest {

    @Mock
    private CampaignRepository campaignRepository;

    @Mock
    private CampaignTargetRepository campaignTargetRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private OutboxService outboxService;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private CampaignService campaignService;

    private Campaign campaign;
    private UUID campaignId;

    @BeforeEach
    void setUp() {
        campaignId = UUID.randomUUID();
        campaign = new Campaign();
        campaign.setId(campaignId);
        campaign.setName("Diwali EMI Campaign");
        campaign.setCampaignType(CampaignType.PAYMENT_REMINDER);
        campaign.setStatus(CampaignStatus.DRAFT);
        campaign.setDescription("Special EMI reminder for festive season");
        campaign.setCreatedBy("admin-1");
    }

    @Test
    @DisplayName("Should create campaign successfully")
    void createCampaign_Success() {
        CreateCampaignRequest request = new CreateCampaignRequest(
                "Diwali EMI Campaign", CampaignType.PAYMENT_REMINDER,
                "Special EMI reminder for festive season", OffsetDateTime.now().plusDays(1)
        );

        when(campaignRepository.save(any(Campaign.class))).thenReturn(campaign);

        CampaignResponse response = campaignService.createCampaign(request, "admin-1");

        assertThat(response).isNotNull();
        assertThat(response.name()).isEqualTo("Diwali EMI Campaign");
        assertThat(response.status()).isEqualTo(CampaignStatus.DRAFT);
        verify(auditService).record(eq("admin-1"), eq("CAMPAIGN_CREATED"), eq("Campaign"), eq(campaignId.toString()), any());
    }

    @Test
    @DisplayName("Should schedule campaign from DRAFT state and publish outbox event")
    void scheduleCampaign_Success() {
        when(campaignRepository.findById(campaignId)).thenReturn(Optional.of(campaign));
        when(campaignRepository.save(any(Campaign.class))).thenReturn(campaign);

        OffsetDateTime scheduledTime = OffsetDateTime.now().plusDays(2);
        CampaignResponse response = campaignService.scheduleCampaign(campaignId, scheduledTime, "admin-1");

        assertThat(response.status()).isEqualTo(CampaignStatus.SCHEDULED);
        verify(outboxService).publishEvent(eq("Campaign"), eq(campaignId.toString()), eq("CampaignScheduled"), any());
        verify(auditService).record(eq("admin-1"), eq("CAMPAIGN_SCHEDULED"), eq("Campaign"), eq(campaignId.toString()), any());
    }

    @Test
    @DisplayName("Should reject invalid state transition from DRAFT directly to COMPLETED")
    void invalidTransition_DraftToCompleted() {
        assertThatThrownBy(() -> campaign.transitionTo(CampaignStatus.COMPLETED))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessageContaining("Cannot transition");
    }

    @Test
    @DisplayName("Should pause and resume running campaign")
    void pauseAndResumeCampaign_Success() {
        campaign.setStatus(CampaignStatus.RUNNING);
        when(campaignRepository.findById(campaignId)).thenReturn(Optional.of(campaign));
        when(campaignRepository.save(any(Campaign.class))).thenReturn(campaign);

        CampaignResponse paused = campaignService.pauseCampaign(campaignId, "admin-1");
        assertThat(paused.status()).isEqualTo(CampaignStatus.PAUSED);

        CampaignResponse resumed = campaignService.resumeCampaign(campaignId, "admin-1");
        assertThat(resumed.status()).isEqualTo(CampaignStatus.RUNNING);
    }
}
