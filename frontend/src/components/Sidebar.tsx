import React from 'react';
import { NavLink } from 'react-router-dom';
import {
  LayoutDashboard,
  Receipt,
  CreditCard,
  Target,
  Link2,
  Wallet
} from 'lucide-react';

export const Sidebar: React.FC = () => {
  const menuItems = [
    { path: '/', label: 'Visão Geral', icon: LayoutDashboard },
    { path: '/extrato', label: 'Extrato', icon: Receipt },
    { path: '/faturas', label: 'Faturas', icon: CreditCard },
    { path: '/orcamentos', label: 'Orçamentos', icon: Target },
    { path: '/conexoes', label: 'Contas Conectadas', icon: Link2 },
  ];

  return (
    <aside
      style={{
        width: '260px',
        backgroundColor: '#14101F',
        borderRight: '1px solid #2E2644',
        display: 'flex',
        flexDirection: 'column',
        height: '100vh',
        position: 'sticky',
        top: 0,
      }}
    >
      {/* Brand Logo */}
      <div style={{ padding: '24px', display: 'flex', alignItems: 'center', gap: '12px', borderBottom: '1px solid #2E2644' }}>
        <div style={{ backgroundColor: '#5B37C4', padding: '10px', borderRadius: '12px', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
          <Wallet size={24} color="#CAF33C" />
        </div>
        <div>
          <h1 style={{ fontSize: '1.1rem', fontWeight: 700, color: '#FFF', fontFamily: 'Space Grotesk' }}>Meu Financeiro</h1>
          <span style={{ fontSize: '0.75rem', color: '#CAF33C', fontWeight: 600 }}>OPEN FINANCE</span>
        </div>
      </div>

      {/* Navigation Menu */}
      <nav style={{ padding: '20px 16px', flex: 1, display: 'flex', flexDirection: 'column', gap: '8px' }}>
        {menuItems.map((item) => {
          const Icon = item.icon;
          return (
            <NavLink
              key={item.path}
              to={item.path}
              style={({ isActive }) => ({
                display: 'flex',
                alignItems: 'center',
                gap: '12px',
                padding: '12px 16px',
                borderRadius: '12px',
                fontSize: '0.9rem',
                fontWeight: isActive ? 700 : 500,
                color: isActive ? '#FFFFFF' : '#A098B5',
                backgroundColor: isActive ? '#5B37C4' : 'transparent',
                textDecoration: 'none',
                transition: 'all 0.2s ease',
              })}
            >
              <Icon size={20} />
              <span>{item.label}</span>
            </NavLink>
          );
        })}
      </nav>

      {/* Footer Info */}
      <div style={{ padding: '20px', borderTop: '1px solid #2E2644', fontSize: '0.75rem', color: '#A098B5' }}>
        <p>Pluggy Open Finance API v2</p>
        <p style={{ color: '#CAF33C', marginTop: '4px' }}>Status: Conectado</p>
      </div>
    </aside>
  );
};
