# 🎨 Auto Insurance Calculator — Frontend React
 
Frontend moderno e responsivo para a API de Cálculo de Seguro Auto.
 
## 🚀 Stack Tecnológica
 
| Tecnologia | Versão | Propósito |
|-----------|--------|-----------|
| React | 18 | Framework UI |
| TypeScript | 5.2 | Tipagem estática |
| Vite | 5 | Build tool ultra-rápido |
| Tailwind CSS | 3.3 | Utility-first CSS |
| React Query | 5 | State management assíncrono |
| React Hook Form | 7.4 | Validação de formulários |
| Recharts | 2.10 | Gráficos interativos |
| Axios | 1.6 | Cliente HTTP |
| Sonner | 1.2 | Toast notifications |
| Lucide React | 0.29 | Ícones |
 
---
 
## 📦 Pré-requisitos
 
- Node.js 18+ → https://nodejs.org
- Backend rodando em `localhost:8080` (opcional para testar integração)
---
 
## ⚙️ Instalação
 
```bash
# 1. Entre na pasta do frontend
cd auto-insurance-frontend
 
# 2. Instale as dependências (incluindo Tailwind)
npm install
 
# 3. Verifique se esses arquivos existem na raiz:
# ✅ tailwind.config.js
# ✅ postcss.config.js   ← OBRIGATÓRIO para o Tailwind funcionar
# ✅ vite.config.ts
```
 
### ⚠️ Se o Tailwind não estiver aplicando estilos
 
Crie o arquivo `postcss.config.js` na raiz do projeto:
 
```javascript
export default {
  plugins: {
    tailwindcss: {},
    autoprefixer: {},
  },
}
```
 
---
 
## 🏃 Executar
 
### Modo Desenvolvimento
 
```bash
npm run dev
```
 
Acesse: **http://localhost:3000** (ou **http://localhost:3001** se a porta 3000 estiver em uso)
 
### Build de Produção
 
```bash
npm run build
```
 
Os arquivos otimizados serão gerados em `dist/`
 
### Preview da Build
 
```bash
npm run preview
```
 
---
 
## 🎯 Funcionalidades
 
### ✅ Formulário Completo
- Validação CPF em tempo real (algoritmo de dígitos verificadores)
- Máscaras automáticas para CEP, telefone e placa
- Cálculo automático de idade ao preencher data de nascimento
- Validação de campos obrigatórios com mensagens claras
### ✅ Exibição de Resultados
- **Gráfico de barras** — Breakdown visual dos 8 fatores de risco
  - 🟢 Verde = fator de desconto (multiplicador < 1.0)
  - 🔴 Vermelho = fator de acréscimo (multiplicador > 1.0)
- **Gráfico de pizza** — Composição do prêmio (base vs. fatores de risco)
- **Cards de resumo** — Prêmio anual, mensal e multiplicador total
- **Badge de perfil** — BAIXO / MÉDIO / ALTO com cores distintas
### ✅ UX Moderna
- Design responsivo (mobile-first)
- Loading states com spinners animados
- Toast notifications de sucesso e erro (Sonner)
- Scroll suave até os resultados após cálculo
- Botão de impressão da cotação
### ✅ Integração com API
- Proxy Vite: `/api` → `http://localhost:8080`
- Tratamento de erros HTTP (400, 422, 429, 500)
- Exibição de erros de validação campo a campo (vindos do backend)
- Retry automático com React Query
---
 
## 📁 Estrutura do Projeto
 
```
auto-insurance-frontend/
├── src/
│   ├── components/
│   │   ├── InsuranceForm.tsx      # Formulário com validações
│   │   └── ResultsDisplay.tsx     # Gráficos e resultados
│   ├── services/
│   │   └── api.ts                 # Cliente Axios + interceptors
│   ├── types/
│   │   └── api.ts                 # Types sincronizados com DTOs do backend
│   ├── utils/
│   │   └── formatters.ts          # validateCPF, formatCEP, formatCurrency...
│   ├── App.tsx                    # QueryClientProvider + Toaster
│   ├── main.tsx                   # Entry point
│   └── index.css                  # @tailwind directives
├── postcss.config.js              # OBRIGATÓRIO para Tailwind CSS
├── tailwind.config.js             # Configuração do Tailwind
├── vite.config.ts                 # Proxy /api → :8080
├── tsconfig.json
└── package.json
```
 
---
 
## 🔌 Integração com Backend
 
O Vite redireciona as chamadas da API automaticamente:
 
```typescript
// vite.config.ts
server: {
  proxy: {
    '/api': {
      target: 'http://localhost:8080',
      changeOrigin: true,
    }
  }
}
```
 
**Fluxo:**
```
Frontend (localhost:3001) → POST /api/v1/insurance/calculate
         ↓ Proxy Vite
Backend  (localhost:8080) → POST /api/v1/insurance/calculate
```
 
---
 
## 🧪 Dados de Teste
 
Use estes dados para testar a aplicação:
 
### Condutor
 
| Campo | Valor |
|-------|-------|
| Nome | Priscila Batista |
| CPF | **529.982.247-25** ← CPF válido |
| Data Nascimento | 03/11/1994 |
| Gênero | Feminino |
| CEP | 02068-030 |
| CNH | 21/01/2021 |
| Sinistros | 0 |
 
### Veículo
 
| Campo | Valor |
|-------|-------|
| Marca | Toyota |
| Modelo | Corolla |
| Ano Fabricação | 2020 |
| Ano Modelo | 2021 |
| Combustível | Flex |
| Categoria | Sedan |
| Condição | Bom |
| Valor | R$ 45.000,00 |
 
### Resultado Esperado
 
| Métrica | Valor |
|---------|-------|
| Prêmio base (5%) | R$ 2.250,00 |
| Multiplicador total | ~1.36x |
| Prêmio anual | ~R$ 3.060,00 |
| Prêmio mensal | ~R$ 255,00 |
| Perfil de risco | MÉDIO |
 
---
 
## 🚀 Deploy
 
### Vercel (Recomendado)
 
```bash
npm install -g vercel
vercel --prod
```
 
Configurar variável de ambiente no painel Vercel:
```
VITE_API_URL=https://sua-api.com
```
 
### Docker
 
```dockerfile
FROM node:18-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build
 
FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```
 
```bash
docker build -t auto-insurance-frontend .
docker run -p 80:80 auto-insurance-frontend
```
 
---
 
## 🗺️ Roadmap
 
- [ ] Dark mode
- [ ] Histórico de cotações (localStorage)
- [ ] Exportar cotação em PDF
- [ ] Comparar múltiplas cotações lado a lado
- [ ] PWA (funcionar offline)
- [ ] Testes E2E com Cypress
- [ ] i18n (inglês / espanhol)
---
 
## 📝 Licença
 
MIT License — sinta-se à vontade para usar, modificar e distribuir.
 
---
 
**Frontend production-ready! 🎨✨**