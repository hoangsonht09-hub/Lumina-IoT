package com.example.demo.repository;

import com.example.demo.model.Device;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DeviceRepository extends JpaRepository<Device, Long> {
    Optional<Device> findByDeviceCode(String deviceCode);
    List<Device> findAllByOrderByDeviceCode();
    List<Device> findAllByDeviceCodeNotOrderByDeviceCode(String deviceCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select device from Device device where device.deviceCode = :deviceCode")
    Optional<Device> findByDeviceCodeForUpdate(@Param("deviceCode") String deviceCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select device from Device device where device.deviceCode <> 'LED_04' order by device.deviceCode")
    List<Device> findAllForUpdate();
}
