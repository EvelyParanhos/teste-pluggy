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
                .balanceCloseDate(now.minusDays(2))
                .balanceDueDate(now.plusDays(8))
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

    @Test
    @DisplayName("Deve derivar a fatura atual pelo extrato somando transações ocorridas após o último pagamento de cartão")
    void shouldDeriveCurrentInvoiceFromStatementTransactionsAfterLastPayment() {
        LocalDate now = LocalDate.of(2026, 9, 6);
        LocalDate closeDate = LocalDate.of(2026, 9, 3);
        LocalDate dueDate = LocalDate.of(2026, 9, 10);

        Account creditCard = Account.builder()
                .id(1L)
                .name("Cartão Itaú Uniclass")
                .number("9988")
                .type(AccountType.CREDIT)
                .subtype(AccountSubtype.CREDIT_CARD)
                .creditLimit(new BigDecimal("5000.00"))
                .availableCreditLimit(new BigDecimal("4774.23"))
                .balanceCloseDate(closeDate)
                .balanceDueDate(dueDate)
                .build();

        // Transação de pagamento do mês passado (02/08)
        com.finance.pluggy.domain.model.Transaction paymentTx = com.finance.pluggy.domain.model.Transaction.builder()
                .id(100L)
                .pluggyTransactionId("tx-pay-aug")
                .account(creditCard)
                .pluggyCategory("Credit card payment")
                .description("Pagamento de fatura")
                .type(com.finance.pluggy.domain.model.TransactionType.CREDIT)
                .amount(new BigDecimal("503.13"))
                .date(LocalDate.of(2026, 8, 2))
                .build();

        // Transações da fatura atual (entre 03/08 e 03/09)
        com.finance.pluggy.domain.model.Transaction tx1 = com.finance.pluggy.domain.model.Transaction.builder()
                .id(101L)
                .pluggyTransactionId("tx-current-1")
                .account(creditCard)
                .description("Restaurante")
                .type(com.finance.pluggy.domain.model.TransactionType.DEBIT)
                .amount(new BigDecimal("50.00"))
                .date(LocalDate.of(2026, 8, 10))
                .build();

        com.finance.pluggy.domain.model.Transaction tx2 = com.finance.pluggy.domain.model.Transaction.builder()
                .id(102L)
                .pluggyTransactionId("tx-current-2")
                .account(creditCard)
                .description("Farmácia")
                .type(com.finance.pluggy.domain.model.TransactionType.DEBIT)
                .amount(new BigDecimal("75.77"))
                .date(LocalDate.of(2026, 8, 25))
                .build();

        // Transação pós-fechamento (futura)
        com.finance.pluggy.domain.model.Transaction txFuture = com.finance.pluggy.domain.model.Transaction.builder()
                .id(103L)
                .pluggyTransactionId("tx-future-1")
                .account(creditCard)
                .description("Supermercado")
                .type(com.finance.pluggy.domain.model.TransactionType.DEBIT)
                .amount(new BigDecimal("100.00"))
                .date(LocalDate.of(2026, 9, 5))
                .build();

        when(accountRepository.findAll()).thenReturn(List.of(creditCard));
        when(invoiceRepository.findByAccountIdOrderByDueDateAsc(1L)).thenReturn(Collections.emptyList());
        when(transactionRepository.findByAccountId(1L)).thenReturn(List.of(paymentTx, tx1, tx2, txFuture));

        List<InvoiceResponse> invoices = invoiceService.getInvoices();

        assertThat(invoices).hasSize(1);
        InvoiceResponse response = invoices.get(0);

        assertThat(response.getAccountName()).isEqualTo("Cartão Itaú Uniclass");
        assertThat(response.getCurrentBalance()).isEqualByComparingTo("125.77");
        assertThat(response.getFutureBalance()).isEqualByComparingTo("100.00");
        assertThat(response.getTransactionCount()).isEqualTo(2);
        assertThat(response.getTransactions()).containsExactlyInAnyOrder(tx1, tx2);
        assertThat(response.getFutureTransactions()).containsExactly(txFuture);
        assertThat(response.isPendingSync()).isTrue();
        assertThat(response.isCurrent()).isTrue();
    }
}
