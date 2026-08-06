package com.dsa.schedule_manager.schedule.service;

import com.dsa.schedule_manager.common.error.BusinessException;
import com.dsa.schedule_manager.common.error.ErrorCode;
import com.dsa.schedule_manager.schedule.domain.Schedule;
import com.dsa.schedule_manager.schedule.domain.ScheduleParticipant;
import com.dsa.schedule_manager.schedule.repository.ScheduleParticipantRepository;
import com.dsa.schedule_manager.schedule.repository.ScheduleRepository;
import com.dsa.schedule_manager.user.domain.User;
import com.dsa.schedule_manager.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class ScheduleParticipantServiceTest {

    @Mock
    ScheduleRepository scheduleRepository;

    @Mock
    ScheduleParticipantRepository participantRepository;

    @Mock
    UserRepository userRepository;

    @InjectMocks
    ScheduleParticipantService sut;

    @Test
    void 작성자는_다른_사용자를_참여자로_추가한다() {
        Schedule schedule = schedule(1L);
        User target = user(2L, "participant@example.com");
        given(scheduleRepository.findById(10L)).willReturn(Optional.of(schedule));
        given(userRepository.findById(2L)).willReturn(Optional.of(target));
        given(participantRepository.existsByScheduleIdAndUserId(10L, 2L)).willReturn(false);
        given(participantRepository.saveAndFlush(org.mockito.ArgumentMatchers.any()))
                .willAnswer(invocation -> invocation.getArgument(0));

        sut.addParticipant(1L, 10L, 2L);

        then(participantRepository).should().saveAndFlush(
                org.mockito.ArgumentMatchers.any(ScheduleParticipant.class));
    }

    @Test
    void 작성자를_참여자로_추가하면_실패한다() {
        given(scheduleRepository.findById(10L)).willReturn(Optional.of(schedule(1L)));

        assertThatThrownBy(() -> sut.addParticipant(1L, 10L, 1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CANNOT_ADD_OWNER_AS_PARTICIPANT);

        then(participantRepository).should(never()).saveAndFlush(
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void 이미_참여_중이면_중복_추가를_막는다() {
        given(scheduleRepository.findById(10L)).willReturn(Optional.of(schedule(1L)));
        given(userRepository.findById(2L)).willReturn(Optional.of(user(2L, "participant@example.com")));
        given(participantRepository.existsByScheduleIdAndUserId(10L, 2L)).willReturn(true);

        assertThatThrownBy(() -> sut.addParticipant(1L, 10L, 2L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PARTICIPANT_ALREADY_EXISTS);
    }

    @Test
    void 작성자가_아니면_참여자를_추가할_수_없다() {
        given(scheduleRepository.findById(10L)).willReturn(Optional.of(schedule(1L)));

        assertThatThrownBy(() -> sut.addParticipant(2L, 10L, 3L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FORBIDDEN);

        then(userRepository).should(never()).findById(any());
        then(participantRepository).should(never()).saveAndFlush(any());
    }

    @Test
    void 존재하지_않는_사용자는_참여자로_추가할_수_없다() {
        given(scheduleRepository.findById(10L)).willReturn(Optional.of(schedule(1L)));
        given(userRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> sut.addParticipant(1L, 10L, 99L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_NOT_FOUND);

        then(participantRepository).should(never()).saveAndFlush(any());
    }

    @Test
    void 작성자가_아니면_참여자를_제거할_수_없다() {
        given(scheduleRepository.findById(10L)).willReturn(Optional.of(schedule(1L)));

        assertThatThrownBy(() -> sut.removeParticipant(2L, 10L, 3L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    void 존재하지_않는_참여자를_제거하면_404다() {
        given(scheduleRepository.findById(10L)).willReturn(Optional.of(schedule(1L)));
        given(participantRepository.findByScheduleIdAndUserId(10L, 3L))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> sut.removeParticipant(1L, 10L, 3L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PARTICIPANT_NOT_FOUND);

        then(participantRepository).should(never()).delete(any());
    }

    @Test
    void 작성자는_참여자를_제거한다() {
        Schedule schedule = schedule(1L);
        ScheduleParticipant participant =
                ScheduleParticipant.of(schedule, user(3L, "remove@example.com"));
        given(scheduleRepository.findById(10L)).willReturn(Optional.of(schedule));
        given(participantRepository.findByScheduleIdAndUserId(10L, 3L))
                .willReturn(Optional.of(participant));

        sut.removeParticipant(1L, 10L, 3L);

        then(participantRepository).should().delete(participant);
    }

    private Schedule schedule(Long ownerId) {
        Schedule schedule = Schedule.create(
                ownerId,
                "일정",
                null,
                LocalDateTime.now().plusDays(1));
        ReflectionTestUtils.setField(schedule, "id", 10L);
        return schedule;
    }

    private User user(Long id, String email) {
        User user = User.createNewUser(email, "encoded-password", "사용자");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
