import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, Cell, PieChart, Pie, Legend } from 'recharts';
import { TrendingUp, TrendingDown, Shield, DollarSign, Calendar, MapPin, Car, User } from 'lucide-react';
import type { CalculationResponse } from '../types/api';
import { formatCurrency } from '../utils/formatters';

interface ResultsDisplayProps {
  result: CalculationResponse;
}

export default function ResultsDisplay({ result }: ResultsDisplayProps) {
  // Prepara dados para o gráfico de barras (fatores de risco)
  const riskFactorsData = Object.entries(result.riskFactors).map(([name, value]) => ({
    name: name.replace(/do |da |de /g, ''),
    value: Number(value),
    color: value > 1.0 ? '#ef4444' : value < 1.0 ? '#10b981' : '#6b7280'
  }));

  // Dados para o gráfico de pizza (distribuição do prêmio)
  const premiumDistribution = [
    { name: 'Prêmio Base', value: result.basePremium, color: '#3b82f6' },
    { name: 'Fatores de Risco', value: result.totalPremium - result.basePremium, color: '#f59e0b' }
  ];

  return (
    <div className="space-y-6">
      {/* Header com Resumo */}
      <div className="bg-gradient-to-r from-primary-600 to-indigo-600 rounded-2xl shadow-xl p-8 text-white">
        <div className="flex items-start justify-between mb-6">
          <div>
            <h2 className="text-3xl font-bold mb-2">Cotação #{result.quoteId.slice(0, 8)}</h2>
            <p className="text-primary-100">
              Calculado em {new Date(result.calculatedAt).toLocaleString('pt-BR')}
            </p>
          </div>
          <div className={`px-4 py-2 rounded-full border-2 font-semibold ${
            result.riskProfile === 'BAIXO' ? 'bg-green-500 border-green-300' :
            result.riskProfile === 'MÉDIO' ? 'bg-yellow-500 border-yellow-300' :
            'bg-red-500 border-red-300'
          }`}>
            <Shield className="inline-block w-5 h-5 mr-2" />
            Risco {result.riskProfile}
          </div>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          <div className="bg-white/10 backdrop-blur-sm rounded-xl p-6">
            <div className="flex items-center gap-3 mb-2">
              <DollarSign className="w-8 h-8" />
              <span className="text-sm opacity-90">Prêmio Anual</span>
            </div>
            <p className="text-4xl font-bold">{formatCurrency(result.totalPremium)}</p>
          </div>

          <div className="bg-white/10 backdrop-blur-sm rounded-xl p-6">
            <div className="flex items-center gap-3 mb-2">
              <Calendar className="w-8 h-8" />
              <span className="text-sm opacity-90">Parcela Mensal</span>
            </div>
            <p className="text-4xl font-bold">{formatCurrency(result.monthlyPremium)}</p>
            <p className="text-sm opacity-75 mt-1">12x sem juros</p>
          </div>

          <div className="bg-white/10 backdrop-blur-sm rounded-xl p-6">
            <div className="flex items-center gap-3 mb-2">
              {result.totalRiskMultiplier < 1.2 ? (
                <TrendingDown className="w-8 h-8 text-green-300" />
              ) : (
                <TrendingUp className="w-8 h-8 text-red-300" />
              )}
              <span className="text-sm opacity-90">Multiplicador</span>
            </div>
            <p className="text-4xl font-bold">{result.totalRiskMultiplier.toFixed(2)}x</p>
          </div>
        </div>

        <div className="mt-6 p-4 bg-white/10 backdrop-blur-sm rounded-lg">
          <p className="text-lg">{result.message}</p>
        </div>
      </div>

      {/* Informações do Segurado */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <div className="bg-white rounded-xl shadow-lg p-6">
          <h3 className="text-xl font-semibold text-gray-800 mb-4 flex items-center gap-2">
            <User className="w-6 h-6 text-primary-600" />
            Dados do Condutor
          </h3>
          <div className="space-y-3">
            <div className="flex justify-between">
              <span className="text-gray-600">Nome:</span>
              <span className="font-semibold text-gray-900">{result.driverName}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-gray-600">Idade:</span>
              <span className="font-semibold text-gray-900">{result.driverAge} anos</span>
            </div>
            <div className="flex justify-between">
              <span className="text-gray-600">Experiência:</span>
              <span className="font-semibold text-gray-900">{result.drivingExperience} anos</span>
            </div>
            <div className="flex justify-between items-center">
              <span className="text-gray-600">Localização:</span>
              <span className="font-semibold text-gray-900 flex items-center gap-1">
                <MapPin className="w-4 h-4" />
                {result.driverLocation}
              </span>
            </div>
          </div>
        </div>

        <div className="bg-white rounded-xl shadow-lg p-6">
          <h3 className="text-xl font-semibold text-gray-800 mb-4 flex items-center gap-2">
            <Car className="w-6 h-6 text-primary-600" />
            Dados do Veículo
          </h3>
          <div className="space-y-3">
            <div className="flex justify-between">
              <span className="text-gray-600">Veículo:</span>
              <span className="font-semibold text-gray-900">{result.vehicleDescription}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-gray-600">Valor de mercado:</span>
              <span className="font-semibold text-gray-900">{formatCurrency(result.vehicleValue)}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-gray-600">Prêmio base (5%):</span>
              <span className="font-semibold text-gray-900">{formatCurrency(result.basePremium)}</span>
            </div>
          </div>
        </div>
      </div>

      {/* Gráfico de Fatores de Risco */}
      <div className="bg-white rounded-xl shadow-lg p-6">
        <h3 className="text-xl font-semibold text-gray-800 mb-6">
          Análise Detalhada de Fatores de Risco
        </h3>
        
        <div className="mb-6">
          <ResponsiveContainer width="100%" height={400}>
            <BarChart data={riskFactorsData} margin={{ top: 20, right: 30, left: 20, bottom: 80 }}>
              <XAxis 
                dataKey="name" 
                angle={-45} 
                textAnchor="end" 
                height={100}
                tick={{ fontSize: 12 }}
              />
              <YAxis 
                label={{ value: 'Multiplicador', angle: -90, position: 'insideLeft' }}
                domain={[0.8, 2.0]}
              />
              <Tooltip 
                formatter={(value: number) => value.toFixed(2) + 'x'}
                contentStyle={{ backgroundColor: '#fff', border: '1px solid #ccc', borderRadius: '8px' }}
              />
              <Bar dataKey="value" radius={[8, 8, 0, 0]}>
                {riskFactorsData.map((entry, index) => (
                  <Cell key={`cell-${index}`} fill={entry.color} />
                ))}
              </Bar>
            </BarChart>
          </ResponsiveContainer>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {riskFactorsData.map((factor, index) => (
            <div 
              key={index}
              className="flex items-center justify-between p-4 bg-gray-50 rounded-lg"
            >
              <span className="text-sm text-gray-700">{factor.name}</span>
              <span className={`font-bold ${
                factor.value > 1.0 ? 'text-red-600' :
                factor.value < 1.0 ? 'text-green-600' :
                'text-gray-600'
              }`}>
                {factor.value > 1.0 && '+'}
                {((factor.value - 1) * 100).toFixed(0)}%
              </span>
            </div>
          ))}
        </div>
      </div>

      {/* Gráfico de Distribuição do Prêmio */}
      <div className="bg-white rounded-xl shadow-lg p-6">
        <h3 className="text-xl font-semibold text-gray-800 mb-6">
          Composição do Prêmio
        </h3>
        
        <ResponsiveContainer width="100%" height={300}>
          <PieChart>
            <Pie
              data={premiumDistribution}
              cx="50%"
              cy="50%"
              labelLine={false}
              label={(entry) => `${entry.name}: ${formatCurrency(entry.value)}`}
              outerRadius={100}
              fill="#8884d8"
              dataKey="value"
            >
              {premiumDistribution.map((entry, index) => (
                <Cell key={`cell-${index}`} fill={entry.color} />
              ))}
            </Pie>
            <Tooltip formatter={(value: number) => formatCurrency(value)} />
            <Legend />
          </PieChart>
        </ResponsiveContainer>

        <div className="mt-6 p-4 bg-blue-50 rounded-lg">
          <p className="text-sm text-gray-700">
            <strong>Como calculamos:</strong> O prêmio base representa 5% do valor do veículo.
            Os fatores de risco multiplicam esse valor baseado em dados estatísticos de sinistralidade.
          </p>
        </div>
      </div>

      {/* Call to Action */}
      <div className="bg-gradient-to-r from-green-500 to-emerald-600 rounded-xl shadow-lg p-8 text-white text-center">
        <h3 className="text-2xl font-bold mb-2">Gostou da cotação?</h3>
        <p className="text-lg mb-6 opacity-90">
          Entre em contato conosco para contratar seu seguro agora mesmo!
        </p>
        <div className="flex flex-col sm:flex-row gap-4 justify-center">
          <button className="bg-white text-green-600 font-semibold px-8 py-3 rounded-lg hover:bg-gray-100 transition-colors">
            Contratar Agora
          </button>
          <button 
            onClick={() => window.print()}
            className="bg-green-700 text-white font-semibold px-8 py-3 rounded-lg hover:bg-green-800 transition-colors"
          >
            Imprimir Cotação
          </button>
        </div>
      </div>
    </div>
  );
}
