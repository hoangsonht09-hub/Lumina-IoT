package com.example.demo.controller;

import com.example.demo.common.ApiResponse;
import com.example.demo.common.PagedResponse;
import com.example.demo.dto.SensorDtos.RealtimeSensorsResponse;
import com.example.demo.dto.SensorDtos.SensorReadingDto;
import com.example.demo.service.SensorService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;

@RestController
@RequestMapping("/api/sensors")
public class SensorController {
    private final SensorService service;

    public SensorController(SensorService service) { this.service = service; }

    @GetMapping("/realtime")
    public ApiResponse<RealtimeSensorsResponse> realtime() {
        return ApiResponse.ok(service.realtime());
    }

    @GetMapping("/history")
    public ApiResponse<PagedResponse<SensorReadingDto>> history(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Double value,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String timeQuery,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "desc") String direction) {
        return ApiResponse.ok(service.history(search, type, value, status, timeQuery,
                from, to, page, limit, direction));
    }
}
