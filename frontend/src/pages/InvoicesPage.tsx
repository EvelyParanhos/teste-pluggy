import React, { useEffect, useState } from 'react';
import { CreditCard, Calendar, AlertTriangle, CheckCircle2, ArrowUpRight, ArrowDownLeft, Clock } from 'lucide-react';
import { financeApi } from '../api/financeApi';
import type { Invoice, Transaction } from '../types/finance';

export const InvoicesPage: React.FC = () => {
  const [invoices, setInvoices] = useState<Invoice[]>([]);
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

  useEffect(() => {
    loadInvoices();
  }, []);

  const loadInvoices = async () => {
    setLoading(true);
    try {
      const data = await financeApi.getInvoices();
      setInvoices(data);
    } catch (err) {
      console.error('Erro ao carregar faturas:', err);
    } finally {
      setLoading(false);
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

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '32px' }}>
      
      {invoices.length === 0 ? (
        <div className="card-fintech" style={{ padding: '40px', textAlign: 'center', color: '#A098B5' }}>
          <CreditCard size={48} color="#5B37C4" style={{ marginBottom: '16px' }} />
          <h3 style={{ color: '#FFF', marginBottom: '8px' }}>Nenhum Cartão de Crédito Conectado</h3>
          <p style={{ fontSize: '0.9rem' }}>Conecte sua conta bancária contendo cartão de crédito para visualizar suas faturas ativas e projetadas.</p>
        </div>
      ) : (
        invoices.map((inv, idx) => {
          const isOverdue = inv.status === 'OVERDUE';
          const isClosed = inv.status === 'CLOSED';
          const isPaid = inv.status === 'PAID';
          const futureTxs = inv.futureTransactions || [];
          const usedLimit = inv.totalUsedLimit !== undefined ? inv.totalUsedLimit : (inv.currentBalance + (inv.futureBalance || 0));

          return (
            <div key={`${inv.accountId}-${inv.balanceDueDate || idx}`} className="card-fintech">
              
              {/* Header do Cartão */}
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '20px', flexWrap: 'wrap', gap: '12px' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                  <div style={{ width: '44px', height: '44px', borderRadius: '12px', backgroundColor: '#241D37', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                    <CreditCard size={24} color="#EB2478" />
                  </div>
                  <div>
                    <h3 style={{ fontSize: '1.2rem', fontWeight: 700, color: '#FFF', fontFamily: 'Space Grotesk' }}>{inv.accountName}</h3>
                    <span style={{ fontSize: '0.8rem', color: '#A098B5' }}>{inv.maskedNumber}</span>
                  </div>
                </div>

                <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                  {inv.pendingSync ? (
                    <span className="badge-status badge-warning" style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
                      <Clock size={14} /> AGUARDANDO SINCRONIZAÇÃO DE FATURAS
                    </span>
                  ) : isPaid ? (
                    <span className="badge-status badge-positive" style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
                      <CheckCircle2 size={14} /> PAGA
                    </span>
                  ) : isOverdue ? (
                    <span className="badge-status badge-critical" style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
                      <AlertTriangle size={14} /> VENCIDA
                    </span>
                  ) : isClosed ? (
                    <span className="badge-status badge-warning" style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
                      <Calendar size={14} /> FECHADA
                    </span>
                  ) : (
                    <span className="badge-status badge-positive" style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
                      <CheckCircle2 size={14} /> EM ABERTO
                    </span>
                  )}
                </div>
              </div>

              {inv.pendingSync && (
                <div style={{ fontSize: '0.85rem', color: '#EAB308', backgroundColor: 'rgba(234, 179, 8, 0.08)', padding: '12px 16px', borderRadius: '8px', marginBottom: '20px', border: '1px solid rgba(234, 179, 8, 0.2)', display: 'flex', alignItems: 'center', gap: '8px' }}>
                  <Clock size={16} />
                  <span>As faturas oficiais deste cartão ainda estão sendo sincronizadas pela Pluggy. Os valores exibidos abaixo são estimativas temporárias.</span>
                </div>
              )}

              {/* Grid de Informações Financeiras da Fatura */}
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: '16px', marginBottom: '24px', backgroundColor: '#14101F', padding: '20px', borderRadius: '12px', border: '1px solid #2E2644' }}>
                <div>
                  <span style={{ fontSize: '0.75rem', color: '#A098B5', textTransform: 'uppercase', fontWeight: 600 }}>Fatura Atual</span>
                  <h3 className="tabular-nums" style={{ fontSize: '1.5rem', fontWeight: 700, color: '#EB2478', marginTop: '4px' }}>
                    {formatBRL(inv.currentBalance)}
                  </h3>
                </div>

                {inv.futureBalance !== undefined && inv.futureBalance > 0 && (
                  <div>
                    <span style={{ fontSize: '0.75rem', color: '#A098B5', textTransform: 'uppercase', fontWeight: 600 }}>Faturas Futuras</span>
                    <h3 className="tabular-nums" style={{ fontSize: '1.5rem', fontWeight: 700, color: '#A098B5', marginTop: '4px' }}>
                      {formatBRL(inv.futureBalance)}
                    </h3>
                  </div>
                )}

                <div>
                  <span style={{ fontSize: '0.75rem', color: '#A098B5', textTransform: 'uppercase', fontWeight: 600 }}>Limite Utilizado</span>
                  <h4 className="tabular-nums" style={{ fontSize: '1.1rem', fontWeight: 700, color: '#FFF', marginTop: '4px' }}>
                    {inv.utilizationPercentage}% de {formatBRL(inv.creditLimit)}
                  </h4>
                  <div style={{ fontSize: '0.75rem', color: '#A098B5', marginTop: '2px' }}>
                    Usado: {formatBRL(usedLimit)}
                  </div>
                  <div className="progress-bar-bg" style={{ marginTop: '6px' }}>
                    <div className="progress-bar-fill" style={{ width: `${Math.min(inv.utilizationPercentage, 100)}%`, backgroundColor: '#EB2478' }} />
                  </div>
                </div>

                <div>
                  <span style={{ fontSize: '0.75rem', color: '#A098B5', textTransform: 'uppercase', fontWeight: 600 }}>Fechamento</span>
                  <h4 style={{ fontSize: '1.1rem', fontWeight: 700, color: '#FFF', marginTop: '4px' }}>
                    {formatDate(inv.balanceCloseDate)}
                  </h4>
                </div>

                <div>
                  <span style={{ fontSize: '0.75rem', color: '#A098B5', textTransform: 'uppercase', fontWeight: 600 }}>Vencimento</span>
                  <h4 style={{ fontSize: '1.1rem', fontWeight: 700, color: isOverdue ? '#EB2478' : '#CAF33C', marginTop: '4px' }}>
                    {formatDate(inv.balanceDueDate)}
                  </h4>
                </div>
              </div>

              {/* Lançamentos do Ciclo Atual */}
              <div style={{ marginBottom: futureTxs.length > 0 ? '24px' : '0' }}>
                <h4 style={{ fontSize: '1rem', fontWeight: 700, fontFamily: 'Space Grotesk', marginBottom: '12px', color: '#FFF', display: 'flex', alignItems: 'center', gap: '8px' }}>
                  <Calendar size={16} color="#EB2478" /> Lançamentos da Fatura Atual ({inv.transactionCount})
                </h4>

                {inv.transactions.length === 0 ? (
                  <div style={{ fontSize: '0.85rem', color: '#A098B5', padding: '16px 0' }}>
                    Nenhuma despesa lançada neste cartão durante o ciclo atual.
                  </div>
                ) : (
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                    {inv.transactions.map((tx) => renderTransactionItem(tx))}
                  </div>
                )}
              </div>

              {/* Lançamentos de Faturas Futuras / Parcelamentos */}
              {futureTxs.length > 0 && (
                <div>
                  <h4 style={{ fontSize: '1rem', fontWeight: 700, fontFamily: 'Space Grotesk', marginBottom: '12px', color: '#FFF', display: 'flex', alignItems: 'center', gap: '8px' }}>
                    <Clock size={16} color="#A098B5" /> Faturas Futuras / Parcelamentos ({futureTxs.length})
                  </h4>
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                    {futureTxs.map((tx) => renderTransactionItem(tx))}
                  </div>
                </div>
              )}

            </div>
          );
        })
      )}

    </div>
  );
};
