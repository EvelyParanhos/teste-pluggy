package com.finance.pluggy.domain.repository;

import com.finance.pluggy.domain.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    Optional<Account> findByPluggyAccountId(String pluggyAccountId);
    List<Account> findByItemId(Long itemId);

    @Query("SELECT SUM(a.balance) FROM Account a")
    BigDecimal sumTotalBalance();

    @Query("SELECT COALESCE(SUM(a.balance), 0) FROM Account a WHERE a.type IN (com.finance.pluggy.domain.model.AccountType.BANK, com.finance.pluggy.domain.model.AccountType.SAVINGS) OR a.subtype IN (com.finance.pluggy.domain.model.AccountSubtype.CHECKING_ACCOUNT, com.finance.pluggy.domain.model.AccountSubtype.SAVINGS_ACCOUNT)")
    BigDecimal sumBankAccountsBalance();

    @Query("SELECT COALESCE(SUM(a.balance), 0) FROM Account a WHERE a.type = com.finance.pluggy.domain.model.AccountType.CREDIT OR a.subtype = com.finance.pluggy.domain.model.AccountSubtype.CREDIT_CARD")
    BigDecimal sumCreditCardBalance();

    @Query("SELECT COALESCE(SUM(a.balance), 0) FROM Account a WHERE a.type = com.finance.pluggy.domain.model.AccountType.INVESTMENT OR a.subtype = com.finance.pluggy.domain.model.AccountSubtype.INVESTMENT_ACCOUNT")
    BigDecimal sumInvestmentBalance();
}
