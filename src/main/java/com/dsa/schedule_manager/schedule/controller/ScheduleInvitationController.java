package com.dsa.schedule_manager.schedule.controller;

import com.dsa.schedule_manager.auth.service.UserPrincipal;
import com.dsa.schedule_manager.common.error.ErrorResponse;
import com.dsa.schedule_manager.schedule.dto.ScheduleInvitationResponse;
import com.dsa.schedule_manager.schedule.service.ScheduleParticipantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/schedules/invitations")
@SecurityRequirement(name = "sessionCookie")
public class ScheduleInvitationController {

    private final ScheduleParticipantService participantService;

    @Operation(
            summary = "내 초대 목록 조회",
            description = "로그인한 사용자에게 온 PLANNED 일정의 PENDING 초대 목록을 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "초대 목록 조회 성공"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 필요",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    @GetMapping
    public List<ScheduleInvitationResponse> getMyInvitations(
            @AuthenticationPrincipal UserPrincipal principal) {

        return participantService.getMyInvitations(principal.getId());
    }
}
