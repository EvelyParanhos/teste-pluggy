import React, { useEffect, useState } from 'react';
import { Link2, Building2, FileText } from 'lucide-react';
import { financeApi } from '../api/financeApi';
import type { Account, SyncLog } from '../types/finance';
import { usePluggyConnect } from '../components/PluggyConnectModal';

export const ConnectionsPage: React.FC = () => {
  const [accounts, setAccounts] = useState<Account[]>([]);
  const [syncLogs, setSyncLogs] = useState<SyncLog[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const { openConnect } = usePluggyConnect();

  const formatBRL = (val: number) => {
    return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(val);
  };

  const formatDate = (dateStr?: string) => {
    if (!dateStr) return '-';
    return new Date(dateStr).toLocaleString('pt-BR');
  };

  useEffect(() => {
    loadConnections();
  }, []);

  const loadConnections = async () => {
    setLoading(true);
    try {
      const [accRes, logRes] = await Promise.all([
        financeApi.getAccounts(),
        financeApi.getSyncLogs(),
      ]);
      setAccounts(accRes);
      setSyncLogs(logRes);
    } catch (err) {
      console.error('Erro ao carregar conexões:', err);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '32px' }}>
      
      {/* Header Actions */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '16px' }}>
        <div>
          <h3 style={{ fontSize: '1.4rem', fontWeight: 700, fontFamily: 'Space Grotesk', marginBottom: '4px' }}>Contas Conectadas (Open Finance)</h3>
          <p style={{ fontSize: '0.85rem', color: '#A098B5' }}>Gerencie suas instituições bancárias e visualize o status de sincronização em tempo real.</p>
        </div>

        <button className="btn-primary-fintech" onClick={() => openConnect(loadConnections)}>
          <Link2 size={16} />
          <span>Vincular Nova Conta (Pluggy Connect)</span>
        </button>
      </div>

      {/* Grid de Contas Salvas */}
      <div>
        <h4 style={{ fontSize: '1.1rem', fontWeight: 700, fontFamily: 'Space Grotesk', marginBottom: '16px', color: '#FFF' }}>
          Contas Financeiras ({accounts.length})
        </h4>

        {loading ? (
          <div className="card-fintech" style={{ textAlign: 'center', color: '#A098B5' }}>Carregando contas...</div>
        ) : accounts.length === 0 ? (
          <div className="card-fintech" style={{ padding: '32px', textAlign: 'center', color: '#A098B5' }}>
            Nenhuma conta bancária vinculada ainda. Clique em "Vincular Nova Conta" acima.
          </div>
        ) : (
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: '16px' }}>
            {accounts.map((acc) => (
              <div key={acc.id} className="card-fintech" style={{ padding: '20px' }}>
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '12px' }}>
                  <span className="badge-status badge-primary">{acc.subtype || acc.type}</span>
                  <span style={{ fontSize: '0.75rem', color: '#A098B5' }}>{acc.number ? `Nº ${acc.number}` : ''}</span>
                </div>

                <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '16px' }}>
                  <div style={{ width: '36px', height: '36px', borderRadius: '10px', backgroundColor: '#241D37', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                    <Building2 size={20} color="#5B37C4" />
                  </div>
                  <div>
                    <h5 style={{ fontSize: '1rem', fontWeight: 700, color: '#FFF' }}>{acc.name}</h5>
                    <span style={{ fontSize: '0.75rem', color: '#A098B5' }}>Moeda: {acc.currencyCode}</span>
                  </div>
                </div>

                <div className="tabular-nums" style={{ fontSize: '1.4rem', fontWeight: 700, color: '#CAF33C' }}>
                  {formatBRL(acc.balance)}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Histórico de Logs de Sincronização */}
      <div className="card-fintech">
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '20px' }}>
          <FileText size={20} color="#5B37C4" />
          <h4 style={{ fontSize: '1.1rem', fontWeight: 700, fontFamily: 'Space Grotesk' }}>Histórico de Sincronizações (SyncLog)</h4>
        </div>

        {loading ? (
          <div style={{ padding: '16px', color: '#A098B5', textAlign: 'center' }}>Carregando logs...</div>
        ) : syncLogs.length === 0 ? (
          <div style={{ padding: '16px', color: '#A098B5', textAlign: 'center' }}>Nenhum log de sincronização registrado.</div>
        ) : (
          <div style={{ overflowX: 'auto' }}>
            <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '0.85rem' }}>
              <thead>
                <tr style={{ borderBottom: '1px solid #2E2644', color: '#A098B5', fontSize: '0.75rem', textTransform: 'uppercase' }}>
                  <th style={{ padding: '12px' }}>Item ID</th>
                  <th style={{ padding: '12px' }}>Status</th>
                  <th style={{ padding: '12px' }}>Tentativas</th>
                  <th style={{ padding: '12px' }}>Último Erro</th>
                  <th style={{ padding: '12px' }}>Última Atualização</th>
                </tr>
              </thead>
              <tbody>
                {syncLogs.map((log) => (
                  <tr key={log.id} style={{ borderBottom: '1px solid #2E2644' }}>
                    <td style={{ padding: '12px', fontWeight: 600, color: '#FFF' }}>{log.pluggyItemId}</td>
                    <td style={{ padding: '12px' }}>
                      {log.status === 'SUCCESS' ? (
                        <span className="badge-status badge-positive">SUCESSO</span>
                      ) : log.status === 'FAILED' ? (
                        <span className="badge-status badge-critical">FALHOU</span>
                      ) : (
                        <span className="badge-status badge-warning">PENDENTE</span>
                      )}
                    </td>
                    <td style={{ padding: '12px', color: '#A098B5' }}>{log.attempts}</td>
                    <td style={{ padding: '12px', color: log.lastError ? '#EB2478' : '#A098B5' }}>{log.lastError || '-'}</td>
                    <td style={{ padding: '12px', color: '#A098B5' }}>{formatDate(log.updatedAt)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

    </div>
  );
};
