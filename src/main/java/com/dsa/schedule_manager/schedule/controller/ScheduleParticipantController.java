package com.dsa.schedule_manager.schedule.controller;

import com.dsa.schedule_manager.auth.service.UserPrincipal;
import com.dsa.schedule_manager.schedule.domain.Schedule;
import com.dsa.schedule_manager.schedule.domain.ScheduleParticipant;
import com.dsa.schedule_manager.schedule.dto.ScheduleParticipantResponse;
import com.dsa.schedule_manager.schedule.service.ScheduleParticipantService;
import com.dsa.schedule_manager.schedule.service.ScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@Tag(name = "Schedule Participant", description = "일정 참여자 관리 API")
@RestController
@RequestMapping("/api/schedules/{id}/participants")
@RequiredArgsConstructor
public class ScheduleParticipantController {

    private final ScheduleParticipantService participantService;


    @Operation(summary = "참여자 추가", description = "일정 작성자가 새로운 참여자를 추가합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "참여자 추가 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "작성자 권한 없음 (작성자만 추가 가능)"),
            @ApiResponse(responseCode = "404", description = "일정 또는 대상 사용자를 찾을 수 없음"),
            @ApiResponse(responseCode = "409", description = "이미 등록된 참여자이거나 작성자를 참여자로 추가 시도")
    })
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
    @Operation(summary = "참여자 제거", description = "일정 작성자가 특정 참여자를 제거합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "참여자 제거 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "작성자 권한 없음 (작성자만 제거 가능)"),
            @ApiResponse(responseCode = "404", description = "일정 또는 제거할 참여자를 찾을 수 없음")
    })

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> removeParticipant(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("id") Long scheduleId,
            @PathVariable("userId") Long userId

    ) {

        participantService.removeParticipant(scheduleId, userId, principal.getId());
        return ResponseEntity.noContent().build(); // 204 No Content
    }
}