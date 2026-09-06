package com.finance.pluggy.domain;

import com.finance.pluggy.domain.event.ItemSyncedEvent;
import com.finance.pluggy.domain.event.NewTransactionsIngestedEvent;
import com.finance.pluggy.domain.mapper.PluggyDomainMapper;
import com.finance.pluggy.domain.model.*;
import com.finance.pluggy.domain.repository.AccountRepository;
import com.finance.pluggy.domain.repository.ItemRepository;
import com.finance.pluggy.domain.repository.SyncLogRepository;
import com.finance.pluggy.domain.repository.TransactionRepository;
import com.finance.pluggy.domain.service.SyncService;
import com.finance.pluggy.infrastructure.pluggy.client.PluggyClient;
import com.finance.pluggy.infrastructure.pluggy.dto.PluggyBillPaymentResponse;
import com.finance.pluggy.infrastructure.pluggy.dto.PluggyBillResponse;
import com.finance.pluggy.infrastructure.pluggy.dto.PluggyAccountResponse;
import com.finance.pluggy.infrastructure.pluggy.dto.PluggyItemResponse;
import com.finance.pluggy.infrastructure.pluggy.dto.PluggyPageResponse;
import com.finance.pluggy.infrastructure.pluggy.dto.PluggyTransactionResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SyncServiceTest {

    @Mock
    private PluggyClient pluggyClient;

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private SyncLogRepository syncLogRepository;

    @Mock
    private com.finance.pluggy.domain.repository.InvoiceRepository invoiceRepository;

    @Mock
    private PluggyDomainMapper pluggyDomainMapper;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private SyncService syncService;

    @Test
    @DisplayName("Deve sincronizar Item, Account e Transaction com sucesso e marcar SyncLog como SUCCESS")
    void shouldSyncItemAccountAndTransactionsAndPublishEvents() {
        String itemId = "item-pluggy-100";
        String accountId = "account-pluggy-200";
        String transactionId = "tx-pluggy-300";

        PluggyItemResponse itemDto = PluggyItemResponse.builder().id(itemId).status("UPDATED").build();
        PluggyAccountResponse accountDto = PluggyAccountResponse.builder().id(accountId).itemId(itemId).build();
        PluggyTransactionResponse txDto = PluggyTransactionResponse.builder().id(transactionId).accountId(accountId).build();

        PluggyPageResponse<PluggyAccountResponse> accountsPage = PluggyPageResponse.<PluggyAccountResponse>builder()
                .page(1).totalPages(1).results(List.of(accountDto)).build();

        PluggyPageResponse<PluggyTransactionResponse> txPage = PluggyPageResponse.<PluggyTransactionResponse>builder()
                .results(List.of(txDto)).next(null).build();

        Item itemEntity = Item.builder().id(1L).pluggyItemId(itemId).build();
        Account accountEntity = Account.builder().id(2L).pluggyAccountId(accountId).item(itemEntity).build();
        Transaction txEntity = Transaction.builder().id(3L).pluggyTransactionId(transactionId).account(accountEntity).internalCategory(InternalCategory.ALIMENTACAO).build();

        when(pluggyClient.getItem(itemId)).thenReturn(itemDto);
        when(pluggyClient.getAccounts(itemId)).thenReturn(accountsPage);
        when(pluggyClient.getTransactions(accountId, null, null, null)).thenReturn(txPage);

        when(itemRepository.findByPluggyItemId(itemId)).thenReturn(Optional.empty());
        when(accountRepository.findByPluggyAccountId(accountId)).thenReturn(Optional.empty());
        when(transactionRepository.findByPluggyTransactionId(transactionId)).thenReturn(Optional.empty());

        when(pluggyDomainMapper.toItemEntity(itemDto, null)).thenReturn(itemEntity);
        when(pluggyDomainMapper.toAccountEntity(accountDto, itemEntity, null)).thenReturn(accountEntity);
        when(pluggyDomainMapper.toTransactionEntity(txDto, accountEntity, null)).thenReturn(txEntity);

        when(itemRepository.save(any(Item.class))).thenReturn(itemEntity);
        when(accountRepository.save(any(Account.class))).thenReturn(accountEntity);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(txEntity);

        // Execução
        syncService.syncItem(itemId);

        // Validações
        verify(itemRepository).save(itemEntity);
        verify(accountRepository).save(accountEntity);
        verify(transactionRepository).save(txEntity);

        // Valida registro de sucesso no SyncLog
        ArgumentCaptor<SyncLog> syncLogCaptor = ArgumentCaptor.forClass(SyncLog.class);
        verify(syncLogRepository).save(syncLogCaptor.capture());
        assertThat(syncLogCaptor.getValue().getStatus()).isEqualTo(SyncLogStatus.SUCCESS);
    }

    @Test
    @DisplayName("Deve registrar falha e agendar retentativa com backoff em SyncLog se a API Pluggy falhar")
    void shouldRegisterFailureAndBackoffInSyncLogOnApiError() {
        String itemId = "item-failed-999";

        when(pluggyClient.getItem(itemId)).thenThrow(new RuntimeException("API Connection Timeout"));
        when(syncLogRepository.findByPluggyItemId(itemId)).thenReturn(Optional.empty());

        syncService.syncItem(itemId);

        ArgumentCaptor<SyncLog> syncLogCaptor = ArgumentCaptor.forClass(SyncLog.class);
        verify(syncLogRepository).save(syncLogCaptor.capture());

        SyncLog savedLog = syncLogCaptor.getValue();
        assertThat(savedLog.getStatus()).isEqualTo(SyncLogStatus.PENDING);
        assertThat(savedLog.getAttempts()).isEqualTo(1);
        assertThat(savedLog.getLastError()).isEqualTo("API Connection Timeout");
        assertThat(savedLog.getNextAttemptAt()).isNotNull();
    }

    @Test
    @DisplayName("Deve reconciliar status de fatura anterior como PAID ao encontrar pagamento no payments[] da fatura seguinte (regra Pluggy)")
    void shouldReconcileInvoiceStatusUsingNextBillPayments() {
        String itemId = "item-cc-1";
        String accountId = "acc-cc-1";

        PluggyItemResponse itemDto = PluggyItemResponse.builder().id(itemId).status("UPDATED").build();
        PluggyAccountResponse accountDto = PluggyAccountResponse.builder()
                .id(accountId)
                .itemId(itemId)
                .type("CREDIT")
                .subtype("CREDIT_CARD")
                .build();

        PluggyPageResponse<PluggyAccountResponse> accountsPage = PluggyPageResponse.<PluggyAccountResponse>builder()
                .results(List.of(accountDto)).build();

        // Fatura A (Outubro): R$ 745,24 total, sem pagamentos no seu próprio array payments[]
        PluggyBillResponse billADto = PluggyBillResponse.builder()
                .id("bill-october")
                .dueDate("2026-10-10")
                .totalAmount(new BigDecimal("745.24"))
                .payments(Collections.emptyList())
                .build();

        // Fatura B (Novembro): Pagamento de R$ 745,24 referente a Outubro efetuado durante o ciclo de Novembro
        PluggyBillPaymentResponse paymentForOct = PluggyBillPaymentResponse.builder()
                .amount(new BigDecimal("745.24"))
                .date("2026-10-28")
                .build();

        PluggyBillResponse billBDto = PluggyBillResponse.builder()
                .id("bill-november")
                .dueDate("2026-11-10")
                .totalAmount(new BigDecimal("1200.00"))
                .payments(List.of(paymentForOct))
                .build();

        PluggyPageResponse<PluggyBillResponse> billsPage = PluggyPageResponse.<PluggyBillResponse>builder()
                .results(List.of(billADto, billBDto)).build();

        Item itemEntity = Item.builder().id(1L).pluggyItemId(itemId).build();
        Account accountEntity = Account.builder()
                .id(2L)
                .pluggyAccountId(accountId)
                .type(AccountType.CREDIT)
                .subtype(AccountSubtype.CREDIT_CARD)
                .item(itemEntity)
                .build();

        Invoice invoiceAEntity = Invoice.builder()
                .id(100L)
                .pluggyBillId("bill-october")
                .account(accountEntity)
                .dueDate(LocalDate.of(2026, 10, 10))
                .totalAmount(new BigDecimal("745.24"))
                .status("OPEN")
                .build();

        Invoice invoiceBEntity = Invoice.builder()
                .id(101L)
                .pluggyBillId("bill-november")
                .account(accountEntity)
                .dueDate(LocalDate.of(2026, 11, 10))
                .totalAmount(new BigDecimal("1200.00"))
                .status("OPEN")
                .build();

        when(pluggyClient.getItem(itemId)).thenReturn(itemDto);
        when(pluggyClient.getAccounts(itemId)).thenReturn(accountsPage);
        when(pluggyClient.getBills(accountId)).thenReturn(billsPage);
        when(pluggyClient.getTransactions(eq(accountId), any(), any(), any()))
                .thenReturn(PluggyPageResponse.<PluggyTransactionResponse>builder().results(Collections.emptyList()).build());

        when(pluggyDomainMapper.toItemEntity(any(), any())).thenReturn(itemEntity);
        when(pluggyDomainMapper.toAccountEntity(any(), any(), any())).thenReturn(accountEntity);
        when(itemRepository.save(any(Item.class))).thenReturn(itemEntity);
        when(accountRepository.save(any(Account.class))).thenReturn(accountEntity);

        when(pluggyDomainMapper.parseDate("2026-10-10")).thenReturn(LocalDate.of(2026, 10, 10));
        when(pluggyDomainMapper.parseDate("2026-11-10")).thenReturn(LocalDate.of(2026, 11, 10));
        when(pluggyDomainMapper.toInvoiceEntity(eq(billADto), any(), any())).thenReturn(invoiceAEntity);
        when(pluggyDomainMapper.toInvoiceEntity(eq(billBDto), any(), any())).thenReturn(invoiceBEntity);

        when(invoiceRepository.findByAccountIdOrderByDueDateAsc(2L)).thenReturn(List.of(invoiceAEntity, invoiceBEntity));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Executa sincronização do item
        syncService.syncItem(itemId);

        // Valida que a Fatura A (Outubro) foi reconciliada e salva com status PAID
        ArgumentCaptor<Invoice> invoiceCaptor = ArgumentCaptor.forClass(Invoice.class);
        verify(invoiceRepository, atLeast(2)).save(invoiceCaptor.capture());

        List<Invoice> savedInvoices = invoiceCaptor.getAllValues();
        Invoice finalInvoiceA = savedInvoices.stream()
                .filter(inv -> "bill-october".equals(inv.getPluggyBillId()))
                .reduce((first, second) -> second)
                .orElseThrow();

        assertThat(finalInvoiceA.getStatus()).isEqualTo("PAID");
    }

    @Test
    @DisplayName("Deve reconciliar status de fatura como PAID quando houver transação CREDIT correspondente e não existir fatura N+1")
    void shouldReconcileInvoiceStatusUsingCreditTransactionWhenNoNextBillExists() {
        String itemId = "item-cc-2";
        String accountId = "acc-cc-2";

        PluggyItemResponse itemDto = PluggyItemResponse.builder().id(itemId).status("UPDATED").build();
        PluggyAccountResponse accountDto = PluggyAccountResponse.builder()
                .id(accountId)
                .itemId(itemId)
                .type("CREDIT")
                .subtype("CREDIT_CARD")
                .build();

        PluggyPageResponse<PluggyAccountResponse> accountsPage = PluggyPageResponse.<PluggyAccountResponse>builder()
                .results(List.of(accountDto)).build();

        // Fatura única fechada no Pluggy sem pagamentos e sem fatura N+1
        PluggyBillResponse billDto = PluggyBillResponse.builder()
                .id("bill-september")
                .dueDate("2026-09-10")
                .totalAmount(new BigDecimal("500.00"))
                .payments(Collections.emptyList())
                .build();

        PluggyPageResponse<PluggyBillResponse> billsPage = PluggyPageResponse.<PluggyBillResponse>builder()
                .results(List.of(billDto)).build();

        Item itemEntity = Item.builder().id(1L).pluggyItemId(itemId).build();
        Account accountEntity = Account.builder()
                .id(2L)
                .pluggyAccountId(accountId)
                .type(AccountType.CREDIT)
                .subtype(AccountSubtype.CREDIT_CARD)
                .item(itemEntity)
                .build();

        Invoice invoiceEntity = Invoice.builder()
                .id(200L)
                .pluggyBillId("bill-september")
                .account(accountEntity)
                .closeDate(LocalDate.of(2026, 9, 1))
                .dueDate(LocalDate.of(2026, 9, 10))
                .totalAmount(new BigDecimal("500.00"))
                .status("OPEN")
                .build();

        // Transação de pagamento via cartão/crédito (CREDIT) no valor de R$ 500,00
        Transaction paymentTx = Transaction.builder()
                .id(300L)
                .pluggyTransactionId("tx-pay-1")
                .account(accountEntity)
                .type(TransactionType.CREDIT)
                .amount(new BigDecimal("500.00"))
                .date(LocalDate.of(2026, 9, 5))
                .build();

        when(pluggyClient.getItem(itemId)).thenReturn(itemDto);
        when(pluggyClient.getAccounts(itemId)).thenReturn(accountsPage);
        when(pluggyClient.getBills(accountId)).thenReturn(billsPage);
        when(pluggyClient.getTransactions(eq(accountId), any(), any(), any()))
                .thenReturn(PluggyPageResponse.<PluggyTransactionResponse>builder().results(Collections.emptyList()).build());

        when(pluggyDomainMapper.toItemEntity(any(), any())).thenReturn(itemEntity);
        when(pluggyDomainMapper.toAccountEntity(any(), any(), any())).thenReturn(accountEntity);
        when(itemRepository.save(any(Item.class))).thenReturn(itemEntity);
        when(accountRepository.save(any(Account.class))).thenReturn(accountEntity);

        when(pluggyDomainMapper.toInvoiceEntity(eq(billDto), any(), any())).thenReturn(invoiceEntity);
        when(invoiceRepository.findByAccountIdOrderByDueDateAsc(2L)).thenReturn(List.of(invoiceEntity));
        when(transactionRepository.findByAccountId(2L)).thenReturn(List.of(paymentTx));

        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        syncService.syncItem(itemId);

        ArgumentCaptor<Invoice> invoiceCaptor = ArgumentCaptor.forClass(Invoice.class);
        verify(invoiceRepository, atLeastOnce()).save(invoiceCaptor.capture());

        List<Invoice> savedInvoices = invoiceCaptor.getAllValues();
        Invoice finalInvoice = savedInvoices.get(savedInvoices.size() - 1);
        assertThat(finalInvoice.getStatus()).isEqualTo("PAID");
    }
}
