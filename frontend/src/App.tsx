import React from 'react';
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { Layout } from './components/Layout';
import { OverviewPage } from './pages/OverviewPage';
import { TransactionsPage } from './pages/TransactionsPage';
import { InvoicesPage } from './pages/InvoicesPage';
import { BudgetsPage } from './pages/BudgetsPage';
import { ConnectionsPage } from './pages/ConnectionsPage';

export const App: React.FC = () => {
  return (
    <BrowserRouter>
      <Layout>
        <Routes>
          <Route path="/" element={<OverviewPage />} />
          <Route path="/extrato" element={<TransactionsPage />} />
          <Route path="/faturas" element={<InvoicesPage />} />
          <Route path="/orcamentos" element={<BudgetsPage />} />
          <Route path="/conexoes" element={<ConnectionsPage />} />
        </Routes>
      </Layout>
    </BrowserRouter>
  );
};

export default App;
