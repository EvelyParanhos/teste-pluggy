import React, { useEffect, useState } from 'react';
import { Search, Filter, ArrowUpRight, ArrowDownLeft } from 'lucide-react';
import { financeApi } from '../api/financeApi';
import type { Transaction } from '../types/finance';

export const TransactionsPage: React.FC = () => {
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [search, setSearch] = useState<string>('');
  const [typeFilter, setTypeFilter] = useState<'ALL' | 'DEBIT' | 'CREDIT'>('ALL');
  const [loading, setLoading] = useState<boolean>(true);

  const formatBRL = (val: number) => {
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
    loadTransactions();
  }, []);

  const loadTransactions = async () => {
    setLoading(true);
    try {
      const data = await financeApi.getTransactions();
      setTransactions(data);
    } catch (err) {
      console.error('Erro ao carregar transações:', err);
    } finally {
      setLoading(false);
    }
  };

  const filteredTransactions = transactions.filter((tx) => {
    const matchesSearch =
      (tx.description && tx.description.toLowerCase().includes(search.toLowerCase())) ||
      (tx.rawDescription && tx.rawDescription.toLowerCase().includes(search.toLowerCase())) ||
      (tx.pluggyCategory && tx.pluggyCategory.toLowerCase().includes(search.toLowerCase()));
    const matchesType = typeFilter === 'ALL' || tx.type === typeFilter;
    return matchesSearch && matchesType;
  });

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
      
      {/* Header Controls */}
      <div className="card-fintech" style={{ padding: '20px', display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '16px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '12px', flex: 1, minWidth: '260px' }}>
          <div style={{ position: 'relative', width: '100%' }}>
            <Search size={18} color="#A098B5" style={{ position: 'absolute', left: '12px', top: '50%', transform: 'translateY(-50%)' }} />
            <input
              type="text"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="Buscar transação por descrição ou categoria..."
              style={{
                width: '100%',
                padding: '10px 12px 10px 40px',
                borderRadius: '8px',
                backgroundColor: '#14101F',
                border: '1px solid #2E2644',
                color: '#FFF',
                fontSize: '0.9rem',
              }}
            />
          </div>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
          <Filter size={18} color="#A098B5" />
          <select
            value={typeFilter}
            onChange={(e) => setTypeFilter(e.target.value as any)}
            style={{
              padding: '10px 16px',
              borderRadius: '8px',
              backgroundColor: '#14101F',
              border: '1px solid #2E2644',
              color: '#FFF',
              fontSize: '0.9rem',
            }}
          >
            <option value="ALL">Todos os Tipos</option>
            <option value="DEBIT">Débitos</option>
            <option value="CREDIT">Créditos</option>
          </select>
        </div>
      </div>

      {/* Transactions Table */}
      <div className="card-fintech" style={{ padding: 0, overflow: 'hidden' }}>
        <div style={{ overflowX: 'auto' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '0.9rem' }}>
            <thead>
              <tr style={{ backgroundColor: '#14101F', borderBottom: '1px solid #2E2644', color: '#A098B5', fontSize: '0.8rem', textTransform: 'uppercase' }}>
                <th style={{ padding: '16px 24px' }}>Data</th>
                <th style={{ padding: '16px 24px' }}>Descrição</th>
                <th style={{ padding: '16px 24px' }}>Categoria Pluggy</th>
                <th style={{ padding: '16px 24px' }}>Categoria Interna</th>
                <th style={{ padding: '16px 24px' }}>Tipo</th>
                <th style={{ padding: '16px 24px', textAlign: 'right' }}>Valor</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr>
                  <td colSpan={6} style={{ padding: '32px', textAlign: 'center', color: '#A098B5' }}>
                    Carregando extrato de transações...
                  </td>
                </tr>
              ) : filteredTransactions.length === 0 ? (
                <tr>
                  <td colSpan={6} style={{ padding: '32px', textAlign: 'center', color: '#A098B5' }}>
                    Nenhuma transação encontrada.
                  </td>
                </tr>
              ) : (
                filteredTransactions.map((tx) => {
                  const isDebit = tx.type === 'DEBIT';
                  return (
                    <tr key={tx.id} style={{ borderBottom: '1px solid #2E2644' }}>
                      <td style={{ padding: '16px 24px', color: '#A098B5' }}>{formatDate(tx.date)}</td>
                      <td style={{ padding: '16px 24px', fontWeight: 600, color: '#FFF' }}>{tx.description}</td>
                      <td style={{ padding: '16px 24px' }}>
                        <span style={{ fontSize: '0.75rem', padding: '4px 8px', borderRadius: '4px', backgroundColor: '#14101F', border: '1px solid #2E2644', color: '#A098B5' }}>
                          {tx.pluggyCategory || 'Geral'}
                        </span>
                      </td>
                      <td style={{ padding: '16px 24px' }}>
                        <span className="badge-status badge-primary">
                          {tx.internalCategory || 'OUTROS'}
                        </span>
                      </td>
                      <td style={{ padding: '16px 24px' }}>
                        {isDebit ? (
                          <span style={{ color: '#EB2478', display: 'flex', alignItems: 'center', gap: '4px', fontWeight: 600, fontSize: '0.8rem' }}>
                            <ArrowUpRight size={14} /> DÉBITO
                          </span>
                        ) : (
                          <span style={{ color: '#CAF33C', display: 'flex', alignItems: 'center', gap: '4px', fontWeight: 600, fontSize: '0.8rem' }}>
                            <ArrowDownLeft size={14} /> CRÉDITO
                          </span>
                        )}
                      </td>
                      <td className="tabular-nums" style={{ padding: '16px 24px', textAlign: 'right', fontWeight: 700, color: isDebit ? '#EB2478' : '#CAF33C' }}>
                        {isDebit ? `- ${formatBRL(tx.amount)}` : `+ ${formatBRL(tx.amount)}`}
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>
      </div>

    </div>
  );
};
