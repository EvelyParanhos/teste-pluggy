package com.finance.pluggy.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_budget_alert_log", indexes = {
    @Index(name = "idx_alert_cat_month", columnList = "category, year_month")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class BudgetAlertLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private InternalCategory category;

    @Column(name = "year_month", length = 7, nullable = false)
    private String yearMonth;

    @Column(name = "monthly_limit", precision = 19, scale = 4, nullable = false)
    private BigDecimal monthlyLimit;

    @Column(name = "total_spent", precision = 19, scale = 4, nullable = false)
    private BigDecimal totalSpent;

    @Column(name = "percentage_used", precision = 5, scale = 2, nullable = false)
    private BigDecimal percentageUsed;

    @Column(name = "alert_message", length = 500, nullable = false)
    private String alertMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
