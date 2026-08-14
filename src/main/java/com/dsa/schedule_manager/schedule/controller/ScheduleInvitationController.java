package com.dsa.schedule_manager.schedule.controller;

import com.dsa.schedule_manager.auth.service.UserPrincipal;
import com.dsa.schedule_manager.schedule.dto.ScheduleInvitationResponse;
import com.dsa.schedule_manager.schedule.service.ScheduleParticipantService;
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

    @GetMapping
    public List<ScheduleInvitationResponse> getMyInvitations(
            @AuthenticationPrincipal UserPrincipal principal) {

        return participantService.getMyInvitations(principal.getId());
    }
}