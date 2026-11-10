package com.example.demo.repository;

import com.example.demo.model.AutoLightEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface AutoLightEventRepository extends JpaRepository<AutoLightEvent, Long> {
    Optional<AutoLightEvent> findTopByOrderByObservedAtDescIdDesc();

    long countByTransitionTrueAndTurnedOnTrueAndObservedAtGreaterThanEqualAndObservedAtLessThan(
            OffsetDateTime fromInclusive, OffsetDateTime toExclusive);
}
