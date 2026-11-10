package com.example.demo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "auto_light_events", indexes = {
        @Index(name = "idx_auto_light_event_time", columnList = "observed_at, is_transition, turned_on")
})
public class AutoLightEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "turned_on", nullable = false)
    private boolean turnedOn;

    @Column(name = "is_transition", nullable = false)
    private boolean transition;

    @Column(name = "observed_at", nullable = false)
    private OffsetDateTime observedAt;

    protected AutoLightEvent() { }

    public AutoLightEvent(boolean turnedOn, boolean transition, OffsetDateTime observedAt) {
        this.turnedOn = turnedOn;
        this.transition = transition;
        this.observedAt = observedAt;
    }

    public boolean isTurnedOn() { return turnedOn; }
    public Long getId() { return id; }
    public boolean isTransition() { return transition; }
    public OffsetDateTime getObservedAt() { return observedAt; }
}
