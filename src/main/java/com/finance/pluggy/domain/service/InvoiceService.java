package com.finance.pluggy.domain.service;

import com.finance.pluggy.domain.model.Account;
import com.finance.pluggy.domain.model.AccountSubtype;
import com.finance.pluggy.domain.model.AccountType;
import com.finance.pluggy.domain.model.Transaction;
import com.finance.pluggy.domain.repository.AccountRepository;
import com.finance.pluggy.domain.repository.TransactionRepository;
import com.finance.pluggy.infrastructure.rest.dto.InvoiceHistoryItem;
import com.finance.pluggy.infrastructure.rest.dto.InvoiceResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.finance.pluggy.domain.model.TransactionType;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final com.finance.pluggy.domain.repository.InvoiceRepository invoiceRepository;

    /**
     * Retorna a lista de faturas ativas e projetadas para todos os cartões de crédito.
     */
    @Transactional(readOnly = true)
    public List<InvoiceResponse> getInvoices() {
        List<Account> allAccounts = accountRepository.findAll();
        List<InvoiceResponse> invoices = new ArrayList<>();
        LocalDate now = LocalDate.now();

        for (Account acc : allAccounts) {
            if (isCreditCard(acc)) {
                List<com.finance.pluggy.domain.model.Invoice> dbInvoices =
                        invoiceRepository.findByAccountIdOrderByDueDateAsc(acc.getId());

                String maskedNum = acc.getNumber() != null && acc.getNumber().length() >= 4
                        ? "xxxx " + acc.getNumber().substring(acc.getNumber().length() - 4)
                        : "xxxx 0000";

                BigDecimal limit = acc.getCreditLimit() != null && acc.getCreditLimit().compareTo(BigDecimal.ZERO) > 0
                        ? acc.getCreditLimit()
                        : new BigDecimal("5000.00");

                boolean hasReliableBills = false;
                if (dbInvoices != null && !dbInvoices.isEmpty()) {
                    LocalDate maxBillDate = dbInvoices.stream()
                            .map(inv -> inv.getCloseDate() != null ? inv.getCloseDate() : inv.getDueDate())
                            .filter(java.util.Objects::nonNull)
                            .max(Comparator.naturalOrder())
                            .orElse(null);

                    hasReliableBills = maxBillDate != null
                            && java.time.temporal.ChronoUnit.DAYS.between(maxBillDate, now) <= 32;
                }

                java.util.Optional<com.finance.pluggy.domain.model.Invoice> unpaidCurrent = dbInvoices.stream()
                        .filter(inv -> !"PAID".equalsIgnoreCase(inv.getStatus()))
                        .filter(inv -> inv.getCloseDate() != null && !inv.getCloseDate().isAfter(now))
                        .max(Comparator.comparing(com.finance.pluggy.domain.model.Invoice::getCloseDate));

                if (hasReliableBills && unpaidCurrent.isPresent()) {
                    com.finance.pluggy.domain.model.Invoice currentInvoice = unpaidCurrent.get();

                    List<Transaction> accountTxs = transactionRepository.findByAccountId(acc.getId());

                    // Retorna apenas a fatura atual (isCurrent = true) e faturas futuras/projetadas
                    for (int idx = dbInvoices.size() - 1; idx >= 0; idx--) {
                        com.finance.pluggy.domain.model.Invoice inv = dbInvoices.get(idx);
                        int originalIndex = idx;
                        LocalDate previousInvoiceCloseDate = (originalIndex > 0)
                                ? dbInvoices.get(originalIndex - 1).getCloseDate()
                                : null;

                        boolean isCurrent = inv.getId() != null && inv.getId().equals(currentInvoice.getId());
                        boolean isFuture = false;
                        if (!isCurrent) {
                            if (inv.getCloseDate() != null && currentInvoice.getCloseDate() != null) {
                                isFuture = inv.getCloseDate().isAfter(currentInvoice.getCloseDate());
                            } else if (inv.getDueDate() != null && currentInvoice.getDueDate() != null) {
                                isFuture = inv.getDueDate().isAfter(currentInvoice.getDueDate());
                            } else if (inv.getDueDate() != null) {
                                isFuture = inv.getDueDate().isAfter(now);
                            }
                        }

                        // Faturas passadas são omitidas da lista exposta no endpoint /invoices
                        if (!isCurrent && !isFuture) {
                            continue;
                        }

                        List<Transaction> invTxs = new ArrayList<>();
                        List<Transaction> futureTxs = new ArrayList<>();
                        BigDecimal futureBalance = BigDecimal.ZERO;

                        for (Transaction tx : accountTxs) {
                            String txBillId = tx.getPluggyBillId();
                            LocalDate txDate = tx.getDate();

                            if (txBillId != null && !txBillId.isBlank()) {
                                if (txBillId.equals(inv.getPluggyBillId())) {
                                    invTxs.add(tx);
                                } else if (isCurrent && inv.getCloseDate() != null && txDate != null && txDate.isAfter(inv.getCloseDate())) {
                                    futureTxs.add(tx);
                                    if (tx.getType() == TransactionType.DEBIT && tx.getAmount() != null) {
                                        futureBalance = futureBalance.add(tx.getAmount().abs());
                                    }
                                }
                            } else {
                                if (inv.getCloseDate() != null && txDate != null && txDate.isAfter(inv.getCloseDate())) {
                                    if (isCurrent) {
                                        futureTxs.add(tx);
                                        if (tx.getType() == TransactionType.DEBIT && tx.getAmount() != null) {
                                            futureBalance = futureBalance.add(tx.getAmount().abs());
                                        }
                                    }
                                } else if (previousInvoiceCloseDate != null && txDate != null) {
                                    if (txDate.isAfter(previousInvoiceCloseDate)) {
                                        invTxs.add(tx);
                                    }
                                } else {
                                    invTxs.add(tx);
                                }
                            }
                        }

                        BigDecimal invBalance = inv.getTotalAmount() != null
                                ? inv.getTotalAmount()
                                : (inv.getTotalBalance() != null ? inv.getTotalBalance() : BigDecimal.ZERO);

                        BigDecimal availableLimit = acc.getAvailableCreditLimit() != null
                                ? acc.getAvailableCreditLimit()
                                : limit.subtract(invBalance.add(futureBalance)).max(BigDecimal.ZERO);

                        BigDecimal totalUsedLimit = limit.subtract(availableLimit).max(BigDecimal.ZERO);
                        BigDecimal utilizationPct = limit.compareTo(BigDecimal.ZERO) > 0
                                ? totalUsedLimit.multiply(new BigDecimal("100")).divide(limit, 1, RoundingMode.HALF_UP)
                                : BigDecimal.ZERO;

                        invoices.add(InvoiceResponse.builder()
                                .accountId(acc.getId())
                                .accountName(acc.getName() != null ? acc.getName() : "Cartão de Crédito")
                                .maskedNumber(maskedNum)
                                .status(inv.getStatus() != null ? inv.getStatus() : "OPEN")
                                .currentBalance(invBalance)
                                .futureBalance(futureBalance)
                                .totalUsedLimit(totalUsedLimit)
                                .creditLimit(limit)
                                .availableCreditLimit(availableLimit)
                                .utilizationPercentage(utilizationPct)
                                .balanceCloseDate(inv.getCloseDate())
                                .balanceDueDate(inv.getDueDate())
                                .minimumPaymentAmount(inv.getMinimumPaymentAmount())
                                .transactionCount(invTxs.size())
                                .transactions(invTxs)
                                .futureTransactions(futureTxs)
                                .pendingSync(false)
                                .isCurrent(isCurrent)
                                .build());
                    }
                } else {
                    // Fallback por extrato para contas sem faturas salvas ou conectores sem GET /bills confiável (ex: Itaú)
                    List<Transaction> accountTxs = transactionRepository.findByAccountId(acc.getId());

                    List<Account> itemAccounts = (acc.getItem() != null && acc.getItem().getId() != null)
                            ? accountRepository.findByItemId(acc.getItem().getId())
                            : List.of(acc);

                    List<Transaction> itemTxs = new ArrayList<>();
                    for (Account itemAcc : itemAccounts) {
                        List<Transaction> txs = transactionRepository.findByAccountId(itemAcc.getId());
                        if (txs != null) {
                            itemTxs.addAll(txs);
                        }
                    }

                    // Identifica a transação de pagamento de fatura mais recente no extrato (em todas as contas do mesmo item)
                    Transaction lastPaymentTx = itemTxs.stream()
                            .filter(this::isCreditCardPayment)
                            .max(Comparator.comparing(Transaction::getDate, Comparator.nullsFirst(Comparator.naturalOrder())))
                            .orElse(null);

                    LocalDate lastPaymentDate = lastPaymentTx != null ? lastPaymentTx.getDate() : null;

                    int gapDays = resolveCloseDueGapDays(dbInvoices);

                    LocalDate rawDueDate = acc.getBalanceDueDate();
                    if (rawDueDate == null && dbInvoices != null && !dbInvoices.isEmpty()) {
                        Integer histDueDay = dbInvoices.stream()
                                .filter(inv -> inv.getDueDate() != null)
                                .collect(Collectors.groupingBy(inv -> inv.getDueDate().getDayOfMonth(), Collectors.counting()))
                                .entrySet().stream()
                                .max(Map.Entry.comparingByValue())
                                .map(Map.Entry::getKey)
                                .orElse(null);
                        if (histDueDay != null) {
                            LocalDate targetMonth = now.getDayOfMonth() <= histDueDay ? now : now.plusMonths(1);
                            int safeDay = Math.min(histDueDay, targetMonth.lengthOfMonth());
                            rawDueDate = targetMonth.withDayOfMonth(safeDay);
                        }
                    }

                    if (rawDueDate == null) {
                        rawDueDate = now.plusDays(10);
                    }

                    LocalDate closeDate = acc.getBalanceCloseDate();
                    if (closeDate == null) {
                        closeDate = rawDueDate.minusDays(gapDays);
                    }

                    LocalDate dueDate = adjustForWeekendAndHoliday(rawDueDate);

                    LocalDate previousCloseDate = closeDate.minusMonths(1);
                    LocalDate previousDueDate = (lastPaymentDate != null && lastPaymentDate.isAfter(previousCloseDate) && !lastPaymentDate.isAfter(closeDate))
                            ? lastPaymentDate
                            : adjustForWeekendAndHoliday(rawDueDate.minusMonths(1));

                    if (previousDueDate != null && !previousDueDate.isAfter(previousCloseDate)) {
                        previousDueDate = previousDueDate.plusMonths(1);
                    }

                    List<Transaction> currentTxs = new ArrayList<>();
                    List<Transaction> futureTxs = new ArrayList<>();
                    BigDecimal currentBalance = BigDecimal.ZERO;
                    BigDecimal futureBalance = BigDecimal.ZERO;

                    for (Transaction tx : accountTxs) {
                        if (lastPaymentTx != null && tx.getId() != null && tx.getId().equals(lastPaymentTx.getId())) {
                            // Exclui o lançamento do próprio pagamento da soma da fatura atual
                            continue;
                        }

                        if (belongsToPreviousCycle(tx, previousCloseDate, previousDueDate)) {
                            // Transações de parcela (NN/NN) lançadas entre o fechamento e o vencimento do ciclo anterior pertencem à fatura anterior
                            continue;
                        }

                        BigDecimal txAmount = tx.getAmount() != null ? tx.getAmount().abs() : BigDecimal.ZERO;
                        LocalDate txDate = tx.getDate();

                        if (txDate != null && txDate.isAfter(closeDate)) {
                            futureTxs.add(tx);
                            if (tx.getType() == TransactionType.DEBIT) {
                                futureBalance = futureBalance.add(txAmount);
                            } else if (tx.getType() == TransactionType.CREDIT) {
                                futureBalance = futureBalance.subtract(txAmount);
                            }
                        } else if (previousCloseDate != null && txDate != null && !txDate.isAfter(previousCloseDate)) {
                            // Transações ocorridas até o fechamento do ciclo anterior pertencem ao ciclo anterior
                            continue;
                        } else {
                            // Transações no intervalo após o último pagamento até a data de fechamento
                            currentTxs.add(tx);
                            if (tx.getType() == TransactionType.DEBIT) {
                                currentBalance = currentBalance.add(txAmount);
                            } else if (tx.getType() == TransactionType.CREDIT) {
                                currentBalance = currentBalance.subtract(txAmount);
                            }
                        }
                    }

                    if (currentBalance.compareTo(BigDecimal.ZERO) <= 0 && acc.getBalance() != null && acc.getBalance().compareTo(BigDecimal.ZERO) > 0) {
                        currentBalance = acc.getBalance();
                    }

                    BigDecimal availableLimit = acc.getAvailableCreditLimit() != null
                            ? acc.getAvailableCreditLimit()
                            : limit.subtract(currentBalance.add(futureBalance)).max(BigDecimal.ZERO);

                    BigDecimal totalUsedLimit = limit.subtract(availableLimit).max(BigDecimal.ZERO);
                    BigDecimal utilizationPct = limit.compareTo(BigDecimal.ZERO) > 0
                            ? totalUsedLimit.multiply(new BigDecimal("100")).divide(limit, 1, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO;

                    String status = "OPEN";
                    if (dueDate.isBefore(now) && currentBalance.compareTo(BigDecimal.ZERO) > 0) {
                        status = "OVERDUE";
                    } else if (closeDate.isBefore(now)) {
                        status = "CLOSED";
                    }

                    invoices.add(InvoiceResponse.builder()
                            .accountId(acc.getId())
                            .accountName(acc.getName() != null ? acc.getName() : "Cartão de Crédito")
                            .maskedNumber(maskedNum)
                            .status(status)
                            .currentBalance(currentBalance)
                            .futureBalance(futureBalance)
                            .totalUsedLimit(totalUsedLimit)
                            .creditLimit(limit)
                            .availableCreditLimit(availableLimit)
                            .utilizationPercentage(utilizationPct)
                            .balanceCloseDate(closeDate)
                            .balanceDueDate(dueDate)
                            .minimumPaymentAmount(acc.getMinimumPaymentAmount())
                            .transactionCount(currentTxs.size())
                            .transactions(currentTxs)
                            .futureTransactions(futureTxs)
                            .pendingSync(true)
                            .isCurrent(true)
                            .build());
                }
            }
        }

        return invoices;
    }

    /**
     * Retorna o histórico completo de faturas para uma determinada conta de cartão de crédito.
     */
    @Transactional(readOnly = true)
    public List<InvoiceHistoryItem> getInvoiceHistory(Long accountId) {
        List<com.finance.pluggy.domain.model.Invoice> dbInvoices =
                invoiceRepository.findByAccountIdOrderByDueDateAsc(accountId);

        if (dbInvoices == null || dbInvoices.isEmpty()) {
            return List.of();
        }

        List<Transaction> accountTxs = transactionRepository.findByAccountId(accountId);
        List<InvoiceHistoryItem> history = new ArrayList<>();

        for (int i = 0; i < dbInvoices.size(); i++) {
            com.finance.pluggy.domain.model.Invoice inv = dbInvoices.get(i);
            LocalDate prevCloseDate = (i > 0) ? dbInvoices.get(i - 1).getCloseDate() : null;

            List<Transaction> invTxs = new ArrayList<>();
            for (Transaction tx : accountTxs) {
                String txBillId = tx.getPluggyBillId();
                LocalDate txDate = tx.getDate();

                if (txBillId != null && !txBillId.isBlank()) {
                    if (txBillId.equals(inv.getPluggyBillId())) {
                        invTxs.add(tx);
                    }
                } else {
                    if (inv.getCloseDate() != null && txDate != null) {
                        if (prevCloseDate != null) {
                            if (txDate.isAfter(prevCloseDate) && !txDate.isAfter(inv.getCloseDate())) {
                                invTxs.add(tx);
                            }
                        } else {
                            if (!txDate.isAfter(inv.getCloseDate())) {
                                invTxs.add(tx);
                            }
                        }
                    }
                }
            }

            BigDecimal total = inv.getTotalAmount() != null
                    ? inv.getTotalAmount()
                    : (inv.getTotalBalance() != null ? inv.getTotalBalance() : BigDecimal.ZERO);

            history.add(InvoiceHistoryItem.builder()
                    .id(inv.getId())
                    .closeDate(inv.getCloseDate())
                    .dueDate(inv.getDueDate())
                    .status(inv.getStatus() != null ? inv.getStatus() : "OPEN")
                    .totalAmount(total)
                    .minimumPaymentAmount(inv.getMinimumPaymentAmount())
                    .transactions(invTxs)
                    .build());
        }

        history.sort((a, b) -> {
            LocalDate dateA = a.getCloseDate() != null ? a.getCloseDate() : a.getDueDate();
            LocalDate dateB = b.getCloseDate() != null ? b.getCloseDate() : b.getDueDate();
            if (dateA == null && dateB == null) return 0;
            if (dateA == null) return 1;
            if (dateB == null) return -1;
            return dateB.compareTo(dateA);
        });

        return history;
    }

    private boolean isCreditCardPayment(Transaction tx) {
        if (tx == null) return false;

        String category = tx.getPluggyCategory();
        if (category != null && !category.isBlank()) {
            String lowerCat = category.toLowerCase();
            if (lowerCat.contains("credit card payment")
                    || lowerCat.contains("credit_card_payment")
                    || lowerCat.contains("pagamento de cartão")
                    || lowerCat.contains("pagamento de cartao")
                    || lowerCat.contains("pagamento fatura")) {
                return true;
            }
        }

        String desc = tx.getDescription();
        if (desc != null && !desc.isBlank()) {
            String lowerDesc = desc.toLowerCase();
            if (lowerDesc.contains("pagamento de fatura")
                    || lowerDesc.contains("pagamento fatura")
                    || lowerDesc.contains("pagamento de cartao")
                    || lowerDesc.contains("pagamento de cartão")
                    || lowerDesc.contains("pagto fatura")
                    || lowerDesc.contains("fatura paga")
                    || lowerDesc.contains("pagamento efetuado")
                    || lowerDesc.contains("pagamento recebido")) {
                return true;
            }
        }

        String rawDesc = tx.getRawDescription();
        if (rawDesc != null && !rawDesc.isBlank()) {
            String lowerRaw = rawDesc.toLowerCase();
            if (lowerRaw.contains("pagamento de fatura")
                    || lowerRaw.contains("pagamento fatura")
                    || lowerRaw.contains("pagamento de cartao")
                    || lowerRaw.contains("pagamento de cartão")
                    || lowerRaw.contains("pagto fatura")
                    || lowerRaw.contains("fatura paga")) {
                return true;
            }
        }

        return false;
    }

    private int resolveCloseDueGapDays(List<com.finance.pluggy.domain.model.Invoice> historicalInvoices) {
        if (historicalInvoices == null || historicalInvoices.isEmpty()) {
            return 7;
        }

        Map<Long, Long> gapFreq = historicalInvoices.stream()
                .filter(inv -> inv.getCloseDate() != null && inv.getDueDate() != null)
                .map(inv -> java.time.temporal.ChronoUnit.DAYS.between(inv.getCloseDate(), inv.getDueDate()))
                .filter(gap -> gap > 0)
                .collect(Collectors.groupingBy(g -> g, Collectors.counting()));

        return gapFreq.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(e -> e.getKey().intValue())
                .orElse(7);
    }

    private boolean isNationalHoliday(LocalDate date) {
        if (date == null) return false;
        int month = date.getMonthValue();
        int day = date.getDayOfMonth();
        return (month == 1 && day == 1)
                || (month == 4 && day == 21)
                || (month == 5 && day == 1)
                || (month == 9 && day == 7)
                || (month == 10 && day == 12)
                || (month == 11 && day == 2)
                || (month == 11 && day == 15)
                || (month == 12 && day == 25);
    }

    private LocalDate adjustForWeekendAndHoliday(LocalDate date) {
        if (date == null) return null;
        while (date.getDayOfWeek() == java.time.DayOfWeek.SATURDAY
                || date.getDayOfWeek() == java.time.DayOfWeek.SUNDAY
                || isNationalHoliday(date)) {
            date = date.plusDays(1);
        }
        return date;
    }

    private static final Pattern INSTALLMENT_PATTERN = Pattern.compile("(\\d{2})/(\\d{2})\\s*$");

    private boolean belongsToPreviousCycle(Transaction tx, LocalDate previousCloseDate, LocalDate previousDueDate) {
        if (tx == null || tx.getDate() == null || previousCloseDate == null || previousDueDate == null) {
            return false;
        }
        String desc = tx.getDescription() != null && !tx.getDescription().isBlank()
                ? tx.getDescription()
                : (tx.getRawDescription() != null ? tx.getRawDescription() : "");
        boolean isInstallment = INSTALLMENT_PATTERN.matcher(desc.trim()).find();
        return isInstallment
                && tx.getDate().isAfter(previousCloseDate)
                && !tx.getDate().isAfter(previousDueDate);
    }

    private boolean isCreditCard(Account acc) {
        return acc.getType() == AccountType.CREDIT || acc.getSubtype() == AccountSubtype.CREDIT_CARD;
    }
}
