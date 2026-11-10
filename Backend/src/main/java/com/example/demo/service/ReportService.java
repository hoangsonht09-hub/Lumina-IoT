package com.example.demo.service;

import com.example.demo.common.BusinessException;
import com.example.demo.model.ActionLog;
import com.example.demo.model.SensorReading;
import com.example.demo.repository.ActionLogRepository;
import com.example.demo.repository.SensorReadingRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Service
public class ReportService {
    private final SensorReadingRepository sensorRepository;
    private final ActionLogRepository actionRepository;

    public ReportService(SensorReadingRepository sensorRepository, ActionLogRepository actionRepository) {
        this.sensorRepository = sensorRepository;
        this.actionRepository = actionRepository;
    }

    @Transactional(readOnly = true)
    public byte[] exportCsv(String type, LocalDate from, LocalDate to) {
        if ("sensors".equalsIgnoreCase(type)) return sensorCsv(from, to).getBytes(StandardCharsets.UTF_8);
        if ("actions".equalsIgnoreCase(type)) return actionCsv(from, to).getBytes(StandardCharsets.UTF_8);
        throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_REPORT_TYPE", "Loại báo cáo không hợp lệ");
    }

    private String sensorCsv(LocalDate from, LocalDate to) {
        StringBuilder csv = new StringBuilder("id,sensorId,type,value,unit,status,recordedAt\n");
        for (SensorReading item : sensorRepository.findAll()) {
            if (!inRange(item.getRecordedAt(), from, to)) continue;
            csv.append(item.getId()).append(',').append(item.getSensorId()).append(',')
                    .append(item.getType().value()).append(',').append(item.getValue()).append(',')
                    .append(item.getUnit()).append(',').append(item.getStatus().value()).append(',')
                    .append(item.getRecordedAt()).append('\n');
        }
        return csv.toString();
    }

    private String actionCsv(LocalDate from, LocalDate to) {
        StringBuilder csv = new StringBuilder("id,commandId,deviceId,action,status,performedBy,createdAt\n");
        for (ActionLog item : actionRepository.findAll()) {
            if (!inRange(item.getCreatedAt(), from, to)) continue;
            csv.append(item.getId()).append(',').append(item.getCommandId()).append(',')
                    .append(item.getDeviceId()).append(',').append(item.getAction().value()).append(',')
                    .append(item.getStatus().value()).append(',').append(csvCell(item.getPerformedByName())).append(',')
                    .append(item.getCreatedAt()).append('\n');
        }
        return csv.toString();
    }

    private boolean inRange(OffsetDateTime time, LocalDate from, LocalDate to) {
        LocalDate date = time.toLocalDate();
        return (from == null || !date.isBefore(from)) && (to == null || !date.isAfter(to));
    }

    private String csvCell(String value) {
        return '"' + value.replace("\"", "\"\"") + '"';
    }
}
