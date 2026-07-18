package com.dsa.schedule_manager.user.domain;

import com.dsa.schedule_manager.user.domain.BaseEntity;
import com.dsa.schedule_manager.user.domain.UserRole;
import com.dsa.schedule_manager.user.domain.UserStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false, length = 255)
    private String password; // BCrypt hash

    @Column(nullable = false, length = 50)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status;

    private User(String email, String password, String nickname, UserRole role, UserStatus status) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.role = role;
        this.status = status;
    }

    public static User createNewUser(String email, String encodedPassword, String nickname) {
        return new User(email, encodedPassword, nickname, UserRole.USER, UserStatus.ACTIVE);
    }

    public void block() {
        this.status = UserStatus.BLOCKED;
    }

    public boolean isBlocked() {
        return this.status == UserStatus.BLOCKED;
    }
}