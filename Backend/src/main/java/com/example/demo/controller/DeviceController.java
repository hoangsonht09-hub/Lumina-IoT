package com.example.demo.controller;

import com.example.demo.common.ApiResponse;
import com.example.demo.dto.DeviceDtos.CommandStatusResponse;
import com.example.demo.dto.DeviceDtos.ControlAllRequest;
import com.example.demo.dto.DeviceDtos.ControlAllResponse;
import com.example.demo.dto.DeviceDtos.DeviceControlRequest;
import com.example.demo.dto.DeviceDtos.DeviceControlResponse;
import com.example.demo.dto.DeviceDtos.DeviceDto;
import com.example.demo.service.DeviceService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/devices")
public class DeviceController {
    private final DeviceService service;

    public DeviceController(DeviceService service) { this.service = service; }

    @GetMapping
    public ApiResponse<List<DeviceDto>> list() {
        return ApiResponse.ok(service.getDevices());
    }

    @GetMapping("/commands/{commandId}")
    public ApiResponse<CommandStatusResponse> commandStatus(@PathVariable String commandId) {
        return ApiResponse.ok(service.getCommandStatus(commandId));
    }

    @PostMapping("/{id}/control")
    public ApiResponse<DeviceControlResponse> control(@PathVariable String id,
                                                       @Valid @RequestBody DeviceControlRequest request,
                                                       Authentication authentication) {
        DeviceControlResponse result = service.control(id, request, authentication.getName());
        String message = "pending".equals(result.status())
                ? "Đã gửi lệnh tới " + result.device().name()
                : "Điều khiển " + result.device().name() + " thành công";
        return ApiResponse.ok(message, result);
    }

    @PostMapping("/control-all")
    public ApiResponse<ControlAllResponse> controlAll(@Valid @RequestBody ControlAllRequest request,
                                                       Authentication authentication) {
        ControlAllResponse result = service.controlAll(request.isOn(), authentication.getName());
        boolean pending = result.commands().stream().anyMatch(command -> "pending".equals(command.status()));
        String message = pending
                ? "Đã gửi lệnh điều khiển tất cả đèn LED"
                : (request.isOn() ? "Đã bật tất cả đèn LED" : "Đã tắt tất cả đèn LED");
        return ApiResponse.ok(message, result);
    }


}
