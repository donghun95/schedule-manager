package com.dsa.schedule_manager.user.repository;

import com.dsa.schedule_manager.config.JpaAuditingConfig;
import com.dsa.schedule_manager.user.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaAuditingConfig.class)
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void findByEmail_존재하는_이메일이면_유저를_반환한다() {
        // given
        User user = User.createNewUser("hoon@example.com", "encoded", "동훈");
        userRepository.save(user);

        // when
        Optional<User> found = userRepository.findByEmail("hoon@example.com");

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getNickname()).isEqualTo("동훈");
    }

    @Test
    void existsByEmail_존재하지_않는_이메일이면_false를_반환한다() {
        // when
        boolean exists = userRepository.existsByEmail("none@example.com");

        // then
        assertThat(exists).isFalse();
    }
}