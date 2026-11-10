package com.example.demo.service;

import com.example.demo.common.PagedResponse;
import com.example.demo.common.BusinessException;
import com.example.demo.common.HistoryTimeFilter;
import com.example.demo.dto.SensorDtos.RealtimeSensorDto;
import com.example.demo.dto.SensorDtos.RealtimeSensorsResponse;
import com.example.demo.dto.SensorDtos.SensorReadingDto;
import com.example.demo.enums.SensorStatus;
import com.example.demo.enums.SensorType;
import com.example.demo.feature.mqtt.MqttEvents.SensorSnapshot;
import com.example.demo.mapper.IotMapper;
import com.example.demo.model.Sensor;
import com.example.demo.model.SensorReading;
import com.example.demo.repository.SensorReadingRepository;
import com.example.demo.repository.SensorRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Service
public class SensorService {
    private final SensorReadingRepository repository;
    private final SensorRepository sensorRepository;
    private final IotMapper mapper;

    public SensorService(SensorReadingRepository repository, SensorRepository sensorRepository, IotMapper mapper) {
        this.repository = repository;
        this.sensorRepository = sensorRepository;
        this.mapper = mapper;
    }

    @EventListener
    @Transactional
    public void ingest(SensorSnapshot snapshot) {
        List<SensorReading> readings = new ArrayList<>();
        if (snapshot.temperature() != null) {
            readings.add(reading(SensorType.TEMPERATURE, snapshot.temperature(),
                    temperatureStatus(snapshot.temperature()), snapshot.receivedAt()));
        }
        if (snapshot.humidity() != null) {
            readings.add(reading(SensorType.HUMIDITY, snapshot.humidity(),
                    humidityStatus(snapshot.humidity()), snapshot.receivedAt()));
        }
        readings.add(reading(SensorType.LIGHT, snapshot.lightValue(), snapshot.luxReading() ? "lux" : "digital",
                snapshot.lightKnown() ? (snapshot.dark() ? SensorStatus.LOW : SensorStatus.NORMAL)
                        : SensorStatus.WARNING, snapshot.receivedAt()));
        repository.saveAll(readings);
    }

    @Transactional(readOnly = true)
    public RealtimeSensorsResponse realtime() {
        List<RealtimeSensorDto> readings = Arrays.stream(SensorType.values())
                .map(type -> repository.findTopBySensor_TypeOrderByCreatedAtDesc(type).orElse(null))
                .filter(reading -> reading != null)
                .map(mapper::toRealtimeDto)
                .toList();
        return new RealtimeSensorsResponse(readings);
    }

