package com.ford.bank.kafka.consumer;

import java.io.Serializable;
import java.util.Objects;

public class ProcessedEventId implements Serializable {
    private String eventId;
    private String consumerName;

    public ProcessedEventId() {}

    public ProcessedEventId(String eventId, String consumerName) {
        this.eventId = eventId;
        this.consumerName = consumerName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProcessedEventId)) return false;
        ProcessedEventId that = (ProcessedEventId) o;
        return Objects.equals(eventId, that.eventId) && Objects.equals(consumerName, that.consumerName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventId, consumerName);
    }
}
