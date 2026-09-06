package com.finance.pluggy.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_invoice", indexes = {
    @Index(name = "idx_invoice_pluggy_id", columnList = "pluggy_bill_id", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pluggy_bill_id", nullable = false, unique = true)
    private String pluggyBillId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    @JsonIgnoreProperties({"transactions", "item", "hibernateLazyInitializer", "handler"})
    private Account account;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "close_date")
    private LocalDate closeDate;

    @Column(name = "total_amount", precision = 19, scale = 4)
    private BigDecimal totalAmount;

    @Column(name = "total_balance", precision = 19, scale = 4)
    private BigDecimal totalBalance;

    @Column(name = "minimum_payment_amount", precision = 19, scale = 4)
    private BigDecimal minimumPaymentAmount;

    @Column(name = "status", length = 30)
    private String status; // OPEN, CLOSED, OVERDUE, PAID

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
