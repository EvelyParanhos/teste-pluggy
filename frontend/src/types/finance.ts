export interface Item {
  id: number;
  pluggyItemId: String;
  connectorId?: number;
  connectorName?: string;
  status: string;
  lastUpdatedAt: string;
}

export interface Account {
  id: number;
  pluggyAccountId: string;
  name: string;
  marketingName?: string;
  number?: string;
  agency?: string;
  type: string;
  subtype: string;
  balance: number;
  currencyCode: string;
  creditLimit?: number;
  availableCreditLimit?: number;
  balanceCloseDate?: string;
  balanceDueDate?: string;
  minimumPaymentAmount?: number;
}

export interface Transaction {
  id: number;
  pluggyTransactionId: string;
  description: string;
  rawDescription?: string;
  amount: number;
  date: string;
  type: 'DEBIT' | 'CREDIT';
  status: string;
  pluggyCategory?: string;
  internalCategory?: string;
}

export interface DashboardSummary {
  totalConsolidatedBalance: number;
  totalBankBalance: number;
  totalCreditCardBalance: number;
  totalInvestmentBalance: number;
  netWorth: number;
  totalIncomeCurrentMonth: number;
  totalExpensesCurrentMonth: number;
  netSavingsCurrentMonth: number;
  activeItemsCount: number;
  activeAccountsCount: number;
}

export interface BankAccountItem {
  id: number;
  institutionName: string;
  name: string;
  number: string;
  balance: number;
  percentageShare: number;
}

export interface CreditCardItem {
  id: number;
  name: string;
  maskedNumber: string;
  balance: number;
  limit: number;
}

export interface InvestmentItem {
  id: number;
  name: string;
  assetClass: string;
  balance: number;
  percentageShare: number;
}

export interface AccountOverview {
  bankAccountsGroup: {
    totalBalance: number;
    items: BankAccountItem[];
  };
  creditCardsGroup: {
    totalSpent: number;
    totalLimit: number;
    utilizationPercentage: number;
    items: CreditCardItem[];
  };
  investmentsGroup: {
    totalBalance: number;
    items: InvestmentItem[];
  };
}

export interface CategoryExpenseReport {
  category: string;
  categoryDescription: string;
  totalAmount: number;
  percentageOfTotal: number;
  transactionCount: number;
  monthlyLimit?: number;
}

export interface MonthlyExpenseReport {
  yearMonth: string;
  monthName: string;
  totalIncome: number;
  totalExpenses: number;
  netResult: number;
}

export interface CategoryBudgetStatus {
  id: number;
  category: string;
  categoryDescription: string;
  monthlyLimit: number;
  currentSpent: number;
  percentageUsed: number;
  status: 'NORMAL' | 'WARN' | 'EXCEEDED';
}

export interface BudgetAlertLog {
  id: number;
  pluggyItemId?: string;
  category: string;
  monthlyLimit: number;
  currentSpent: number;
  yearMonth: string;
  createdAt: string;
}

export interface Invoice {
  accountId: number;
  accountName: string;
  maskedNumber: string;
  status: 'OPEN' | 'CLOSED' | 'OVERDUE' | 'PAID';
  currentBalance: number;
  futureBalance?: number;
  totalUsedLimit?: number;
  creditLimit: number;
  availableCreditLimit: number;
  utilizationPercentage: number;
  balanceCloseDate: string;
  balanceDueDate: string;
  minimumPaymentAmount?: number;
  transactionCount: number;
  transactions: Transaction[];
  futureTransactions?: Transaction[];
  pendingSync?: boolean;
  isCurrent?: boolean;
}

export interface SyncLog {
  id: number;
  pluggyItemId: string;
  status: 'PENDING' | 'SUCCESS' | 'FAILED';
  attempts: number;
  lastError?: string;
  nextAttemptAt?: string;
  createdAt: string;
  updatedAt: string;
}
