import React from 'react';
import { RefreshCw, PlusCircle } from 'lucide-react';

interface HeaderProps {
  title: string;
  onOpenSync: () => void;
  onConnectPluggy: () => void;
}

export const Header: React.FC<HeaderProps> = ({ title, onOpenSync, onConnectPluggy }) => {
  return (
    <header
      style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        padding: '24px 32px',
        borderBottom: '1px solid #2E2644',
        backgroundColor: '#14101F',
      }}
    >
      <div>
        <h2 style={{ fontSize: '1.5rem', fontWeight: 700, color: '#FFF', fontFamily: 'Space Grotesk' }}>{title}</h2>
      </div>

      <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
        <button className="btn-outline-fintech" onClick={onOpenSync}>
          <RefreshCw size={16} />
          <span>Sincronizar</span>
        </button>

        <button className="btn-primary-fintech" onClick={onConnectPluggy}>
          <PlusCircle size={16} />
          <span>Conectar Banco (Pluggy)</span>
        </button>
      </div>
    </header>
  );
};
