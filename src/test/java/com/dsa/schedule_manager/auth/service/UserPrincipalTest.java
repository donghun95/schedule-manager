package com.dsa.schedule_manager.auth.service;

import com.dsa.schedule_manager.user.domain.UserRole;
import com.dsa.schedule_manager.user.domain.UserStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserPrincipalTest {

    @Test
    void eraseCredentialsRemovesPasswordHashBeforeSessionSerialization() {
        UserPrincipal principal = new UserPrincipal(
                1L,
                "hoon@example.com",
                "$2a$10$encoded-password",
                "동훈",
                UserRole.USER,
                UserStatus.ACTIVE
        );

        principal.eraseCredentials();

        assertThat(principal.getPassword()).isNull();
    }
}
