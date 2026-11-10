package com.example.demo.service;

import com.example.demo.common.PagedResponse;
import com.example.demo.common.BusinessException;
import com.example.demo.common.HistoryTimeFilter;
import com.example.demo.dto.ActionDtos.ActionDto;
import com.example.demo.enums.CommandStatus;
import com.example.demo.enums.DeviceAction;
import com.example.demo.mapper.IotMapper;
import com.example.demo.model.ActionLog;
import com.example.demo.repository.ActionLogRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class ActionService {
    private final ActionLogRepository repository;
    private final IotMapper mapper;

    public ActionService(ActionLogRepository repository, IotMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public PagedResponse<ActionDto> history(String search, String deviceId,
                                             String action, String status, String timeQuery,
                                             OffsetDateTime from, OffsetDateTime to,
                                             int page, int limit, String direction) {
        return history(search, deviceId, action, status, timeQuery,
                from, to, page, limit, direction, null);
    }

    @Transactional(readOnly = true)
    public PagedResponse<ActionDto> history(String search, String deviceId,
                                             String action, String status, String timeQuery,
                                             OffsetDateTime from, OffsetDateTime to,
                                             int page, int limit, String direction, String device) {
        validateTimeRange(from, to);
        boolean disconnectedAction = isDisconnectedAction(action);
        String actionKey = action == null ? "" : normalizeFilterKey(action);
        boolean automaticAction = List.of("autoturnon", "autoturnoff").contains(actionKey);
        boolean manualAction = List.of("manualturnon", "manualturnoff").contains(actionKey);
        String actionToParse = switch (actionKey) {
            case "autoturnon", "manualturnon" -> "turn_on";
            case "autoturnoff", "manualturnoff" -> "turn_off";
            default -> action;
        };
        DeviceAction selectedAction = disconnectedAction ? null : parseAction(actionToParse);
        CommandStatus selectedStatus = parseStatus(status);
        HistoryTimeFilter selectedTime = HistoryTimeFilter.parse(timeQuery);

        Specification<ActionLog> specification = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("device").get("deviceCode")), pattern),
                        cb.like(cb.lower(root.get("device").get("name")), pattern),
                        cb.like(cb.lower(root.get("commandId")), pattern),
                        cb.like(cb.lower(root.get("user").get("username")), pattern),
                        cb.like(cb.lower(root.get("user").get("fullName")), pattern)
                ));
            }
            if (deviceId != null && !deviceId.isBlank()) {
                predicates.add(cb.equal(root.get("device").get("deviceCode"), deviceId.trim()));
            }
            if (device != null && !device.isBlank()) {
                String selectedDevice = device.trim().toLowerCase(Locale.ROOT);
                predicates.add(cb.or(
                        cb.equal(cb.lower(root.get("device").get("deviceCode")), selectedDevice),
                        cb.equal(cb.lower(root.get("device").get("name")), selectedDevice)
                ));
            }
            if (selectedAction != null) predicates.add(cb.equal(root.get("action"), selectedAction));
            if (automaticAction) predicates.add(cb.equal(root.get("device").get("deviceCode"), "LED_04"));
            if (manualAction) predicates.add(cb.notEqual(root.get("device").get("deviceCode"), "LED_04"));
            if (disconnectedAction) predicates.add(cb.equal(root.get("status"), CommandStatus.FAILED));
            if (selectedStatus != null) predicates.add(cb.equal(root.get("status"), selectedStatus));
            if (selectedTime != null) predicates.add(selectedTime.toPredicate(cb, root.get("createdAt")));
            if (from != null) predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from));
            if (to != null) predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), to));
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        Sort.Direction sortDirection = "asc".equalsIgnoreCase(direction)
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sort = Sort.by(sortDirection, "createdAt").and(Sort.by(sortDirection, "id"));
        Page<ActionDto> result = repository.findAll(specification,
                        PageRequest.of(Math.max(0, page - 1), Math.min(Math.max(limit, 1), 100), sort))
                .map(mapper::toActionDto);
        return PagedResponse.from(result);
    }

    private void validateTimeRange(OffsetDateTime from, OffsetDateTime to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_TIME_RANGE",
                    "Thời gian bắt đầu không được sau thời gian kết thúc");
        }
    }

    private DeviceAction parseAction(String action) {
        if (action == null || action.isBlank()) return null;
        String normalized = normalizeFilterKey(action);
        if (List.of("bat", "batthietbi", "turnon", "on").contains(normalized)) {
            return DeviceAction.TURN_ON;
        }
        if (List.of("tat", "tatthietbi", "turnoff", "off").contains(normalized)) {
            return DeviceAction.TURN_OFF;
        }
        if (List.of("doidosang", "changebrightness", "brightness").contains(normalized)) {
            return DeviceAction.CHANGE_BRIGHTNESS;
        }
        try {
            return DeviceAction.valueOf(action.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_DEVICE_ACTION",
                    "Hành động thiết bị không hợp lệ: " + action);
        }
    }

    private boolean isDisconnectedAction(String action) {
        if (action == null || action.isBlank()) return false;
        return List.of("matketnoi", "disconnected", "disconnect")
                .contains(normalizeFilterKey(action));
    }

    private String normalizeFilterKey(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[\\s_-]+", "");
    }

    private CommandStatus parseStatus(String status) {
        if (status == null || status.isBlank()) return null;
        try {
            return CommandStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_COMMAND_STATUS",
                    "Trạng thái lệnh không hợp lệ: " + status);
        }
    }
}
