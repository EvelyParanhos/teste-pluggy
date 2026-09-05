import { useEffect } from 'react';
import { financeApi } from '../api/financeApi';

declare global {
  interface Window {
    PluggyConnect: any;
  }
}

export const usePluggyConnect = () => {
  useEffect(() => {
    // Inject Pluggy Connect script if not already present
    if (!document.getElementById('pluggy-connect-script')) {
      const script = document.createElement('script');
      script.id = 'pluggy-connect-script';
      script.src = 'https://cdn.pluggy.ai/pluggy-connect/v2.8.2/pluggy-connect.js';
      script.async = true;
      document.head.appendChild(script);
    }
  }, []);

  const openConnect = async (onSuccessCallback: (itemId: string) => void) => {
    try {
      const { accessToken } = await financeApi.getConnectToken();
      if (!accessToken) {
        alert('Erro: Token de acesso não retornado pelo servidor.');
        return;
      }

      if (!window.PluggyConnect) {
        alert('O SDK do Pluggy Connect ainda está carregando. Tente novamente em alguns segundos.');
        return;
      }

      const pluggyConnect = new window.PluggyConnect({
        connectToken: accessToken,
        includeSandbox: true,
        onSuccess: (itemData: any) => {
          console.log('Conexão Pluggy com sucesso:', itemData);
          const itemId = itemData?.item?.id;
          if (itemId) {
            financeApi.triggerSync(itemId).then(() => {
              onSuccessCallback(itemId);
            });
          }
        },
        onError: (error: any) => {
          console.error('Erro no Pluggy Connect:', error);
          alert('Erro no widget Pluggy Connect: ' + (error.message || JSON.stringify(error)));
        },
      });

      pluggyConnect.init();
    } catch (err: any) {
      console.error('Erro ao abrir Pluggy Connect:', err);
      alert('Erro ao comunicar com o servidor: ' + err.message);
    }
  };

  return { openConnect };
};
