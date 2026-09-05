import React, { useState } from 'react';
import { X, Target, CheckCircle2, AlertCircle } from 'lucide-react';
import { financeApi } from '../api/financeApi';

interface BudgetModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccess: () => void;
}

export const BudgetModal: React.FC<BudgetModalProps> = ({ isOpen, onClose, onSuccess }) => {
  const [category, setCategory] = useState<string>('ALIMENTACAO');
  const [monthlyLimit, setMonthlyLimit] = useState<string>('1000');
  const [threshold, setThreshold] = useState<string>('80');
  const [loading, setLoading] = useState<boolean>(false);
  const [message, setMessage] = useState<{ text: string; type: 'success' | 'error' } | null>(null);

  if (!isOpen) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const limitNum = parseFloat(monthlyLimit);
    const thresholdNum = parseFloat(threshold);

    if (isNaN(limitNum) || limitNum <= 0) {
      setMessage({ text: 'Por favor, insira um limite mensal válido.', type: 'error' });
      return;
    }

    setLoading(true);
    try {
      await financeApi.saveBudget({
        category,
        monthlyLimit: limitNum,
        alertThresholdPercentage: thresholdNum,
      });

      setMessage({ text: 'Limite de orçamento salvo com sucesso!', type: 'success' });
      setTimeout(() => {
        onSuccess();
        onClose();
        setMessage(null);
      }, 1200);
    } catch (err: any) {
      setMessage({ text: 'Erro ao salvar orçamento: ' + (err.response?.data?.message || err.message), type: 'error' });
    } finally {
      setLoading(false);
    }
  };

  return (
    <div
      style={{
        position: 'fixed',
        top: 0,
        left: 0,
        right: 0,
        bottom: 0,
        backgroundColor: 'rgba(0, 0, 0, 0.75)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        zIndex: 1000,
        backdropFilter: 'blur(4px)',
      }}
    >
      <div className="card-fintech" style={{ width: '100%', maxWidth: '480px' }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '20px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <Target size={20} color="#CAF33C" />
            <h3 style={{ fontSize: '1.2rem', fontWeight: 700, fontFamily: 'Space Grotesk' }}>Definir Limite de Orçamento</h3>
          </div>
          <button onClick={onClose} style={{ background: 'none', border: 'none', color: '#A098B5', cursor: 'pointer' }}>
            <X size={20} />
          </button>
        </div>

        <form onSubmit={handleSubmit}>
          <div style={{ marginBottom: '16px' }}>
            <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 600, color: '#A098B5', marginBottom: '8px' }}>
              Categoria Interna
            </label>
            <select
              value={category}
              onChange={(e) => setCategory(e.target.value)}
              style={{
                width: '100%',
                padding: '10px',
                borderRadius: '8px',
                backgroundColor: '#14101F',
                border: '1px solid #2E2644',
                color: '#FFF',
              }}
            >
              <option value="ALIMENTACAO">Alimentação e Restaurantes</option>
              <option value="MORADIA">Moradia e Contas Domésticas</option>
              <option value="TRANSPORTE">Transporte e Combustível</option>
              <option value="LAZER">Lazer e Entretenimento</option>
              <option value="SAUDE">Saúde e Cuidados Pessoais</option>
              <option value="EDUCACAO">Educação</option>
              <option value="COMPRAS">Compras e Vestuário</option>
              <option value="SERVICOS">Serviços e Assinaturas</option>
              <option value="IMPOSTOS">Impostos e Taxas</option>
              <option value="OUTROS">Outros / Não Categorizado</option>
            </select>
          </div>

          <div style={{ marginBottom: '16px' }}>
            <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 600, color: '#A098B5', marginBottom: '8px' }}>
              Limite Mensal (R$)
            </label>
            <input
              type="number"
              step="0.01"
              value={monthlyLimit}
              onChange={(e) => setMonthlyLimit(e.target.value)}
              placeholder="Ex: 1000.00"
              style={{
                width: '100%',
                padding: '10px',
                borderRadius: '8px',
                backgroundColor: '#14101F',
                border: '1px solid #2E2644',
                color: '#FFF',
              }}
              required
            />
          </div>

          <div style={{ marginBottom: '20px' }}>
            <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 600, color: '#A098B5', marginBottom: '8px' }}>
              Gatilho de Alerta (%)
            </label>
            <input
              type="number"
              value={threshold}
              onChange={(e) => setThreshold(e.target.value)}
              placeholder="80"
              style={{
                width: '100%',
                padding: '10px',
                borderRadius: '8px',
                backgroundColor: '#14101F',
                border: '1px solid #2E2644',
                color: '#FFF',
              }}
              required
            />
            <span style={{ fontSize: '0.75rem', color: '#A098B5', marginTop: '4px', display: 'block' }}>
              Alertar quando os gastos atingirem este percentual do limite.
            </span>
          </div>

          {message && (
            <div
              style={{
                padding: '12px',
                borderRadius: '8px',
                marginBottom: '20px',
                fontSize: '0.85rem',
                display: 'flex',
                alignItems: 'center',
                gap: '8px',
                backgroundColor: message.type === 'success' ? 'rgba(202, 243, 60, 0.15)' : 'rgba(235, 36, 120, 0.15)',
                color: message.type === 'success' ? '#CAF33C' : '#EB2478',
              }}
            >
              {message.type === 'success' ? <CheckCircle2 size={18} /> : <AlertCircle size={18} />}
              <span>{message.text}</span>
            </div>
          )}

          <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '12px' }}>
            <button type="button" className="btn-outline-fintech" onClick={onClose} disabled={loading}>
              Cancelar
            </button>
            <button type="submit" className="btn-primary-fintech" disabled={loading}>
              <span>{loading ? 'Salvando...' : 'Salvar Limite'}</span>
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
