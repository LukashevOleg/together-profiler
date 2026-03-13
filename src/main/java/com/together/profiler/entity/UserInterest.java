package com.together.profiler.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Интересы пользователя — список выбранных категорий.
 */
@Entity
@Table(name = "user_interests", schema = "profiler",
        uniqueConstraints = @UniqueConstraint(columnNames = {"profile_id", "interest"}))
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class UserInterest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    private UserProfile userProfile;

    @Enumerated(EnumType.STRING)
    @Column(name = "interest", nullable = false, length = 20)
    private Interest interest;
}