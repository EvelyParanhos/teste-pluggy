package com.finance.pluggy.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tb_account", indexes = {
    @Index(name = "idx_account_pluggy_id", columnList = "pluggy_account_id", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pluggy_account_id", nullable = false, unique = true)
    private String pluggyAccountId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    @JsonIgnoreProperties({"accounts", "hibernateLazyInitializer", "handler"})
    private Item item;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "marketing_name")
    private String marketingName;

    @Column(name = "number")
    private String number;

    @Column(name = "agency")
    private String agency;

    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private AccountType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "subtype")
    private AccountSubtype subtype;

    @Column(name = "balance", precision = 19, scale = 4)
    private BigDecimal balance;

    @Column(name = "currency_code", length = 10)
    private String currencyCode;

    @Column(name = "credit_limit", precision = 19, scale = 4)
    private BigDecimal creditLimit;

    @Column(name = "available_credit_limit", precision = 19, scale = 4)
    private BigDecimal availableCreditLimit;

    @Column(name = "balance_close_date")
    private java.time.LocalDate balanceCloseDate;

    @Column(name = "balance_due_date")
    private java.time.LocalDate balanceDueDate;

    @Column(name = "minimum_payment_amount", precision = 19, scale = 4)
    private BigDecimal minimumPaymentAmount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @JsonIgnoreProperties({"account", "hibernateLazyInitializer", "handler"})
    private List<Transaction> transactions = new ArrayList<>();

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
