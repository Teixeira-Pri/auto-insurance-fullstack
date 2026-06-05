# Auto Insurance Frontend

Interface web para a [Auto Insurance API](../auto-insurance-api/README.md), desenvolvida com React 18 e TypeScript.

## Tecnologias

- **React 18** + TypeScript
- **Vite 5** — bundler e dev server (porta 3000)
- **Tailwind CSS 3** — estilização
- **React Hook Form** — formulários e validação
- **TanStack React Query** — gerenciamento de estado assíncrono
- **Recharts** — gráficos de fatores de risco (barra + pizza)
- **Axios** — cliente HTTP com proxy para o backend
- **Sonner** — notificações toast
- **react-input-mask** — máscaras para CPF, CEP e telefone

## Pré-requisitos

- Node.js 18+
- Backend rodando em `http://localhost:8080`

## Como Executar

```bash
# Instalar dependências
npm install

# Servidor de desenvolvimento (http://localhost:3000)
npm run dev

# Build de produção
npm run build

# Preview do build de produção
npm run preview
```

## Estrutura

```
src/
├── components/
│   ├── InsuranceForm.tsx    # Formulário principal (condutor + veículo)
│   └── ResultsDisplay.tsx   # Exibição da cotação com gráficos
├── services/
│   └── api.ts               # Cliente Axios configurado
├── types/
│   └── api.ts               # Tipos TypeScript alinhados com o backend
├── utils/
│   └── formatters.ts        # Validação de CPF, máscaras, formatadores
├── App.tsx
├── main.tsx
├── index.css
└── declarations.d.ts        # Declarações de módulos sem tipos nativos
```

## Proxy de Desenvolvimento

O Vite está configurado para fazer proxy de `/api` para `http://localhost:8080`,
eliminando problemas de CORS em desenvolvimento. Ver [vite.config.ts](vite.config.ts).

## Variáveis de Ambiente

Nenhuma variável de ambiente é necessária. O proxy do Vite gerencia o roteamento
para o backend em desenvolvimento. Em produção, sirva o build estático e configure
seu servidor web (Nginx, etc.) para fazer proxy de `/api` para o backend.
