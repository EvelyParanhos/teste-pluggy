package com.finance.pluggy.domain;

import com.finance.pluggy.domain.model.Account;
import com.finance.pluggy.domain.model.AccountSubtype;
import com.finance.pluggy.domain.model.AccountType;
import com.finance.pluggy.domain.repository.AccountRepository;
import com.finance.pluggy.domain.repository.TransactionRepository;
import com.finance.pluggy.domain.service.InvoiceService;
import com.finance.pluggy.infrastructure.rest.dto.InvoiceResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private InvoiceService invoiceService;

    @Test
    @DisplayName("Deve listar faturas apenas de contas do tipo CREDIT ou CREDIT_CARD com creditData")
    void shouldGetInvoicesForCreditCardAccounts() {
        Account creditCard = Account.builder()
                .id(1L)
                .name("Cartão Itaú Gold")
                .number("7425")
                .type(AccountType.CREDIT)
                .subtype(AccountSubtype.CREDIT_CARD)
                .balance(new BigDecimal("1500.00"))
                .creditLimit(new BigDecimal("5000.00"))
                .availableCreditLimit(new BigDecimal("3500.00"))
                .balanceCloseDate(LocalDate.now().plusDays(5))
                .balanceDueDate(LocalDate.now().plusDays(15))
                .build();

        Account bankAccount = Account.builder()
                .id(2L)
                .name("Conta Corrente Nubank")
                .type(AccountType.BANK)
                .subtype(AccountSubtype.CHECKING_ACCOUNT)
                .balance(new BigDecimal("2000.00"))
                .build();

        when(accountRepository.findAll()).thenReturn(List.of(creditCard, bankAccount));
        when(transactionRepository.findByAccountId(1L)).thenReturn(Collections.emptyList());

        List<InvoiceResponse> invoices = invoiceService.getInvoices();

        assertThat(invoices).hasSize(1);
        InvoiceResponse inv = invoices.get(0);
        assertThat(inv.getAccountName()).isEqualTo("Cartão Itaú Gold");
        assertThat(inv.getMaskedNumber()).isEqualTo("xxxx 7425");
        assertThat(inv.getCurrentBalance()).isEqualByComparingTo("1500.00");
        assertThat(inv.getCreditLimit()).isEqualByComparingTo("5000.00");
        assertThat(inv.getAvailableCreditLimit()).isEqualByComparingTo("3500.00");
        assertThat(inv.getUtilizationPercentage()).isEqualByComparingTo("30.0");
        assertThat(inv.getStatus()).isEqualTo("OPEN");
    }
}
