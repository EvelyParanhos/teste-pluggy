package com.finance.pluggy.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_category_budget", indexes = {
    @Index(name = "idx_budget_category", columnList = "category", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class CategoryBudget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, unique = true)
    private InternalCategory category;

    @Column(name = "monthly_limit", precision = 19, scale = 4, nullable = false)
    private BigDecimal monthlyLimit;

    @Builder.Default
    @Column(name = "alert_threshold_percentage", precision = 5, scale = 2, nullable = false)
    private BigDecimal alertThresholdPercentage = new BigDecimal("80.00");

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