    @Transactional(readOnly = true)
    public PagedResponse<SensorReadingDto> history(String search, String type, Double value,
                                                    String status, String timeQuery,
                                                    OffsetDateTime from, OffsetDateTime to,
                                                    int page, int limit, String direction) {
        validateTimeRange(from, to);
        SensorType selectedType = parseType(type);
        SensorStatus selectedStatus = parseStatus(status);
        HistoryTimeFilter selectedTime = HistoryTimeFilter.parse(timeQuery);
        NumericRange selectedValue = value == null ? null : numericPrefixRange(BigDecimal.valueOf(value));

        Specification<SensorReading> specification = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (search != null && !search.isBlank()) {
                String keyword = search.trim();
                String pattern = "%" + keyword.toLowerCase(Locale.ROOT) + "%";
                List<Predicate> matches = new ArrayList<>();
                matches.add(cb.like(cb.lower(root.get("sensor").get("sensorCode")), pattern));
                matches.add(cb.like(cb.lower(root.get("sensor").get("name")), pattern));
                matches.add(cb.like(cb.lower(root.get("sensor").get("unit")), pattern));

                NumericRange numericValue = parseNumericPrefix(keyword);
                if (numericValue != null) {
                    matches.add(cb.and(
                            cb.greaterThanOrEqualTo(root.<Double>get("value"), numericValue.minimum()),
                            cb.lessThan(root.<Double>get("value"), numericValue.maximumExclusive())
                    ));
                }
                SensorType matchingType = findType(keyword);
                if (matchingType != null) matches.add(cb.equal(root.get("sensor").get("type"), matchingType));
                SensorStatus matchingStatus = findStatus(keyword);
                if (matchingStatus != null) matches.add(cb.equal(root.get("status"), matchingStatus));
                Long readingId = parseReadingId(keyword);
                if (readingId != null) matches.add(cb.equal(root.get("id"), readingId));
                predicates.add(cb.or(matches.toArray(Predicate[]::new)));
            }
            if (selectedType != null) predicates.add(cb.equal(root.get("sensor").get("type"), selectedType));
            if (selectedStatus != null) predicates.add(cb.equal(root.get("status"), selectedStatus));
            if (selectedValue != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.<Double>get("value"), selectedValue.minimum()));
                predicates.add(cb.lessThan(root.<Double>get("value"), selectedValue.maximumExclusive()));
            }
            if (selectedTime != null) predicates.add(selectedTime.toPredicate(cb, root.get("createdAt")));
            if (from != null) predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from));
            if (to != null) predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), to));
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        Sort.Direction sortDirection = "asc".equalsIgnoreCase(direction)
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sort = Sort.by(sortDirection, "createdAt").and(Sort.by(sortDirection, "id"));
        Page<SensorReadingDto> result = repository.findAll(specification,
                        PageRequest.of(Math.max(0, page - 1), Math.min(Math.max(limit, 1), 100), sort))
                .map(mapper::toSensorDto);
        return PagedResponse.from(result);
    }

    private NumericRange parseNumericPrefix(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return numericPrefixRange(new BigDecimal(value.trim().replace(',', '.')));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private NumericRange numericPrefixRange(BigDecimal minimum) {
        BigDecimal normalizedMinimum = minimum.stripTrailingZeros();
        int decimalPlaces = Math.max(normalizedMinimum.scale(), 0);
        BigDecimal step = BigDecimal.ONE.scaleByPowerOfTen(-decimalPlaces);
        return new NumericRange(normalizedMinimum.doubleValue(), normalizedMinimum.add(step).doubleValue());
    }

    private SensorType findType(String value) {
        String normalized = normalizeFilterKey(value);
        return Arrays.stream(SensorType.values())
                .filter(type -> normalizeFilterKey(type.value()).equals(normalized)
                        || normalizeFilterKey(type.label()).contains(normalized))
                .findFirst()
                .orElse(null);
    }

    private SensorStatus findStatus(String value) {
        String normalized = normalizeFilterKey(value);
        return Arrays.stream(SensorStatus.values())
                .filter(status -> normalizeFilterKey(status.value()).equals(normalized))
                .findFirst()
                .orElse(null);
    }

    private Long parseReadingId(String value) {
        String normalized = value.trim().replaceFirst("(?i)^SS-", "").replaceFirst("^#", "");
        try {
            return Long.valueOf(normalized);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private String normalizeFilterKey(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[\\s_-]+", "");
    }

    private void validateTimeRange(OffsetDateTime from, OffsetDateTime to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_TIME_RANGE",
                    "Thời gian bắt đầu không được sau thời gian kết thúc");
        }
    }

    private record NumericRange(double minimum, double maximumExclusive) { }

    private SensorType parseType(String type) {
        if (type == null || type.isBlank()) return null;
        try {
            return SensorType.fromValue(type.trim());
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_SENSOR_TYPE", exception.getMessage());
        }
    }

    private SensorStatus parseStatus(String status) {
        if (status == null || status.isBlank()) return null;
        try {
            return SensorStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_SENSOR_STATUS",
                    "Trạng thái cảm biến không hợp lệ: " + status);
        }
    }

    @Transactional(readOnly = true)
    public SensorReading latest(SensorType type) {
        return repository.findTopBySensor_TypeOrderByCreatedAtDesc(type).orElse(null);
    }

    private SensorReading reading(SensorType type, double value, SensorStatus status, OffsetDateTime time) {
        return reading(type, value, type.unit(), status, time);
    }

    private SensorReading reading(SensorType type, double value, String unit, SensorStatus status, OffsetDateTime time) {
        Sensor sensor = sensorRepository.findByType(type)
                .orElseGet(() -> sensorRepository.save(
                        new Sensor(type.sensorId(), type.label(), type, type.unit())));
        return new SensorReading(sensor, value, unit, status, time);
    }

    private SensorStatus temperatureStatus(double value) {
        if (value < 18) return SensorStatus.LOW;
        if (value > 32) return SensorStatus.WARNING;
        return SensorStatus.NORMAL;
    }

    private SensorStatus humidityStatus(double value) {
        if (value < 40) return SensorStatus.LOW;
        if (value > 75) return SensorStatus.WARNING;
        return SensorStatus.NORMAL;
    }
}
