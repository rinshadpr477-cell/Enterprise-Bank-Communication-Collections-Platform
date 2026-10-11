package com.ford.bank.campaign.domain;

import java.util.Set;

/**
 * Campaign lifecycle states with valid transition rules.
 * This is the source of truth for state machine logic.
 */
public enum CampaignStatus {

    DRAFT {
        @Override
        public Set<CampaignStatus> allowedTransitions() {
            return Set.of(SCHEDULED, CANCELLED);
        }
    },
    SCHEDULED {
        @Override
        public Set<CampaignStatus> allowedTransitions() {
            return Set.of(RUNNING, CANCELLED);
        }
    },
    RUNNING {
        @Override
        public Set<CampaignStatus> allowedTransitions() {
            return Set.of(PAUSED, COMPLETED, CANCELLED);
        }
    },
    PAUSED {
        @Override
        public Set<CampaignStatus> allowedTransitions() {
            return Set.of(RUNNING, CANCELLED);
        }
    },
    COMPLETED {
        @Override
        public Set<CampaignStatus> allowedTransitions() {
            return Set.of(); // Terminal state
        }
    },
    CANCELLED {
        @Override
        public Set<CampaignStatus> allowedTransitions() {
            return Set.of(); // Terminal state
        }
    };

    public abstract Set<CampaignStatus> allowedTransitions();

    public boolean canTransitionTo(CampaignStatus target) {
        return allowedTransitions().contains(target);
    }
}
