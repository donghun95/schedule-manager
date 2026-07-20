package com.dsa.schedule_manager.schedule.controller;

import com.dsa.schedule_manager.auth.service.UserPrincipal;
import com.dsa.schedule_manager.schedule.dto.ScheduleCreateRequest;
import com.dsa.schedule_manager.schedule.dto.ScheduleResponse;
import com.dsa.schedule_manager.schedule.dto.ScheduleUpdateRequest;
import com.dsa.schedule_manager.schedule.service.ScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/schedules")
public class ScheduleController {
    private final ScheduleService scheduleService;
    @PostMapping
    public ResponseEntity<ScheduleResponse> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ScheduleCreateRequest request) {
        ScheduleResponse response = scheduleService.create(principal.getId(),

                request);
        return ResponseEntity
                .created(URI.create("/api/schedules/" + response.id()))
                .body(response);
    }
    @GetMapping("/{id}")
    public ScheduleResponse findById(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        return scheduleService.findById(principal.getId(), id);
    }
    @GetMapping
    public List<ScheduleResponse> findMySchedules(
            @AuthenticationPrincipal UserPrincipal principal) {
        return scheduleService.findMySchedules(principal.getId());
    }
    @PatchMapping("/{id}")
    public ScheduleResponse update(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody ScheduleUpdateRequest request) {
        return scheduleService.update(principal.getId(), id, request);
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        scheduleService.delete(principal.getId(), id);
        return ResponseEntity.noContent().build();
    }
}