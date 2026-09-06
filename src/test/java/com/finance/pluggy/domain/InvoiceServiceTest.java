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

    @Mock
    private com.finance.pluggy.domain.repository.InvoiceRepository invoiceRepository;

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

    @Test
    @DisplayName("Deve selecionar a fatura com closeDate mais recente (<= agora) como atual por data, ignorando o status de pagamento de faturas anteriores")
    void shouldSelectMostRecentlyClosedInvoiceAsCurrentByDate() {
        LocalDate now = LocalDate.now();

        Account creditCard = Account.builder()
                .id(1L)
                .name("Cartão Itaú Gold")
                .number("7425")
                .type(AccountType.CREDIT)
                .subtype(AccountSubtype.CREDIT_CARD)
                .creditLimit(new BigDecimal("5000.00"))
                .availableCreditLimit(new BigDecimal("3000.00"))
                .build();

        // Fatura do mês passado presa como OVERDUE (mesmo no passado)
        com.finance.pluggy.domain.model.Invoice pastOverdueInvoice = com.finance.pluggy.domain.model.Invoice.builder()
                .id(10L)
                .pluggyBillId("bill-1")
                .account(creditCard)
                .closeDate(now.minusMonths(1).withDayOfMonth(3))
                .dueDate(now.minusMonths(1).withDayOfMonth(10))
                .totalAmount(new BigDecimal("500.00"))
                .status("OVERDUE")
                .build();

        // Fatura atual fechada recentemente (closeDate <= agora)
        com.finance.pluggy.domain.model.Invoice currentInvoice = com.finance.pluggy.domain.model.Invoice.builder()
                .id(11L)
                .pluggyBillId("bill-2")
                .account(creditCard)
                .closeDate(now.minusDays(2))
                .dueDate(now.plusDays(8))
                .totalAmount(new BigDecimal("1200.00"))
                .status("OPEN")
                .build();

        // Fatura futura (closeDate > agora)
        com.finance.pluggy.domain.model.Invoice futureInvoice = com.finance.pluggy.domain.model.Invoice.builder()
                .id(12L)
                .pluggyBillId("bill-3")
                .account(creditCard)
                .closeDate(now.plusMonths(1).withDayOfMonth(3))
                .dueDate(now.plusMonths(1).withDayOfMonth(10))
                .totalAmount(new BigDecimal("800.00"))
                .status("OPEN")
                .build();

        when(accountRepository.findAll()).thenReturn(List.of(creditCard));
        when(invoiceRepository.findByAccountIdOrderByDueDateAsc(1L)).thenReturn(List.of(pastOverdueInvoice, currentInvoice, futureInvoice));
        when(transactionRepository.findByAccountId(1L)).thenReturn(Collections.emptyList());

        List<InvoiceResponse> invoices = invoiceService.getInvoices();

        // Fatura do mês passado é omitida da lista
        assertThat(invoices).hasSize(2);

        // A fatura selecionada como atual (isCurrent=true) deve ser a com closeDate mais recente <= agora (bill-2)
        InvoiceResponse currentInv = invoices.stream().filter(InvoiceResponse::isCurrent).findFirst().orElseThrow();
        assertThat(currentInv.getBalanceDueDate()).isEqualTo(now.plusDays(8));
        assertThat(currentInv.getCurrentBalance()).isEqualByComparingTo("1200.00");

        // A fatura futura é a de mês seguinte
        InvoiceResponse futureInv = invoices.stream().filter(inv -> !inv.isCurrent()).findFirst().orElseThrow();
        assertThat(futureInv.getBalanceDueDate()).isEqualTo(now.plusMonths(1).withDayOfMonth(10));
    }
}
