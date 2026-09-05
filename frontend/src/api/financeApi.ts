import { apiClient } from './client';
import type {
  DashboardSummary,
  AccountOverview,
  CategoryExpenseReport,
  MonthlyExpenseReport,
  Transaction,
  Account,
  Item,
  Invoice,
  CategoryBudgetStatus,
  BudgetAlertLog,
  SyncLog,
} from '../types/finance';

export const financeApi = {
  // Dashboard
  getSummary: async (): Promise<DashboardSummary> => {
    const res = await apiClient.get('/dashboard/summary');
    return res.data;
  },

  getAccountOverview: async (): Promise<AccountOverview> => {
    const res = await apiClient.get('/dashboard/account-overview');
    return res.data;
  },

  getExpensesByCategory: async (year?: number, month?: number): Promise<CategoryExpenseReport[]> => {
    const res = await apiClient.get('/dashboard/expenses-by-category', {
      params: { year, month },
    });
    return res.data;
  },

  getMonthlyHistory: async (monthsCount: number = 6): Promise<MonthlyExpenseReport[]> => {
    const res = await apiClient.get('/dashboard/monthly-history', {
      params: { monthsCount },
    });
    return res.data;
  },

  // Transactions & Accounts
  getAccounts: async (): Promise<Account[]> => {
    const res = await apiClient.get('/accounts');
    return res.data;
  },

  getTransactions: async (): Promise<Transaction[]> => {
    const res = await apiClient.get('/transactions');
    return res.data;
  },

  getItems: async (): Promise<Item[]> => {
    const res = await apiClient.get('/items');
    return res.data;
  },

  // Invoices
  getInvoices: async (): Promise<Invoice[]> => {
    const res = await apiClient.get('/invoices');
    return res.data;
  },

  // Budgets
  getBudgets: async (): Promise<CategoryBudgetStatus[]> => {
    const res = await apiClient.get('/budgets');
    return res.data;
  },

  saveBudget: async (data: { category: string; monthlyLimit: number; alertThresholdPercentage: number }) => {
    const res = await apiClient.post('/budgets', data);
    return res.data;
  },

  getBudgetAlerts: async (): Promise<BudgetAlertLog[]> => {
    const res = await apiClient.get('/budgets/alerts');
    return res.data;
  },

  // Sync & Connect
  getConnectToken: async (itemId?: string): Promise<{ accessToken: string }> => {
    const res = await apiClient.post('/pluggy/connect-token', {}, { params: { itemId } });
    return res.data;
  },

  triggerSync: async (itemId: string): Promise<{ status: string; message: string }> => {
    const res = await apiClient.post(`/sync/${itemId}`);
    return res.data;
  },

  getSyncLogs: async (): Promise<SyncLog[]> => {
    const res = await apiClient.get('/sync-logs');
    return res.data;
  },
};
