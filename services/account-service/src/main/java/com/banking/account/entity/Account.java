package com.banking.account.entity;

import com.banking.account.model.AccountStatus;
import com.banking.account.model.AccountType;
import com.banking.utility.audit.Auditable;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "accounts")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@SuperBuilder
public class Account extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 20)
    private String accountNo;

    @Column(nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountType accountType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountStatus accountStatus;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    @Column(nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal reservedBalance = BigDecimal.ZERO;

    public BigDecimal getAvailableBalance() {
        return balance.subtract(reservedBalance);
    }

    public boolean canReserve(BigDecimal amount) {
        return getAvailableBalance().compareTo(amount) >= 0;
    }
}