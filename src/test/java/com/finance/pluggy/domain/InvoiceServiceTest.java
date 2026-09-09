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

    @Test
    @DisplayName("Deve acionar o fallback por extrato quando a fatura mais recente salva for mais antiga que 32 dias")
    void shouldFallbackToStatementWhenMaxBillDateIsOlderThan32Days() {
        LocalDate now = LocalDate.of(2026, 9, 6);
        LocalDate oldBillCloseDate = LocalDate.of(2026, 8, 3); // 34 dias atrás

        Account creditCard = Account.builder()
                .id(1L)
                .name("Cartão Itaú Direct")
                .number("1122")
                .type(AccountType.CREDIT)
                .subtype(AccountSubtype.CREDIT_CARD)
                .balance(new BigDecimal("402.52"))
                .creditLimit(new BigDecimal("5000.00"))
                .availableCreditLimit(new BigDecimal("4597.48"))
                .balanceCloseDate(now.withDayOfMonth(3))
                .balanceDueDate(now.withDayOfMonth(10))
                .build();

        // Fatura antiga salva em dbInvoices (34 dias atrás)
        com.finance.pluggy.domain.model.Invoice oldInvoice = com.finance.pluggy.domain.model.Invoice.builder()
                .id(10L)
                .pluggyBillId("bill-old")
                .account(creditCard)
                .closeDate(oldBillCloseDate)
                .dueDate(oldBillCloseDate.plusDays(7))
                .totalAmount(new BigDecimal("503.13"))
                .status("OVERDUE")
                .build();

        when(accountRepository.findAll()).thenReturn(List.of(creditCard));
        when(invoiceRepository.findByAccountIdOrderByDueDateAsc(1L)).thenReturn(List.of(oldInvoice));
        when(transactionRepository.findByAccountId(1L)).thenReturn(Collections.emptyList());

        List<InvoiceResponse> invoices = invoiceService.getInvoices();

        // Como a fatura em dbInvoices tem 34 dias (> 32 dias), o gate de confiabilidade rejeita e entra no fallback por extrato
        assertThat(invoices).hasSize(1);
        InvoiceResponse response = invoices.get(0);

        assertThat(response.getAccountName()).isEqualTo("Cartão Itaú Direct");
        assertThat(response.getCurrentBalance()).isEqualByComparingTo("402.52");
        assertThat(response.isPendingSync()).isTrue();
        assertThat(response.isCurrent()).isTrue();
    }

    @Test
    @DisplayName("Deve derivar fatura atual pelo extrato usando pagamento de fatura localizado em conta corrente irmã do mesmo Item")
    void shouldDeriveCurrentInvoiceFromStatementTransactionsUsingLastPaymentInSiblingCheckingAccount() {
        LocalDate closeDate = LocalDate.of(2026, 9, 3);
        LocalDate dueDate = LocalDate.of(2026, 9, 10);

        com.finance.pluggy.domain.model.Item itemEntity = com.finance.pluggy.domain.model.Item.builder()
                .id(10L)
                .pluggyItemId("item-itau-cross")
                .build();

        Account creditCard = Account.builder()
                .id(2L)
                .name("Cartão Itaú Uniclass")
                .number("9988")
                .type(AccountType.CREDIT)
                .subtype(AccountSubtype.CREDIT_CARD)
                .creditLimit(new BigDecimal("5000.00"))
                .availableCreditLimit(new BigDecimal("4597.48"))
                .balanceCloseDate(closeDate)
                .balanceDueDate(dueDate)
                .item(itemEntity)
                .build();

        Account checkingAccount = Account.builder()
                .id(1L)
                .name("Conta Corrente Itaú")
                .type(AccountType.BANK)
                .subtype(AccountSubtype.CHECKING_ACCOUNT)
                .item(itemEntity)
                .build();

        // Transação de pagamento na CONTA CORRENTE (id 1) em 15/08 (referente à fatura anterior de 503.13)
        com.finance.pluggy.domain.model.Transaction checkingPaymentTx = com.finance.pluggy.domain.model.Transaction.builder()
                .id(32L)
                .pluggyTransactionId("tx-chk-pay-aug")
                .account(checkingAccount)
                .pluggyCategory("Credit card payment")
                .description("Pagamento de fatura ... FATURA PAGA")
                .type(com.finance.pluggy.domain.model.TransactionType.DEBIT)
                .amount(new BigDecimal("-503.13"))
                .date(LocalDate.of(2026, 8, 15))
                .build();

        // Transação da fatura atual no cartão (id 2) pós-pagamento (25/08)
        com.finance.pluggy.domain.model.Transaction ccTx1 = com.finance.pluggy.domain.model.Transaction.builder()
                .id(101L)
                .pluggyTransactionId("tx-cc-current-1")
                .account(creditCard)
                .description("Restaurante")
                .type(com.finance.pluggy.domain.model.TransactionType.DEBIT)
                .amount(new BigDecimal("402.52"))
                .date(LocalDate.of(2026, 8, 25))
                .build();

        when(accountRepository.findAll()).thenReturn(List.of(creditCard));
        when(invoiceRepository.findByAccountIdOrderByDueDateAsc(2L)).thenReturn(Collections.emptyList());
        when(accountRepository.findByItemId(10L)).thenReturn(List.of(checkingAccount, creditCard));
        when(transactionRepository.findByAccountId(1L)).thenReturn(List.of(checkingPaymentTx));
        when(transactionRepository.findByAccountId(2L)).thenReturn(List.of(ccTx1));

        List<InvoiceResponse> invoices = invoiceService.getInvoices();

        assertThat(invoices).hasSize(1);
        InvoiceResponse response = invoices.get(0);

        assertThat(response.getAccountName()).isEqualTo("Cartão Itaú Uniclass");
        assertThat(response.getCurrentBalance()).isEqualByComparingTo("402.52");
        assertThat(response.getTransactionCount()).isEqualTo(1);
        assertThat(response.getTransactions()).containsExactly(ccTx1);
        assertThat(response.isPendingSync()).isTrue();
        assertThat(response.isCurrent()).isTrue();
    }

    @Test
    @DisplayName("Deve resolver dia de fechamento e vencimento pela moda histórica de dbInvoices quando a conta tiver datas nulas")
    void shouldResolveCloseAndDueDateFromHistoricalInvoicesModePatternWhenAccountDatesAreNull() {
        LocalDate now = LocalDate.now();

        Account creditCard = Account.builder()
                .id(1L)
                .name("Cartão Itaú Mode Test")
                .number("5544")
                .type(AccountType.CREDIT)
                .subtype(AccountSubtype.CREDIT_CARD)
                .balanceCloseDate(null)
                .balanceDueDate(null)
                .creditLimit(new BigDecimal("5000.00"))
                .availableCreditLimit(new BigDecimal("4500.00"))
                .build();

        // Faturas históricas antigas no DB com fechamento dia 3 e vencimento dia 10 (todas > 32 dias atrás para forçar fallback)
        com.finance.pluggy.domain.model.Invoice inv1 = com.finance.pluggy.domain.model.Invoice.builder()
                .id(1L).pluggyBillId("bill-1").account(creditCard)
                .closeDate(now.minusMonths(3).withDayOfMonth(3))
                .dueDate(now.minusMonths(3).withDayOfMonth(10))
                .build();

        com.finance.pluggy.domain.model.Invoice inv2 = com.finance.pluggy.domain.model.Invoice.builder()
                .id(2L).pluggyBillId("bill-2").account(creditCard)
                .closeDate(now.minusMonths(2).withDayOfMonth(3))
                .dueDate(now.minusMonths(2).withDayOfMonth(10))
                .build();

        com.finance.pluggy.domain.model.Invoice inv3 = com.finance.pluggy.domain.model.Invoice.builder()
                .id(3L).pluggyBillId("bill-3").account(creditCard)
                .closeDate(now.minusDays(40).withDayOfMonth(3))
                .dueDate(now.minusDays(40).withDayOfMonth(10))
                .build();

        when(accountRepository.findAll()).thenReturn(List.of(creditCard));
        when(invoiceRepository.findByAccountIdOrderByDueDateAsc(1L)).thenReturn(List.of(inv1, inv2, inv3));
        when(transactionRepository.findByAccountId(1L)).thenReturn(Collections.emptyList());

        List<InvoiceResponse> invoices = invoiceService.getInvoices();

        assertThat(invoices).hasSize(1);
        InvoiceResponse response = invoices.get(0);

        assertThat(response.getBalanceCloseDate()).isNotNull();
        assertThat(response.getBalanceCloseDate().getDayOfMonth()).isEqualTo(3);
        assertThat(response.getBalanceDueDate()).isNotNull();
        assertThat(response.getBalanceDueDate().getDayOfMonth()).isEqualTo(10);
    }

    @Test
    @DisplayName("Deve excluir transações de parcelamento (NN/NN) datadas entre o fechamento e vencimento anterior")
    void shouldExcludeInstallmentTransactionsBelongingToPreviousCycle() {
        LocalDate closeDate = LocalDate.of(2026, 9, 3);
        LocalDate dueDate = LocalDate.of(2026, 9, 10);

        Account creditCard = Account.builder()
                .id(1L)
                .name("Cartão Itaú Installment Test")
                .number("9988")
                .type(AccountType.CREDIT)
                .subtype(AccountSubtype.CREDIT_CARD)
                .creditLimit(new BigDecimal("5000.00"))
                .availableCreditLimit(new BigDecimal("4874.23"))
                .balanceCloseDate(closeDate)
                .balanceDueDate(dueDate)
                .build();

        // Lançamento de parcela (03/03) lançado em 10/08 (no vencimento do ciclo anterior 03/08 - 10/08)
        com.finance.pluggy.domain.model.Transaction instTx = com.finance.pluggy.domain.model.Transaction.builder()
                .id(100L)
                .pluggyTransactionId("tx-inst-1")
                .account(creditCard)
                .description("Magazineluiza 03/03")
                .type(com.finance.pluggy.domain.model.TransactionType.DEBIT)
                .amount(new BigDecimal("139.88"))
                .date(LocalDate.of(2026, 8, 10))
                .build();

        // Transação legítima do ciclo atual (25/08)
        com.finance.pluggy.domain.model.Transaction currTx = com.finance.pluggy.domain.model.Transaction.builder()
                .id(101L)
                .pluggyTransactionId("tx-curr-1")
                .account(creditCard)
                .description("Restaurante")
                .type(com.finance.pluggy.domain.model.TransactionType.DEBIT)
                .amount(new BigDecimal("125.77"))
                .date(LocalDate.of(2026, 8, 25))
                .build();

        when(accountRepository.findAll()).thenReturn(List.of(creditCard));
        when(invoiceRepository.findByAccountIdOrderByDueDateAsc(1L)).thenReturn(Collections.emptyList());
        when(transactionRepository.findByAccountId(1L)).thenReturn(List.of(instTx, currTx));

        List<InvoiceResponse> invoices = invoiceService.getInvoices();

        assertThat(invoices).hasSize(1);
        InvoiceResponse response = invoices.get(0);

        // A parcela de R$ 139,88 lançada em 10/08 com marcador 03/03 deve ser excluída da fatura atual
        assertThat(response.getCurrentBalance()).isEqualByComparingTo("125.77");
        assertThat(response.getTransactionCount()).isEqualTo(1);
        assertThat(response.getTransactions()).containsExactly(currTx);
    }

    @Test
    @DisplayName("Deve derivar fechamento por gap e ajustar vencimento em feriado/fim de semana quando o vencimento da conta muda")
    void shouldCalculateCloseAndDueDateUsingGapAndAdjustForWeekendOrHolidayWhenDueDateChanges() {
        LocalDate now = LocalDate.now();

        // O usuário mudou a data de vencimento no banco para dia 07/09 (7 de setembro - feriado da Independência)
        LocalDate rawDueDate = LocalDate.of(2026, 9, 7);

        Account creditCard = Account.builder()
                .id(1L)
                .name("Cartão Itaú Gap Test")
                .number("5544")
                .type(AccountType.CREDIT)
                .subtype(AccountSubtype.CREDIT_CARD)
                .balanceCloseDate(null)
                .balanceDueDate(rawDueDate)
                .creditLimit(new BigDecimal("5000.00"))
                .availableCreditLimit(new BigDecimal("4500.00"))
                .build();

        // Faturas históricas no DB que mostram um gap histórico de 7 dias (close 3rd, due 10th -> gap 7)
        com.finance.pluggy.domain.model.Invoice inv1 = com.finance.pluggy.domain.model.Invoice.builder()
                .id(1L).pluggyBillId("bill-1").account(creditCard)
                .closeDate(now.minusMonths(3).withDayOfMonth(3))
                .dueDate(now.minusMonths(3).withDayOfMonth(10))
                .build();

        com.finance.pluggy.domain.model.Invoice inv2 = com.finance.pluggy.domain.model.Invoice.builder()
                .id(2L).pluggyBillId("bill-2").account(creditCard)
                .closeDate(now.minusMonths(2).withDayOfMonth(3))
                .dueDate(now.minusMonths(2).withDayOfMonth(10))
                .build();

        when(accountRepository.findAll()).thenReturn(List.of(creditCard));
        when(invoiceRepository.findByAccountIdOrderByDueDateAsc(1L)).thenReturn(List.of(inv1, inv2));
        when(transactionRepository.findByAccountId(1L)).thenReturn(Collections.emptyList());

        List<InvoiceResponse> invoices = invoiceService.getInvoices();

        assertThat(invoices).hasSize(1);
        InvoiceResponse response = invoices.get(0);

        // Gap é de 7 dias: 07/09/2026 - 7 dias = 31/08/2026
        assertThat(response.getBalanceCloseDate()).isEqualTo(LocalDate.of(2026, 8, 31));

        // Vencimento original 07/09/2026 é feriado da Independência, portanto avança para 08/09/2026 (Terça-feira)
        assertThat(response.getBalanceDueDate()).isEqualTo(LocalDate.of(2026, 9, 8));
    }

    @Test
    @DisplayName("Deve incluir transações do ciclo atual ocorridas antes do pagamento quando este é efetuado no vencimento (meio do novo ciclo)")
    void shouldIncludeTransactionsOccurringBeforePaymentDateWhenPaymentIsMadeMidCycleOnDueDate() {
        LocalDate closeDate = LocalDate.of(2026, 9, 28);
        LocalDate dueDate = LocalDate.of(2026, 10, 5);

        Account creditCard = Account.builder()
                .id(1L)
                .name("Cartão Mid-Cycle Test")
                .number("6058")
                .type(AccountType.CREDIT)
                .subtype(AccountSubtype.CREDIT_CARD)
                .creditLimit(new BigDecimal("5000.00"))
                .availableCreditLimit(new BigDecimal("4386.55"))
                .balanceCloseDate(closeDate)
                .balanceDueDate(dueDate)
                .build();

        com.finance.pluggy.domain.model.Transaction paymentTx = com.finance.pluggy.domain.model.Transaction.builder()
                .id(100L)
                .pluggyTransactionId("tx-pay-mid")
                .account(creditCard)
                .pluggyCategory("Credit card payment")
                .description("Pagamento de fatura")
                .type(com.finance.pluggy.domain.model.TransactionType.CREDIT)
                .amount(new BigDecimal("1500.00"))
                .date(LocalDate.of(2026, 9, 5))
                .build();

        com.finance.pluggy.domain.model.Transaction tx1 = com.finance.pluggy.domain.model.Transaction.builder()
                .id(101L)
                .pluggyTransactionId("tx-current-amazon")
                .account(creditCard)
                .description("Amazon BR VI")
                .type(com.finance.pluggy.domain.model.TransactionType.DEBIT)
                .amount(new BigDecimal("30.91"))
                .date(LocalDate.of(2026, 8, 29))
                .build();

        com.finance.pluggy.domain.model.Transaction tx2 = com.finance.pluggy.domain.model.Transaction.builder()
                .id(102L)
                .pluggyTransactionId("tx-current-claro")
                .account(creditCard)
                .description("Claro Flex")
                .type(com.finance.pluggy.domain.model.TransactionType.DEBIT)
                .amount(new BigDecimal("24.99"))
                .date(LocalDate.of(2026, 9, 10))
                .build();

        when(accountRepository.findAll()).thenReturn(List.of(creditCard));
        when(invoiceRepository.findByAccountIdOrderByDueDateAsc(1L)).thenReturn(Collections.emptyList());
        when(transactionRepository.findByAccountId(1L)).thenReturn(List.of(paymentTx, tx1, tx2));

        List<InvoiceResponse> invoices = invoiceService.getInvoices();

        assertThat(invoices).hasSize(1);
        InvoiceResponse response = invoices.get(0);

        assertThat(response.getCurrentBalance()).isEqualByComparingTo("55.90");
        assertThat(response.getTransactionCount()).isEqualTo(2);
        assertThat(response.getTransactions()).containsExactlyInAnyOrder(tx1, tx2);
    }
}
