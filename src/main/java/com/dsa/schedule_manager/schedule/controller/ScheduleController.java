package com.dsa.schedule_manager.schedule.controller;

import com.dsa.schedule_manager.auth.service.UserPrincipal;
import com.dsa.schedule_manager.schedule.dto.ScheduleCreateRequest;
import com.dsa.schedule_manager.schedule.dto.ScheduleResponse;
import com.dsa.schedule_manager.schedule.dto.ScheduleStatusChangeRequest;
import com.dsa.schedule_manager.schedule.dto.ScheduleUpdateRequest;
import com.dsa.schedule_manager.schedule.service.ScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/schedules")
public class ScheduleController {

    private final ScheduleService scheduleService;

    @Operation(summary = "일정 등록")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "일정 등록 성공"),
            @ApiResponse(responseCode = "400", description = "입력값 검증 실패"),
            @ApiResponse(responseCode = "401", description = "인증 필요")
    })
    @PostMapping
    public ResponseEntity<ScheduleResponse> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ScheduleCreateRequest request) {
        ScheduleResponse response = scheduleService.create(principal.getId(), request);
        return ResponseEntity
                .created(URI.create("/api/schedules/" + response.id()))
                .body(response);
    }

    @Operation(summary = "일정 단건 조회")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "작성자 권한 없음"),
            @ApiResponse(responseCode = "404", description = "일정 없음")
    })
    @GetMapping("/{id}")
    public ScheduleResponse findById(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        return scheduleService.findById(principal.getId(), id);
    }

    @Operation(summary = "내 일정 목록 조회")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요")
    })
    @GetMapping
    public List<ScheduleResponse> findMySchedules(
            @AuthenticationPrincipal UserPrincipal principal) {
        return scheduleService.findMySchedules(principal.getId());
    }

    @Operation(summary = "일정 부분 수정")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(responseCode = "400", description = "입력값 검증 실패"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "작성자 권한 없음"),
            @ApiResponse(responseCode = "404", description = "일정 없음"),
            @ApiResponse(responseCode = "409", description = "수정 버전 충돌")
    })
    @PatchMapping("/{id}")
    public ScheduleResponse update(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody ScheduleUpdateRequest request) {
        return scheduleService.update(principal.getId(), id, request);
    }

    @Operation(summary = "일정 삭제")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "삭제 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "작성자 권한 없음"),
            @ApiResponse(responseCode = "404", description = "일정 없음")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        scheduleService.delete(principal.getId(), id);
        return ResponseEntity.noContent().build();
    }
    @PatchMapping("/{id}/status")
    @Operation(summary = "일정 상태 변경", description = "일정의 상태를 변경하고 변경 이력을 남깁니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "상태 변경 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
            @ApiResponse(responseCode = "403", description = "수정 권한 없음 (작성자만 가능)"),
            @ApiResponse(responseCode = "404", description = "일정을 찾을 수 없음"),
            @ApiResponse(responseCode = "409", description = "허용되지 않는 상태 전이")
    })
    public ResponseEntity<ScheduleResponse> changeStatus(
            @PathVariable Long id,
            @Valid @RequestBody ScheduleStatusChangeRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {

        // 2. ScheduleService의 changeStatus 호출
        ScheduleResponse response = scheduleService.changeStatus(principal.getId(), id, request);

        // 3. 200 OK 응답 반환
        return ResponseEntity.ok(response);
    }

}