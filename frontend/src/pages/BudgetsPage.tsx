import React, { useEffect, useState } from 'react';
import { Target, PlusCircle, AlertTriangle, CheckCircle2 } from 'lucide-react';
import { financeApi } from '../api/financeApi';
import type { CategoryBudgetStatus, BudgetAlertLog } from '../types/finance';
import { BudgetModal } from '../components/BudgetModal';

export const BudgetsPage: React.FC = () => {
  const [budgets, setBudgets] = useState<CategoryBudgetStatus[]>([]);
  const [alerts, setAlerts] = useState<BudgetAlertLog[]>([]);
  const [isModalOpen, setIsModalOpen] = useState<boolean>(false);
  const [loading, setLoading] = useState<boolean>(true);

  const formatBRL = (val: number) => {
    return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(val);
  };

  const formatDate = (dateStr?: string) => {
    if (!dateStr) return '-';
    return new Date(dateStr).toLocaleString('pt-BR');
  };

  useEffect(() => {
    loadBudgets();
  }, []);

  const loadBudgets = async () => {
    setLoading(true);
    try {
      const [budRes, altRes] = await Promise.all([
        financeApi.getBudgets(),
        financeApi.getBudgetAlerts(),
      ]);
      setBudgets(budRes);
      setAlerts(altRes);
    } catch (err) {
      console.error('Erro ao carregar orçamentos:', err);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '32px' }}>
      
      {/* Header Actions */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '16px' }}>
        <div>
          <h3 style={{ fontSize: '1.4rem', fontWeight: 700, fontFamily: 'Space Grotesk', marginBottom: '4px' }}>Motor de Orçamentos</h3>
          <p style={{ fontSize: '0.85rem', color: '#A098B5' }}>Defina tetos de gastos por categoria e monitore alertas em tempo real.</p>
        </div>

        <button className="btn-primary-fintech" onClick={() => setIsModalOpen(true)}>
          <PlusCircle size={16} />
          <span>Definir Limite de Categoria</span>
        </button>
      </div>

      {/* Grid: Orçamentos vs Alertas */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))', gap: '24px' }}>
        
        {/* Coluna 1: Lista de Orçamentos com Barras de Progresso */}
        <div className="card-fintech">
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '20px' }}>
            <Target size={20} color="#5B37C4" />
            <h4 style={{ fontSize: '1.1rem', fontWeight: 700, fontFamily: 'Space Grotesk' }}>Limites do Mês Vigente</h4>
          </div>

          {loading ? (
            <div style={{ padding: '24px', textAlign: 'center', color: '#A098B5' }}>Carregando limites...</div>
          ) : budgets.length === 0 ? (
            <div style={{ padding: '24px', textAlign: 'center', color: '#A098B5' }}>
              Nenhum limite de orçamento cadastrado ainda. Clique em "Definir Limite de Categoria".
            </div>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
              {budgets.map((b) => {
                const isExceeded = b.status === 'EXCEEDED';
                const isWarn = b.status === 'WARN';
                const color = isExceeded ? '#EB2478' : isWarn ? '#FF8E1B' : '#CAF33C';

                return (
                  <div key={b.id} style={{ borderBottom: '1px solid #2E2644', paddingBottom: '16px' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px' }}>
                      <div>
                        <strong style={{ fontSize: '0.95rem', color: '#FFF', display: 'block' }}>{b.categoryDescription}</strong>
                        {isExceeded ? (
                          <span className="badge-status badge-critical">ESTOURADO</span>
                        ) : isWarn ? (
                          <span className="badge-status badge-warning">ALERTA (&gt;80%)</span>
                        ) : (
                          <span className="badge-status badge-positive">NORMAL</span>
                        )}
                      </div>
                      <div className="tabular-nums" style={{ textAlign: 'right' }}>
                        <span style={{ fontSize: '0.9rem', fontWeight: 700, color: '#FFF' }}>{formatBRL(b.currentSpent)}</span>
                        <span style={{ fontSize: '0.8rem', color: '#A098B5', display: 'block' }}>/ {formatBRL(b.monthlyLimit)}</span>
                      </div>
                    </div>

                    <div className="progress-bar-bg">
                      <div className="progress-bar-fill" style={{ width: `${Math.min(b.percentageUsed, 100)}%`, backgroundColor: color }} />
                    </div>

                    <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: '6px', fontSize: '0.75rem', color: '#A098B5' }}>
                      <span>Utilizado: {b.percentageUsed}%</span>
                      <span>Restante: {formatBRL(Math.max(b.monthlyLimit - b.currentSpent, 0))}</span>
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>

        {/* Coluna 2: Log de Alertas Disparados */}
        <div className="card-fintech">
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '20px' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <AlertTriangle size={20} color="#FF8E1B" />
              <h4 style={{ fontSize: '1.1rem', fontWeight: 700, fontFamily: 'Space Grotesk' }}>Alertas Disparados no Mês</h4>
            </div>
            <span className="badge-status badge-warning">{alerts.length} Alertas</span>
          </div>

          {loading ? (
            <div style={{ padding: '24px', textAlign: 'center', color: '#A098B5' }}>Carregando alertas...</div>
          ) : alerts.length === 0 ? (
            <div style={{ padding: '32px', textAlign: 'center', color: '#A098B5' }}>
              <CheckCircle2 size={36} color="#CAF33C" style={{ marginBottom: '8px' }} />
              <p>Nenhum teto de orçamento foi ultrapassado neste mês!</p>
            </div>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
              {alerts.map((alt) => (
                <div key={alt.id} style={{ padding: '16px', borderRadius: '12px', backgroundColor: '#14101F', border: '1px solid #FF8E1B', display: 'flex', alignItems: 'flex-start', gap: '12px' }}>
                  <AlertTriangle size={20} color="#FF8E1B" style={{ marginTop: '2px' }} />
                  <div>
                    <strong style={{ fontSize: '0.9rem', color: '#FFF', display: 'block' }}>{alt.category} - Limite Ultrapassado</strong>
                    <span className="tabular-nums" style={{ fontSize: '0.8rem', color: '#A098B5' }}>
                      Gasto acumulado: <strong style={{ color: '#EB2478' }}>{formatBRL(alt.currentSpent)}</strong> (Limite: {formatBRL(alt.monthlyLimit)})
                    </span>
                    <span style={{ fontSize: '0.75rem', color: '#6B7280', display: 'block', marginTop: '4px' }}>{formatDate(alt.createdAt)}</span>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

      </div>

      {/* Modal de cadastro de limite */}
      <BudgetModal isOpen={isModalOpen} onClose={() => setIsModalOpen(false)} onSuccess={loadBudgets} />

    </div>
  );
};
