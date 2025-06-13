package com.juahaki.juahaki.model.otp;

import com.juahaki.juahaki.enums.OtpType;
import com.juahaki.juahaki.model.user.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "otps")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Otp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String otpCode;

    @Column(nullable = false)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OtpType type;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private boolean isUsed = false;

    @Column(nullable = false)
    private int attemptCount = 0;

    @Column(nullable = false)
    private boolean isExpired = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public boolean isExpired() {
        return this.isExpired || LocalDateTime.now().isAfter(this.expiresAt);
    }

    public void markAsUsed() {
        this.isUsed = true;
    }

    public void incrementAttempt() {
        this.attemptCount++;
    }

    public boolean hasExceededMaxAttempts() {
        return this.attemptCount >= 5;
    }

}
