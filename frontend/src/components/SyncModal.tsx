import React, { useState } from 'react';
import { X, RefreshCw, CheckCircle2, AlertCircle } from 'lucide-react';
import { financeApi } from '../api/financeApi';
import type { Item } from '../types/finance';

interface SyncModalProps {
  isOpen: boolean;
  onClose: () => void;
  items: Item[];
  onSuccess: () => void;
}

export const SyncModal: React.FC<SyncModalProps> = ({ isOpen, onClose, items, onSuccess }) => {
  const [selectedItemId, setSelectedItemId] = useState<string>('');
  const [customItemId, setCustomItemId] = useState<string>('c7455564-cdb4-4b0d-8fe4-37223a2a1aa7');
  const [loading, setLoading] = useState<boolean>(false);
  const [message, setMessage] = useState<{ text: string; type: 'success' | 'error' | 'info' } | null>(null);

  if (!isOpen) return null;

  const handleSync = async () => {
    const targetItemId = selectedItemId || customItemId.trim();
    if (!targetItemId) {
      setMessage({ text: 'Por favor, selecione ou digite um Item ID válido.', type: 'error' });
      return;
    }

    setLoading(true);
    setMessage({ text: 'Sincronizando extratos e contas junto à API Pluggy...', type: 'info' });

    try {
      const res = await financeApi.triggerSync(targetItemId);
      setMessage({ text: res.message || 'Sincronização concluída com sucesso!', type: 'success' });
      setTimeout(() => {
        onSuccess();
        onClose();
        setMessage(null);
      }, 1500);
    } catch (err: any) {
      setMessage({ text: 'Erro ao sincronizar: ' + (err.response?.data?.message || err.message), type: 'error' });
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
          <h3 style={{ fontSize: '1.2rem', fontWeight: 700, fontFamily: 'Space Grotesk' }}>Sincronizar Conexão Pluggy</h3>
          <button onClick={onClose} style={{ background: 'none', border: 'none', color: '#A098B5', cursor: 'pointer' }}>
            <X size={20} />
          </button>
        </div>

        <p style={{ fontSize: '0.85rem', color: '#A098B5', marginBottom: '20px' }}>
          Dispare a sincronização sob demanda para buscar transações e saldos mais recentes da Pluggy.
        </p>

        <div style={{ marginBottom: '16px' }}>
          <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 600, color: '#A098B5', marginBottom: '8px' }}>
            Selecione um Item Salvo:
          </label>
          <select
            value={selectedItemId}
            onChange={(e) => setSelectedItemId(e.target.value)}
            style={{
              width: '100%',
              padding: '10px',
              borderRadius: '8px',
              backgroundColor: '#14101F',
              border: '1px solid #2E2644',
              color: '#FFF',
            }}
          >
            <option value="">-- Selecione uma conta salva --</option>
            {items.map((it) => (
              <option key={it.id} value={it.pluggyItemId.toString()}>
                {it.connectorName || 'Conexão'} ({it.pluggyItemId})
              </option>
            ))}
          </select>
        </div>

        <div style={{ marginBottom: '20px' }}>
          <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 600, color: '#A098B5', marginBottom: '8px' }}>
            Ou digite o Item ID manualmente:
          </label>
          <input
            type="text"
            value={customItemId}
            onChange={(e) => setCustomItemId(e.target.value)}
            placeholder="Ex: c7455564-cdb4-4b0d-8fe4-37223a2a1aa7"
            style={{
              width: '100%',
              padding: '10px',
              borderRadius: '8px',
              backgroundColor: '#14101F',
              border: '1px solid #2E2644',
              color: '#FFF',
            }}
          />
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
              backgroundColor:
                message.type === 'success'
                  ? 'rgba(202, 243, 60, 0.15)'
                  : message.type === 'error'
                  ? 'rgba(235, 36, 120, 0.15)'
                  : 'rgba(91, 55, 196, 0.15)',
              color:
                message.type === 'success'
                  ? '#CAF33C'
                  : message.type === 'error'
                  ? '#EB2478'
                  : '#A78BFA',
            }}
          >
            {message.type === 'success' ? (
              <CheckCircle2 size={18} />
            ) : message.type === 'error' ? (
              <AlertCircle size={18} />
            ) : (
              <RefreshCw size={18} className="spin" />
            )}
            <span>{message.text}</span>
          </div>
        )}

        <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '12px' }}>
          <button className="btn-outline-fintech" onClick={onClose} disabled={loading}>
            Cancelar
          </button>
          <button className="btn-primary-fintech" onClick={handleSync} disabled={loading}>
            {loading ? <RefreshCw size={16} className="spin" /> : <RefreshCw size={16} />}
            <span>{loading ? 'Sincronizando...' : 'Iniciar Sincronização'}</span>
          </button>
        </div>
      </div>
    </div>
  );
};
