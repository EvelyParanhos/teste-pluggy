import React, { useEffect, useState } from 'react';
import { CreditCard, Calendar, AlertTriangle, CheckCircle2, ArrowUpRight, ArrowDownLeft, Clock } from 'lucide-react';
import { financeApi } from '../api/financeApi';
import type { Invoice, InvoiceHistoryItem, Transaction } from '../types/finance';

interface MonthTab {
  key: string;
  label: string;
  isCurrent: boolean;
  status: 'OPEN' | 'CLOSED' | 'OVERDUE' | 'PAID';
  closeDate?: string;
  dueDate?: string;
  totalAmount: number;
  minimumPaymentAmount?: number;
  transactions: Transaction[];
  futureTransactions?: Transaction[];
}

export const InvoicesPage: React.FC = () => {
  const [invoices, setInvoices] = useState<Invoice[]>([]);
  const [selectedAccountId, setSelectedAccountId] = useState<number | null>(null);
  const [historyItems, setHistoryItems] = useState<InvoiceHistoryItem[]>([]);
  const [selectedTabKey, setSelectedTabKey] = useState<string>('current');
  const [loading, setLoading] = useState<boolean>(true);

  const formatBRL = (val?: number) => {
    if (val === undefined || val === null) return 'R$ 0,00';
    return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(Math.abs(val));
  };

  const formatDate = (dateStr?: string) => {
    if (!dateStr) return '-';
    const clean = dateStr.split('T')[0];
    const parts = clean.split('-');
    if (parts.length === 3) {
      const [y, m, d] = parts;
      return `${d.padStart(2, '0')}/${m.padStart(2, '0')}/${y}`;
    }
    return dateStr;
  };

  const getMonthLabel = (dateStr?: string, isCurrentTab?: boolean) => {
    if (!dateStr) return isCurrentTab ? 'Atual' : 'Mês';
    const clean = dateStr.split('T')[0];
    const parts = clean.split('-');
    if (parts.length === 3) {
      const year = parts[0];
      const monthIdx = parseInt(parts[1], 10) - 1;
      const months = ['Jan', 'Fev', 'Mar', 'Abr', 'Mai', 'Jun', 'Jul', 'Ago', 'Set', 'Out', 'Nov', 'Dez'];
      const monthName = months[monthIdx] || parts[1];
      return `${monthName} ${year}${isCurrentTab ? ' (atual)' : ''}`;
    }
    return dateStr;
  };

  useEffect(() => {
    loadInvoices();
  }, []);

  const loadInvoices = async () => {
    setLoading(true);
    try {
      const data = await financeApi.getInvoices();
      setInvoices(data);
      if (data.length > 0) {
        setSelectedAccountId(data[0].accountId);
      }
    } catch (err) {
      console.error('Erro ao carregar faturas:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (selectedAccountId !== null) {
      loadHistory(selectedAccountId);
      setSelectedTabKey('current');
    }
  }, [selectedAccountId]);

  const loadHistory = async (accId: number) => {
    try {
      const history = await financeApi.getInvoiceHistory(accId);
      setHistoryItems(history);
    } catch (err) {
      console.error('Erro ao carregar histórico de faturas:', err);
      setHistoryItems([]);
    }
  };

  const currentInvoice = invoices.find(inv => inv.accountId === selectedAccountId);

  // Monta as abas de meses (atual + histórico)
  const monthTabs: MonthTab[] = [];

  if (currentInvoice) {
    monthTabs.push({
      key: 'current',
      label: getMonthLabel(currentInvoice.balanceCloseDate || currentInvoice.balanceDueDate, true),
      isCurrent: true,
      status: currentInvoice.status,
      closeDate: currentInvoice.balanceCloseDate,
      dueDate: currentInvoice.balanceDueDate,
      totalAmount: currentInvoice.currentBalance,
      minimumPaymentAmount: currentInvoice.minimumPaymentAmount,
      transactions: currentInvoice.transactions || [],
      futureTransactions: currentInvoice.futureTransactions || [],
    });
  }

  if (historyItems && historyItems.length > 0) {
    historyItems.forEach(item => {
      // Evita duplicar se a fatura histórica for idêntica à fatura atual
      const isSameDate = currentInvoice &&
        ((item.closeDate && item.closeDate === currentInvoice.balanceCloseDate) ||
         (item.dueDate && item.dueDate === currentInvoice.balanceDueDate));

      if (!isSameDate) {
        monthTabs.push({
          key: `hist-${item.id}`,
          label: getMonthLabel(item.closeDate || item.dueDate, false),
          isCurrent: false,
          status: item.status as any,
          closeDate: item.closeDate,
          dueDate: item.dueDate,
          totalAmount: item.totalAmount,
          minimumPaymentAmount: item.minimumPaymentAmount,
          transactions: item.transactions || [],
        });
      }
    });
  }

  const activeTab = monthTabs.find(t => t.key === selectedTabKey) || monthTabs[0];

  const getStatusConfig = (status: string) => {
    switch (status) {
      case 'PAID':
        return { label: 'PAGA', color: '#CAF33C', bg: 'rgba(202, 243, 60, 0.12)', border: 'rgba(202, 243, 60, 0.3)', icon: <CheckCircle2 size={14} /> };
      case 'OPEN':
        return { label: 'EM ABERTO', color: '#AFEFE1', bg: 'rgba(175, 239, 225, 0.12)', border: 'rgba(175, 239, 225, 0.3)', icon: <Clock size={14} /> };
      case 'OVERDUE':
        return { label: 'VENCIDA', color: '#EB2478', bg: 'rgba(235, 36, 120, 0.12)', border: 'rgba(235, 36, 120, 0.3)', icon: <AlertTriangle size={14} /> };
      case 'CLOSED':
      default:
        return { label: 'FECHADA', color: '#FF8E1B', bg: 'rgba(255, 142, 27, 0.12)', border: 'rgba(255, 142, 27, 0.3)', icon: <Calendar size={14} /> };
    }
  };

  const renderTransactionItem = (tx: Transaction) => {
    const isCredit = tx.type === 'CREDIT' || (tx.amount && tx.amount < 0);
    return (
      <div key={tx.id} style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '12px', borderRadius: '8px', backgroundColor: '#14101F', border: '1px solid #2E2644' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '12px', flex: 1 }}>
          {isCredit ? (
            <ArrowDownLeft size={16} color="#CAF33C" />
          ) : (
            <ArrowUpRight size={16} color="#EB2478" />
          )}
          <div>
            <strong style={{ fontSize: '0.85rem', color: '#FFF', display: 'block' }}>{tx.description}</strong>
            <span style={{ fontSize: '0.75rem', color: '#A098B5' }}>{formatDate(tx.date)} · {tx.internalCategory || 'Geral'}</span>
          </div>
        </div>
        <span className="tabular-nums" style={{ fontWeight: 700, color: isCredit ? '#CAF33C' : '#EB2478' }}>
          {isCredit ? '+ ' : '- '}{formatBRL(tx.amount)}
        </span>
      </div>
    );
  };

  if (loading) {
    return (
      <div style={{ padding: '32px', color: '#A098B5', textAlign: 'center' }}>
        Carregando faturas de cartão de crédito...
      </div>
    );
  }

  if (invoices.length === 0) {
    return (
      <div className="card-fintech" style={{ padding: '40px', textAlign: 'center', color: '#A098B5' }}>
        <CreditCard size={48} color="#5B37C4" style={{ marginBottom: '16px' }} />
        <h3 style={{ color: '#FFF', marginBottom: '8px' }}>Nenhum Cartão de Crédito Conectado</h3>
        <p style={{ fontSize: '0.9rem' }}>Conecte sua conta bancária contendo cartão de crédito para visualizar suas faturas ativas e projetadas.</p>
      </div>
    );
  }

  const usedLimit = currentInvoice?.totalUsedLimit !== undefined
    ? currentInvoice.totalUsedLimit
    : ((currentInvoice?.currentBalance || 0) + (currentInvoice?.futureBalance || 0));

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
      
      {/* Seletor de Cartões (quando houver múltiplos cartões) */}
      {invoices.length > 1 && (
        <div style={{ display: 'flex', alignItems: 'center', gap: '12px', flexWrap: 'wrap' }}>
          <span style={{ fontSize: '0.85rem', color: '#A098B5', fontWeight: 600 }}>Cartão Selecionado:</span>
          <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
            {invoices.map(inv => (
              <button
                key={inv.accountId}
                onClick={() => setSelectedAccountId(inv.accountId)}
                style={{
                  padding: '8px 16px',
                  borderRadius: '20px',
                  fontSize: '0.85rem',
                  fontWeight: 600,
                  backgroundColor: selectedAccountId === inv.accountId ? '#5B37C4' : '#14101F',
                  color: selectedAccountId === inv.accountId ? '#FFF' : '#A098B5',
                  border: selectedAccountId === inv.accountId ? '1px solid #7C5CFF' : '1px solid #2E2644',
                  cursor: 'pointer',
                  transition: 'all 0.2s ease',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '8px'
                }}
              >
                <CreditCard size={14} />
                {inv.accountName} ({inv.maskedNumber})
              </button>
            ))}
          </div>
        </div>
      )}

      {currentInvoice && (
        <div className="card-fintech">
          
          {/* Header do Cartão */}
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '20px', flexWrap: 'wrap', gap: '12px' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
              <div style={{ width: '44px', height: '44px', borderRadius: '12px', backgroundColor: '#241D37', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                <CreditCard size={24} color="#EB2478" />
              </div>
              <div>
                <h3 style={{ fontSize: '1.2rem', fontWeight: 700, color: '#FFF', fontFamily: 'Space Grotesk' }}>{currentInvoice.accountName}</h3>
                <span style={{ fontSize: '0.8rem', color: '#A098B5' }}>{currentInvoice.maskedNumber}</span>
              </div>
            </div>

            {/* Limite de Crédito Utilizado */}
            <div style={{ minWidth: '200px' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.75rem', color: '#A098B5', marginBottom: '4px' }}>
                <span>Limite Utilizado: <strong>{currentInvoice.utilizationPercentage}%</strong></span>
                <span>Usado: {formatBRL(usedLimit)} / {formatBRL(currentInvoice.creditLimit)}</span>
              </div>
              <div className="progress-bar-bg">
                <div className="progress-bar-fill" style={{ width: `${Math.min(currentInvoice.utilizationPercentage, 100)}%`, backgroundColor: '#EB2478' }} />
              </div>
            </div>
          </div>

          {currentInvoice.pendingSync && (
            <div style={{ fontSize: '0.85rem', color: '#EAB308', backgroundColor: 'rgba(234, 179, 8, 0.08)', padding: '12px 16px', borderRadius: '8px', marginBottom: '20px', border: '1px solid rgba(234, 179, 8, 0.2)', display: 'flex', alignItems: 'center', gap: '8px' }}>
              <Clock size={16} />
              <span>As faturas oficiais deste cartão ainda estão sendo sincronizadas pela Pluggy. Os valores exibidos são estimativas baseadas no extrato.</span>
            </div>
          )}

          {/* Abas Mensais Roláveis (InvoiceMonthTabs) */}
          <div style={{ display: 'flex', gap: '10px', overflowX: 'auto', paddingBottom: '8px', marginBottom: '24px', scrollbarWidth: 'thin' }}>
            {monthTabs.map(tab => {
              const isSelected = activeTab && activeTab.key === tab.key;
              const statusCfg = getStatusConfig(tab.status);
              return (
                <button
                  key={tab.key}
                  onClick={() => setSelectedTabKey(tab.key)}
                  style={{
                    display: 'flex',
                    flexDirection: 'column',
                    alignItems: 'flex-start',
                    gap: '6px',
                    padding: '12px 16px',
                    borderRadius: '12px',
                    backgroundColor: isSelected ? '#241D37' : '#14101F',
                    border: isSelected ? `2px solid ${statusCfg.color}` : '1px solid #2E2644',
                    cursor: 'pointer',
                    whiteSpace: 'nowrap',
                    transition: 'all 0.2s ease',
                    minWidth: '130px'
                  }}
                >
                  <span style={{ fontSize: '0.85rem', fontWeight: isSelected ? 700 : 500, color: isSelected ? '#FFF' : '#A098B5' }}>
                    {tab.label}
                  </span>
                  <span
                    style={{
                      fontSize: '0.7rem',
                      fontWeight: 700,
                      color: statusCfg.color,
                      backgroundColor: statusCfg.bg,
                      padding: '2px 8px',
                      borderRadius: '12px',
                      border: `1px solid ${statusCfg.border}`,
                      display: 'inline-flex',
                      alignItems: 'center',
                      gap: '4px'
                    }}
                  >
                    {statusCfg.icon}
                    {statusCfg.label}
                  </span>
                </button>
              );
            })}
          </div>

          {/* Conteúdo do Mês Selecionado (InvoiceMonthDetail) */}
          {activeTab && (
            <div>
              {/* Grid de Resumo Financeiro do Mês */}
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(160px, 1fr))', gap: '16px', marginBottom: '24px', backgroundColor: '#14101F', padding: '20px', borderRadius: '12px', border: '1px solid #2E2644' }}>
                <div>
                  <span style={{ fontSize: '0.75rem', color: '#A098B5', textTransform: 'uppercase', fontWeight: 600 }}>Total da Fatura</span>
                  <h3 className="tabular-nums" style={{ fontSize: '1.5rem', fontWeight: 700, color: activeTab.status === 'OVERDUE' ? '#EB2478' : '#FFF', marginTop: '4px' }}>
                    {formatBRL(activeTab.totalAmount)}
                  </h3>
                </div>

                {activeTab.isCurrent && currentInvoice.futureBalance !== undefined && currentInvoice.futureBalance > 0 && (
                  <div>
                    <span style={{ fontSize: '0.75rem', color: '#A098B5', textTransform: 'uppercase', fontWeight: 600 }}>Faturas Futuras</span>
                    <h3 className="tabular-nums" style={{ fontSize: '1.5rem', fontWeight: 700, color: '#A098B5', marginTop: '4px' }}>
                      {formatBRL(currentInvoice.futureBalance)}
                    </h3>
                  </div>
                )}

                <div>
                  <span style={{ fontSize: '0.75rem', color: '#A098B5', textTransform: 'uppercase', fontWeight: 600 }}>Fechamento</span>
                  <h4 style={{ fontSize: '1.1rem', fontWeight: 700, color: '#FFF', marginTop: '4px' }}>
                    {formatDate(activeTab.closeDate)}
                  </h4>
                </div>

                <div>
                  <span style={{ fontSize: '0.75rem', color: '#A098B5', textTransform: 'uppercase', fontWeight: 600 }}>Vencimento</span>
                  <h4 style={{ fontSize: '1.1rem', fontWeight: 700, color: activeTab.status === 'OVERDUE' ? '#EB2478' : '#CAF33C', marginTop: '4px' }}>
                    {formatDate(activeTab.dueDate)}
                  </h4>
                </div>

                {activeTab.minimumPaymentAmount !== undefined && activeTab.minimumPaymentAmount > 0 && (
                  <div>
                    <span style={{ fontSize: '0.75rem', color: '#A098B5', textTransform: 'uppercase', fontWeight: 600 }}>Pagamento Mínimo</span>
                    <h4 className="tabular-nums" style={{ fontSize: '1.1rem', fontWeight: 700, color: '#A098B5', marginTop: '4px' }}>
                      {formatBRL(activeTab.minimumPaymentAmount)}
                    </h4>
                  </div>
                )}
              </div>

              {/* Lançamentos do Mês Selecionado */}
              <div style={{ marginBottom: (activeTab.futureTransactions && activeTab.futureTransactions.length > 0) ? '24px' : '0' }}>
                <h4 style={{ fontSize: '1rem', fontWeight: 700, fontFamily: 'Space Grotesk', marginBottom: '12px', color: '#FFF', display: 'flex', alignItems: 'center', gap: '8px' }}>
                  <Calendar size={16} color="#EB2478" /> Lançamentos do Mês ({activeTab.transactions.length})
                </h4>

                {activeTab.transactions.length === 0 ? (
                  <div style={{ fontSize: '0.85rem', color: '#A098B5', padding: '16px 0' }}>
                    Nenhuma despesa ou lançamento nesta fatura.
                  </div>
                ) : (
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                    {activeTab.transactions.map((tx) => renderTransactionItem(tx))}
                  </div>
                )}
              </div>

              {/* Lançamentos Futuros / Parcelamentos (Apenas na Aba Atual) */}
              {activeTab.isCurrent && activeTab.futureTransactions && activeTab.futureTransactions.length > 0 && (
                <div>
                  <h4 style={{ fontSize: '1rem', fontWeight: 700, fontFamily: 'Space Grotesk', marginBottom: '12px', color: '#FFF', display: 'flex', alignItems: 'center', gap: '8px' }}>
                    <Clock size={16} color="#A098B5" /> Faturas Futuras / Parcelamentos ({activeTab.futureTransactions.length})
                  </h4>
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                    {activeTab.futureTransactions.map((tx) => renderTransactionItem(tx))}
                  </div>
                </div>
              )}
            </div>
          )}

        </div>
      )}

    </div>
  );
};
