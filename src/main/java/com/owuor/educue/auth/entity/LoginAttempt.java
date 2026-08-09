package com.owuor.educue.auth.entity;
import jakarta.persistence.*;import lombok.Getter;import lombok.Setter;import java.time.LocalDateTime;
@Entity @Table(name="login_attempts") @Getter @Setter
public class LoginAttempt { @Id @Column(name="attempt_key",length=128) private String key; @Column(name="failure_count",nullable=false) private int failureCount; @Column(name="first_failed_at") private LocalDateTime firstFailedAt; @Column(name="locked_until") private LocalDateTime lockedUntil; @Column(name="updated_at",nullable=false) private LocalDateTime updatedAt; }
