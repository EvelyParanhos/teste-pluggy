import React, { useEffect, useState } from 'react';
import { Landmark, CreditCard, TrendingUp, Building2 } from 'lucide-react';
import { Doughnut, Bar } from 'react-chartjs-2';
import {
  Chart as ChartJS,
  ArcElement,
  Tooltip,
  Legend,
  CategoryScale,
  LinearScale,
  BarElement,
  Title,
} from 'chart.js';
import { financeApi } from '../api/financeApi';
import type { DashboardSummary, AccountOverview, CategoryExpenseReport, MonthlyExpenseReport } from '../types/finance';

ChartJS.register(ArcElement, Tooltip, Legend, CategoryScale, LinearScale, BarElement, Title);

export const OverviewPage: React.FC = () => {
  const [summary, setSummary] = useState<DashboardSummary | null>(null);
  const [overview, setOverview] = useState<AccountOverview | null>(null);
  const [categoryExpenses, setCategoryExpenses] = useState<CategoryExpenseReport[]>([]);
  const [monthlyHistory, setMonthlyHistory] = useState<MonthlyExpenseReport[]>([]);
  const [loading, setLoading] = useState<boolean>(true);

  const formatBRL = (val?: number) => {
    if (val === undefined || val === null) return 'R$ 0,00';
    return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(val);
  };

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    setLoading(true);
    try {
      const [sumRes, overRes, catRes, histRes] = await Promise.all([
        financeApi.getSummary(),
        financeApi.getAccountOverview(),
        financeApi.getExpensesByCategory(),
        financeApi.getMonthlyHistory(6),
      ]);
      setSummary(sumRes);
      setOverview(overRes);
      setCategoryExpenses(catRes);
      setMonthlyHistory(histRes);
    } catch (err) {
      console.error('Erro ao carregar dados da visão geral:', err);
    } finally {
      setLoading(false);
    }
  };

  // Chart 1: Doughnut (Expenses by Category)
  const doughnutData = {
    labels: categoryExpenses.map((c) => c.categoryDescription),
    datasets: [
      {
        data: categoryExpenses.map((c) => c.totalAmount),
        backgroundColor: [
          '#5B37C4',
          '#CAF33C',
          '#FF8E1B',
          '#EB2478',
          '#AFEFE1',
          '#9333EA',
          '#3B82F6',
          '#10B981',
          '#F59E0B',
          '#6B7280',
        ],
        borderWidth: 2,
        borderColor: '#1B1629',
      },
    ],
  };

  // Chart 2: Bar (Monthly History)
  const barData = {
    labels: monthlyHistory.map((m) => m.yearMonth),
    datasets: [
      {
        label: 'Receitas',
        data: monthlyHistory.map((m) => m.totalIncome),
        backgroundColor: '#CAF33C',
        borderRadius: 6,
      },
      {
        label: 'Despesas',
        data: monthlyHistory.map((m) => m.totalExpenses),
        backgroundColor: '#EB2478',
        borderRadius: 6,
      },
    ],
  };

  if (loading) {
    return (
      <div style={{ padding: '32px', color: '#A098B5', textAlign: 'center' }}>
        Carregando visão geral financeira...
      </div>
    );
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '32px' }}>

      {/* Overview 3 Cards Section (Contas Bancárias, Cartões, Investimentos) */}
      <div>
        <h3 style={{ fontSize: '1.4rem', fontWeight: 700, fontFamily: 'Space Grotesk', marginBottom: '4px' }}>Overview</h3>
        <p style={{ fontSize: '0.85rem', color: '#A098B5', marginBottom: '20px' }}>Visão geral dos seus dados financeiros.</p>

        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))', gap: '20px' }}>
          
          {/* Card 1: CONTAS BANCÁRIAS */}
          <div className="card-fintech">
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '12px' }}>
              <Landmark size={20} color="#EB2478" />
              <span style={{ fontSize: '0.75rem', fontWeight: 700, color: '#A098B5', textTransform: 'uppercase', letterSpacing: '0.5px' }}>
                CONTAS BANCÁRIAS
              </span>
            </div>
            <h2 className="tabular-nums" style={{ fontSize: '1.8rem', fontWeight: 700, color: '#FFF', marginBottom: '20px' }}>
              {formatBRL(overview?.bankAccountsGroup?.totalBalance)}
            </h2>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
              {overview?.bankAccountsGroup?.items?.length === 0 ? (
                <span style={{ fontSize: '0.85rem', color: '#A098B5' }}>Nenhuma conta bancária conectada.</span>
              ) : (
                overview?.bankAccountsGroup?.items?.map((acc) => (
                  <div key={acc.id} style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                      <div style={{ width: '32px', height: '32px', borderRadius: '50%', backgroundColor: '#241D37', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                        <Building2 size={16} color="#5B37C4" />
                      </div>
                      <div>
                        <strong style={{ fontSize: '0.85rem', color: '#FFF', display: 'block' }}>{acc.institutionName || acc.name}</strong>
                        <span style={{ fontSize: '0.75rem', color: '#A098B5' }}>1 conta · {acc.percentageShare}%</span>
                      </div>
                    </div>
                    <span className="tabular-nums" style={{ fontSize: '0.9rem', fontWeight: 700, color: '#CAF33C' }}>
                      {formatBRL(acc.balance)}
                    </span>
                  </div>
                ))
              )}
            </div>
          </div>

          {/* Card 2: CARTÕES DE CRÉDITO */}
          <div className="card-fintech">
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '12px' }}>
              <CreditCard size={20} color="#EB2478" />
              <span style={{ fontSize: '0.75rem', fontWeight: 700, color: '#A098B5', textTransform: 'uppercase', letterSpacing: '0.5px' }}>
                CARTÕES DE CRÉDITO
              </span>
            </div>
            <h2 className="tabular-nums" style={{ fontSize: '1.8rem', fontWeight: 700, color: '#EB2478', marginBottom: '8px' }}>
              {formatBRL(overview?.creditCardsGroup?.totalSpent)}
            </h2>

            <div style={{ marginBottom: '20px' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.75rem', color: '#A098B5', marginBottom: '4px' }}>
                <span>{overview?.creditCardsGroup?.utilizationPercentage}% utilizado</span>
                <span>Limite: {formatBRL(overview?.creditCardsGroup?.totalLimit)}</span>
              </div>
              <div className="progress-bar-bg">
                <div
                  className="progress-bar-fill"
                  style={{
                    width: `${Math.min(overview?.creditCardsGroup?.utilizationPercentage || 0, 100)}%`,
                    backgroundColor: '#EB2478',
                  }}
                />
              </div>
            </div>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
              {overview?.creditCardsGroup?.items?.length === 0 ? (
                <span style={{ fontSize: '0.85rem', color: '#A098B5' }}>Nenhum cartão cadastrado.</span>
              ) : (
                overview?.creditCardsGroup?.items?.map((card) => (
                  <div key={card.id} style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                      <div style={{ width: '32px', height: '32px', borderRadius: '8px', backgroundColor: '#241D37', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                        <CreditCard size={16} color="#A098B5" />
                      </div>
                      <div>
                        <strong style={{ fontSize: '0.85rem', color: '#FFF', display: 'block' }}>{card.name}</strong>
                        <span style={{ fontSize: '0.75rem', color: '#A098B5' }}>{card.maskedNumber}</span>
                      </div>
                    </div>
                    <span className="tabular-nums" style={{ fontSize: '0.9rem', fontWeight: 700, color: '#EB2478' }}>
                      {formatBRL(card.balance)}
                    </span>
                  </div>
                ))
              )}
            </div>
          </div>

          {/* Card 3: INVESTIMENTOS */}
          <div className="card-fintech">
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '12px' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                <TrendingUp size={20} color="#EB2478" />
                <span style={{ fontSize: '0.75rem', fontWeight: 700, color: '#A098B5', textTransform: 'uppercase', letterSpacing: '0.5px' }}>
                  INVESTIMENTOS
                </span>
              </div>
              <span className="badge-status badge-critical">Classes</span>
            </div>
            <h2 className="tabular-nums" style={{ fontSize: '1.8rem', fontWeight: 700, color: '#CAF33C', marginBottom: '4px' }}>
              {formatBRL(overview?.investmentsGroup?.totalBalance)}
            </h2>
            <span style={{ fontSize: '0.75rem', color: '#A098B5', marginBottom: '20px', display: 'block' }}>
              {overview?.investmentsGroup?.items?.length || 0} classes · ativos
            </span>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
              {overview?.investmentsGroup?.items?.length === 0 ? (
                <span style={{ fontSize: '0.85rem', color: '#A098B5' }}>Nenhum investimento registrado.</span>
              ) : (
                overview?.investmentsGroup?.items?.map((inv) => (
                  <div key={inv.id}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.85rem', marginBottom: '4px' }}>
                      <strong style={{ color: '#FFF' }}>{inv.assetClass} ({inv.percentageShare}%)</strong>
                      <span className="tabular-nums" style={{ fontWeight: 700, color: '#CAF33C' }}>{formatBRL(inv.balance)}</span>
                    </div>
                    <div className="progress-bar-bg">
                      <div className="progress-bar-fill" style={{ width: `${inv.percentageShare}%`, backgroundColor: '#EB2478' }} />
                    </div>
                  </div>
                ))
              )}
            </div>
          </div>

        </div>
      </div>

      {/* KPI Cards Row */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '16px' }}>
        <div className="card-fintech">
          <span style={{ fontSize: '0.75rem', color: '#A098B5', fontWeight: 600, textTransform: 'uppercase' }}>Saldo em Conta</span>
          <h3 className="tabular-nums" style={{ fontSize: '1.4rem', fontWeight: 700, color: '#5B37C4', marginTop: '6px' }}>
            {formatBRL(summary?.totalBankBalance)}
          </h3>
        </div>
        <div className="card-fintech">
          <span style={{ fontSize: '0.75rem', color: '#A098B5', fontWeight: 600, textTransform: 'uppercase' }}>Fatura dos Cartões</span>
          <h3 className="tabular-nums" style={{ fontSize: '1.4rem', fontWeight: 700, color: '#EB2478', marginTop: '6px' }}>
            {formatBRL(summary?.totalCreditCardBalance)}
          </h3>
        </div>
        <div className="card-fintech">
          <span style={{ fontSize: '0.75rem', color: '#A098B5', fontWeight: 600, textTransform: 'uppercase' }}>Despesas no Mês</span>
          <h3 className="tabular-nums" style={{ fontSize: '1.4rem', fontWeight: 700, color: '#FF8E1B', marginTop: '6px' }}>
            {formatBRL(summary?.totalExpensesCurrentMonth)}
          </h3>
        </div>
        <div className="card-fintech">
          <span style={{ fontSize: '0.75rem', color: '#A098B5', fontWeight: 600, textTransform: 'uppercase' }}>Patrimônio Líquido</span>
          <h3 className="tabular-nums" style={{ fontSize: '1.4rem', fontWeight: 700, color: '#CAF33C', marginTop: '6px' }}>
            {formatBRL(summary?.netWorth)}
          </h3>
        </div>
      </div>

      {/* Charts Section */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(340px, 1fr))', gap: '20px' }}>
        {/* Doughnut Chart */}
        <div className="card-fintech" style={{ minHeight: '340px', display: 'flex', flexDirection: 'column' }}>
          <h4 style={{ fontSize: '1rem', fontWeight: 700, fontFamily: 'Space Grotesk', marginBottom: '16px' }}>Despesas por Categoria</h4>
          <div style={{ flex: 1, position: 'relative', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
            <Doughnut
              data={doughnutData}
              options={{
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                  legend: { position: 'bottom', labels: { color: '#A098B5', font: { size: 11 } } },
                },
                cutout: '70%',
              }}
            />
          </div>
        </div>

        {/* Bar Chart */}
        <div className="card-fintech" style={{ minHeight: '340px', display: 'flex', flexDirection: 'column' }}>
          <h4 style={{ fontSize: '1rem', fontWeight: 700, fontFamily: 'Space Grotesk', marginBottom: '16px' }}>Histórico Mensal (Receitas vs Despesas)</h4>
          <div style={{ flex: 1, position: 'relative' }}>
            <Bar
              data={barData}
              options={{
                responsive: true,
                maintainAspectRatio: false,
                scales: {
                  x: { ticks: { color: '#A098B5' }, grid: { display: false } },
                  y: { ticks: { color: '#A098B5' }, grid: { color: '#2E2644' } },
                },
                plugins: {
                  legend: { position: 'top', labels: { color: '#A098B5' } },
                },
              }}
            />
          </div>
        </div>
      </div>

    </div>
  );
};
