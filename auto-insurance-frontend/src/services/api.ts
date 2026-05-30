import axios from 'axios';
import type { CalculationRequest, CalculationResponse } from '../types/api';

const api = axios.create({
  baseURL: '/api/v1',
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 30000, // 30 segundos
});

// Interceptor para logs (desenvolvimento)
api.interceptors.request.use(
  (config) => {
    console.log('🚀 API Request:', config.method?.toUpperCase(), config.url);
    return config;
  },
  (error) => {
    console.error('❌ Request Error:', error);
    return Promise.reject(error);
  }
);

api.interceptors.response.use(
  (response) => {
    console.log('✅ API Response:', response.status, response.data);
    return response;
  },
  (error) => {
    console.error('❌ Response Error:', error.response?.data || error.message);
    return Promise.reject(error);
  }
);

export const insuranceAPI = {
  /**
   * Calcula prêmio de seguro
   */
  calculatePremium: async (data: CalculationRequest): Promise<CalculationResponse> => {
    const response = await api.post<CalculationResponse>('/insurance/calculate', data);
    return response.data;
  },

  /**
   * Health check da API
   */
  healthCheck: async (): Promise<string> => {
    const response = await api.get<string>('/insurance/health');
    return response.data;
  },
};

export default api;
