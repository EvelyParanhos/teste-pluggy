import React, { useState } from 'react';
import { useLocation } from 'react-router-dom';
import { Sidebar } from './Sidebar';
import { Header } from './Header';
import { SyncModal } from './SyncModal';
import { usePluggyConnect } from './PluggyConnectModal';
import { financeApi } from '../api/financeApi';
import type { Item } from '../types/finance';

interface LayoutProps {
  children: React.ReactNode;
}

export const Layout: React.FC<LayoutProps> = ({ children }) => {
  const location = useLocation();
  const [isSyncModalOpen, setIsSyncModalOpen] = useState<boolean>(false);
  const [items, setItems] = useState<Item[]>([]);
  const { openConnect } = usePluggyConnect();

  const getPageTitle = (path: string) => {
    switch (path) {
      case '/':
        return 'Visão Geral';
      case '/extrato':
        return 'Extrato de Transações';
      case '/faturas':
        return 'Faturas de Cartão de Crédito';
      case '/orcamentos':
        return 'Orçamentos & Alertas';
      case '/conexoes':
        return 'Contas Conectadas';
      default:
        return 'Meu Financeiro';
    }
  };

  const loadItems = async () => {
    try {
      const data = await financeApi.getItems();
      setItems(data);
    } catch (err) {
      console.error('Erro ao carregar items:', err);
    }
  };

  const handleOpenSync = () => {
    loadItems();
    setIsSyncModalOpen(true);
  };

  return (
    <div style={{ display: 'flex', minHeight: '100vh', backgroundColor: '#14101F' }}>
      {/* Sidebar */}
      <Sidebar />

      {/* Main Content Area */}
      <div style={{ flex: 1, display: 'flex', flexDirection: 'column' }}>
        <Header
          title={getPageTitle(location.pathname)}
          onOpenSync={handleOpenSync}
          onConnectPluggy={() => openConnect(() => window.location.reload())}
        />

        <main style={{ padding: '32px', flex: 1 }}>
          {children}
        </main>
      </div>

      {/* Sync Modal */}
      <SyncModal
        isOpen={isSyncModalOpen}
        onClose={() => setIsSyncModalOpen(false)}
        items={items}
        onSuccess={() => window.location.reload()}
      />
    </div>
  );
};
