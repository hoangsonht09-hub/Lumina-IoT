package com.example.demo.repository;

import com.example.demo.enums.CommandStatus;
import com.example.demo.model.ActionLog;
import com.example.demo.model.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface ActionLogRepository extends JpaRepository<ActionLog, Long>, JpaSpecificationExecutor<ActionLog> {
    Optional<ActionLog> findByCommandId(String commandId);
    Optional<ActionLog> findFirstByDeviceAndStatusOrderByCreatedAtAsc(Device device, CommandStatus status);
    boolean existsByDeviceAndStatus(Device device, CommandStatus status);
    List<ActionLog> findByStatusAndCreatedAtBefore(CommandStatus status, OffsetDateTime cutoff);
}
