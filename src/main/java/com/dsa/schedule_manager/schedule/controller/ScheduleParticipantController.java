package com.dsa.schedule_manager.schedule.controller;

import com.dsa.schedule_manager.auth.service.UserPrincipal;
import com.dsa.schedule_manager.common.error.ErrorResponse;
import com.dsa.schedule_manager.schedule.dto.ParticipantAddRequest;
import com.dsa.schedule_manager.schedule.dto.ScheduleParticipantDetailResponse;
import com.dsa.schedule_manager.schedule.dto.ScheduleParticipantResponse;
import com.dsa.schedule_manager.schedule.service.ScheduleParticipantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(
        value = "/api/schedules/{scheduleId}/participants",
        produces = MediaType.APPLICATION_JSON_VALUE)
@SecurityRequirement(name = "sessionCookie")
public class ScheduleParticipantController {

    private final ScheduleParticipantService participantService;

    @Operation(summary = "일정 참여자 목록 조회")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "참여자 목록 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "조회 권한 없음",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "일정 없음",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping
    public List<ScheduleParticipantDetailResponse> getParticipants(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long scheduleId) {
        return participantService.getParticipants(principal.getId(), scheduleId);
    }

    @Operation(summary = "일정 참여자 추가")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "참여자 추가 성공"),
            @ApiResponse(responseCode = "400", description = "작성자 본인 추가 또는 입력값 오류",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 필요",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "작성자 권한 없음",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "일정 또는 사용자 없음",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "이미 참여 중인 사용자",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<ScheduleParticipantResponse> addParticipant(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long scheduleId,
            @Valid @RequestBody ParticipantAddRequest request) {
        ScheduleParticipantResponse response = participantService.addParticipant(
                principal.getId(),
                scheduleId,
                request.userId());
        return ResponseEntity
                .created(URI.create("/api/schedules/%d/participants/%d"
                        .formatted(scheduleId, response.userId())))
                .body(response);
    }

    @Operation(summary = "일정 참여자 제거")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "참여자 제거 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "작성자 권한 없음",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "일정 또는 참여자 없음",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> removeParticipant(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long scheduleId,
            @PathVariable Long userId) {
        participantService.removeParticipant(principal.getId(), scheduleId, userId);
        return ResponseEntity.noContent().build();
    }
    @Operation(summary = "일정 참여 초대 수락")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "초대 수락 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "참여 초대 없음",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "현재 상태에서는 수락 불가",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/me/accept")
    public ResponseEntity<Void> acceptInvitation(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long scheduleId) {

        participantService.acceptInvitation(
                principal.getId(),
                scheduleId
        );

        return ResponseEntity.noContent().build();
    }


    @Operation(summary = "일정 참여 초대 거절")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "초대 거절 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "참여 초대 없음",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "현재 상태에서는 거절 불가",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    // 거절
    @PatchMapping("/me/reject")
    public ResponseEntity<Void> rejectInvitation(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long scheduleId) {

        participantService.rejectInvitation(
                principal.getId(),
                scheduleId
        );

        return ResponseEntity.noContent().build();
    }
}
