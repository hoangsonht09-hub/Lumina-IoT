package com.example.demo.controller;

import com.example.demo.common.ApiResponse;
import com.example.demo.common.PagedResponse;
import com.example.demo.dto.ActionDtos.ActionDto;
import com.example.demo.service.ActionService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;

@RestController
@RequestMapping("/api/actions")
@RequiredArgsConstructor
public class ActionController {

    private final ActionService service;

    @GetMapping
    public ApiResponse<PagedResponse<ActionDto>> history(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String deviceId,
            @RequestParam(required = false) String device,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String timeQuery,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "desc") String direction) {
        return ApiResponse.ok(service.history(search, deviceId, action, status, timeQuery,
                from, to, page, limit, direction, device));
    }
}
