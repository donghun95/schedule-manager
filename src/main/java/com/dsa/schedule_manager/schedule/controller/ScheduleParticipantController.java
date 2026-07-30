package com.dsa.schedule_manager.schedule.controller;

import com.dsa.schedule_manager.auth.service.UserPrincipal;
import com.dsa.schedule_manager.schedule.dto.ScheduleParticipantResponse;
import com.dsa.schedule_manager.schedule.service.ScheduleParticipantService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/schedules/{id}/participants")
@RequiredArgsConstructor
public class ScheduleParticipantController {

    private final ScheduleParticipantService participantService;

    @PostMapping
    public ResponseEntity<ScheduleParticipantResponse> addParticipant(
            @AuthenticationPrincipal  UserPrincipal principal,
            @PathVariable("id") Long scheduleId,
            @RequestParam Long targetUserId
    ) {
        Long requesterId = principal.getId(); // 로그인한 사용자 (작성자)

        ScheduleParticipantResponse response = participantService.addParticipant(
                requesterId, scheduleId, targetUserId
        );

        // 명세서 규격: 201 Created + Location 헤더
        return ResponseEntity
                .created(URI.create("/api/schedules/%d/participants/%d".formatted(scheduleId, response.userId())))
                .body(response);
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> removeParticipant(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("id") Long scheduleId,
            @PathVariable("userId") Long userId
    ) {
        // TODO: 참여자 삭제 서비스 호출 logic
        return ResponseEntity.noContent().build(); // 204 No Content
    }
}