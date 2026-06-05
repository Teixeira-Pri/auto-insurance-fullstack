import { useState } from 'react';
import type { InputHTMLAttributes } from 'react';
import { useForm } from 'react-hook-form';
import { useMutation } from '@tanstack/react-query';
import type { AxiosError } from 'axios';
import { Calculator, Loader2, AlertCircle } from 'lucide-react';
import { insuranceAPI } from '../services/api';
import { validateCPF, onlyNumbers, calculateAge } from '../utils/formatters';
import type { CalculationRequest, CalculationResponse, ApiError } from '../types/api';
import ResultsDisplay from './ResultsDisplay';
import InputMask from 'react-input-mask';
import { toast } from 'sonner';

export default function InsuranceForm() {
  const [result, setResult] = useState<CalculationResponse | null>(null);
  
  const { register, handleSubmit, formState: { errors }, watch } = useForm<CalculationRequest>({
    defaultValues: {
      driverClaimsHistory: 0,
    }
  });

  const mutation = useMutation<CalculationResponse, AxiosError<ApiError>, CalculationRequest>({
    mutationFn: insuranceAPI.calculatePremium,
    onSuccess: (data) => {
      setResult(data);
      toast.success('Cotação calculada com sucesso!');
      window.scrollTo({ top: document.body.scrollHeight, behavior: 'smooth' });
    },
    onError: (error) => {
      const apiError = error.response?.data;
      if (apiError?.fieldErrors) {
        apiError.fieldErrors.forEach((err) => {
          toast.error(`${err.field}: ${err.message}`);
        });
      } else {
        toast.error(apiError?.message || 'Erro ao calcular seguro');
      }
    },
  });

  const onSubmit = (data: CalculationRequest) => {
    // Remove máscaras
    const cleanData = {
      ...data,
      driverCpf: onlyNumbers(data.driverCpf),
      driverZipCode: onlyNumbers(data.driverZipCode),
      driverPhone: data.driverPhone ? onlyNumbers(data.driverPhone) : undefined,
    };
    
    mutation.mutate(cleanData);
  };

  const birthDate = watch('driverBirthDate');
  const age = birthDate ? calculateAge(birthDate) : null;

  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-50 to-indigo-100 py-12 px-4">
      <div className="max-w-4xl mx-auto">
        {/* Header */}
        <div className="text-center mb-12">
          <h1 className="text-4xl font-bold text-gray-900 mb-2">
            Calculadora de Seguro Auto
          </h1>
          <p className="text-gray-600">
            Calcule seu prêmio em segundos com análise completa de risco
          </p>
        </div>

        {/* Form Card */}
        <div className="bg-white rounded-2xl shadow-xl p-8">
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-8">
            
            {/* Dados do Condutor */}
            <section>
              <h2 className="text-2xl font-semibold text-gray-800 mb-6 flex items-center gap-2">
                <div className="w-8 h-8 bg-primary-500 text-white rounded-full flex items-center justify-center text-sm">
                  1
                </div>
                Dados do Condutor
              </h2>
              
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                {/* Nome */}
                <div className="md:col-span-2">
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Nome Completo *
                  </label>
                  <input
                    {...register('driverName', {
                      required: 'Nome é obrigatório',
                      minLength: { value: 3, message: 'Mínimo 3 caracteres' }
                    })}
                    className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent"
                    placeholder="João da Silva"
                  />
                  {errors.driverName && (
                    <p className="mt-1 text-sm text-red-600">{errors.driverName.message}</p>
                  )}
                </div>

                {/* CPF */}
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    CPF *
                  </label>
                  <InputMask
                    mask="999.999.999-99"
                    {...register('driverCpf', {
                      required: 'CPF é obrigatório',
                      validate: (value) => validateCPF(value) || 'CPF inválido'
                    })}
                  >
                    {(inputProps: InputHTMLAttributes<HTMLInputElement>) => (
                      <input
                        {...inputProps}
                        className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent"
                        placeholder="000.000.000-00"
                      />
                    )}
                  </InputMask>
                  {errors.driverCpf && (
                    <p className="mt-1 text-sm text-red-600">{errors.driverCpf.message}</p>
                  )}
                </div>

                {/* Data de Nascimento */}
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Data de Nascimento *
                    {age !== null && (
                      <span className="ml-2 text-primary-600 font-semibold">
                        ({age} anos)
                      </span>
                    )}
                  </label>
                  <input
                    type="date"
                    {...register('driverBirthDate', {
                      required: 'Data de nascimento é obrigatória',
                      validate: (value) => {
                        const age = calculateAge(value);
                        return age >= 18 || 'Idade mínima: 18 anos';
                      }
                    })}
                    className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent"
                  />
                  {errors.driverBirthDate && (
                    <p className="mt-1 text-sm text-red-600">{errors.driverBirthDate.message}</p>
                  )}
                </div>

                {/* Gênero */}
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Gênero *
                  </label>
                  <select
                    {...register('driverGender', { required: 'Gênero é obrigatório' })}
                    className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent"
                  >
                    <option value="">Selecione</option>
                    <option value="MALE">Masculino</option>
                    <option value="FEMALE">Feminino</option>
                    <option value="OTHER">Outro</option>
                    <option value="PREFER_NOT_TO_SAY">Prefiro não informar</option>
                  </select>
                  {errors.driverGender && (
                    <p className="mt-1 text-sm text-red-600">{errors.driverGender.message}</p>
                  )}
                </div>

                {/* CEP */}
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    CEP *
                  </label>
                  <InputMask
                    mask="99999-999"
                    {...register('driverZipCode', {
                      required: 'CEP é obrigatório',
                      pattern: {
                        value: /^\d{5}-?\d{3}$/,
                        message: 'CEP inválido'
                      }
                    })}
                  >
                    {(inputProps: InputHTMLAttributes<HTMLInputElement>) => (
                      <input
                        {...inputProps}
                        className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent"
                        placeholder="00000-000"
                      />
                    )}
                  </InputMask>
                  {errors.driverZipCode && (
                    <p className="mt-1 text-sm text-red-600">{errors.driverZipCode.message}</p>
                  )}
                </div>

                {/* Data da CNH */}
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Data da 1ª Habilitação *
                  </label>
                  <input
                    type="date"
                    {...register('driverLicenseIssueDate', {
                      required: 'Data da CNH é obrigatória'
                    })}
                    className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent"
                  />
                  {errors.driverLicenseIssueDate && (
                    <p className="mt-1 text-sm text-red-600">{errors.driverLicenseIssueDate.message}</p>
                  )}
                </div>

                {/* Email */}
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Email
                  </label>
                  <input
                    type="email"
                    {...register('driverEmail', {
                      pattern: {
                        value: /^[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}$/i,
                        message: 'Email inválido'
                      }
                    })}
                    className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent"
                    placeholder="seu@email.com"
                  />
                  {errors.driverEmail && (
                    <p className="mt-1 text-sm text-red-600">{errors.driverEmail.message}</p>
                  )}
                </div>

                {/* Telefone */}
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Telefone
                  </label>
                  <InputMask
                    mask="(99) 99999-9999"
                    {...register('driverPhone')}
                  >
                    {(inputProps: InputHTMLAttributes<HTMLInputElement>) => (
                      <input
                        {...inputProps}
                        className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent"
                        placeholder="(00) 00000-0000"
                      />
                    )}
                  </InputMask>
                </div>

                {/* Sinistros */}
                <div className="md:col-span-2">
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Sinistros nos últimos 5 anos *
                  </label>
                  <input
                    type="number"
                    {...register('driverClaimsHistory', {
                      required: true,
                      min: { value: 0, message: 'Mínimo: 0' },
                      max: { value: 20, message: 'Máximo: 20' },
                      valueAsNumber: true
                    })}
                    className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent"
                    min="0"
                    max="20"
                  />
                  {errors.driverClaimsHistory && (
                    <p className="mt-1 text-sm text-red-600">{errors.driverClaimsHistory.message}</p>
                  )}
                </div>
              </div>
            </section>

            {/* Dados do Veículo */}
            <section className="pt-8 border-t">
              <h2 className="text-2xl font-semibold text-gray-800 mb-6 flex items-center gap-2">
                <div className="w-8 h-8 bg-primary-500 text-white rounded-full flex items-center justify-center text-sm">
                  2
                </div>
                Dados do Veículo
              </h2>
              
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                {/* Marca */}
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Marca *
                  </label>
                  <input
                    {...register('vehicleBrand', {
                      required: 'Marca é obrigatória',
                      minLength: { value: 2, message: 'Mínimo 2 caracteres' }
                    })}
                    className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent"
                    placeholder="Toyota"
                  />
                  {errors.vehicleBrand && (
                    <p className="mt-1 text-sm text-red-600">{errors.vehicleBrand.message}</p>
                  )}
                </div>

                {/* Modelo */}
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Modelo *
                  </label>
                  <input
                    {...register('vehicleModel', {
                      required: 'Modelo é obrigatório',
                      minLength: { value: 2, message: 'Mínimo 2 caracteres' }
                    })}
                    className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent"
                    placeholder="Corolla"
                  />
                  {errors.vehicleModel && (
                    <p className="mt-1 text-sm text-red-600">{errors.vehicleModel.message}</p>
                  )}
                </div>

                {/* Ano Fabricação */}
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Ano de Fabricação *
                  </label>
                  <input
                    type="number"
                    {...register('vehicleManufacturingYear', {
                      required: 'Ano de fabricação é obrigatório',
                      min: { value: 1980, message: 'Mínimo: 1980' },
                      max: { value: 2026, message: 'Máximo: 2026' },
                      valueAsNumber: true
                    })}
                    className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent"
                    placeholder="2020"
                  />
                  {errors.vehicleManufacturingYear && (
                    <p className="mt-1 text-sm text-red-600">{errors.vehicleManufacturingYear.message}</p>
                  )}
                </div>

                {/* Ano Modelo */}
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Ano do Modelo *
                  </label>
                  <input
                    type="number"
                    {...register('vehicleModelYear', {
                      required: 'Ano do modelo é obrigatório',
                      min: { value: 1950, message: 'Mínimo: 1950' },
                      max: { value: 2027, message: 'Máximo: 2027' },
                      valueAsNumber: true
                    })}
                    className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent"
                    placeholder="2021"
                  />
                  {errors.vehicleModelYear && (
                    <p className="mt-1 text-sm text-red-600">{errors.vehicleModelYear.message}</p>
                  )}
                </div>

                {/* Combustível */}
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Combustível *
                  </label>
                  <select
                    {...register('vehicleFuelType', { required: 'Combustível é obrigatório' })}
                    className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent"
                  >
                    <option value="">Selecione</option>
                    <option value="FLEX">Flex (Gasolina/Etanol)</option>
                    <option value="GASOLINE">Gasolina</option>
                    <option value="ETHANOL">Etanol</option>
                    <option value="DIESEL">Diesel</option>
                    <option value="ELECTRIC">Elétrico</option>
                    <option value="HYBRID">Híbrido</option>
                  </select>
                  {errors.vehicleFuelType && (
                    <p className="mt-1 text-sm text-red-600">{errors.vehicleFuelType.message}</p>
                  )}
                </div>

                {/* Categoria */}
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Categoria *
                  </label>
                  <select
                    {...register('vehicleCategory', { required: 'Categoria é obrigatória' })}
                    className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent"
                  >
                    <option value="">Selecione</option>
                    <option value="COMPACT">Compacto/Hatch</option>
                    <option value="SEDAN">Sedan</option>
                    <option value="SUV">SUV/Crossover</option>
                    <option value="PICKUP">Picape</option>
                    <option value="MINIVAN">Van/Minivan</option>
                    <option value="SPORT">Esportivo</option>
                    <option value="MOTORCYCLE">Motocicleta</option>
                    <option value="TRUCK">Caminhão</option>
                  </select>
                  {errors.vehicleCategory && (
                    <p className="mt-1 text-sm text-red-600">{errors.vehicleCategory.message}</p>
                  )}
                </div>

                {/* Condição */}
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Condição *
                  </label>
                  <select
                    {...register('vehicleCondition', { required: 'Condição é obrigatória' })}
                    className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent"
                  >
                    <option value="">Selecione</option>
                    <option value="NEW">0 km / Novo</option>
                    <option value="GOOD">Bom</option>
                    <option value="FAIR">Regular</option>
                    <option value="POOR">Precário</option>
                  </select>
                  {errors.vehicleCondition && (
                    <p className="mt-1 text-sm text-red-600">{errors.vehicleCondition.message}</p>
                  )}
                </div>

                {/* Valor de Mercado */}
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Valor de Mercado (R$) *
                  </label>
                  <input
                    type="number"
                    step="0.01"
                    {...register('vehicleMarketValue', {
                      required: 'Valor de mercado é obrigatório',
                      min: { value: 1000, message: 'Mínimo: R$ 1.000,00' },
                      max: { value: 10000000, message: 'Máximo: R$ 10.000.000,00' },
                      valueAsNumber: true
                    })}
                    className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent"
                    placeholder="85000.00"
                  />
                  {errors.vehicleMarketValue && (
                    <p className="mt-1 text-sm text-red-600">{errors.vehicleMarketValue.message}</p>
                  )}
                </div>

                {/* Placa (opcional) */}
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Placa (opcional)
                  </label>
                  <InputMask
                    mask="aaa9*99"
                    formatChars={{
                      '9': '[0-9]',
                      'a': '[A-Za-z]',
                      '*': '[A-Za-z0-9]'
                    }}
                    {...register('vehicleLicensePlate')}
                  >
                    {(inputProps: InputHTMLAttributes<HTMLInputElement>) => (
                      <input
                        {...inputProps}
                        className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent uppercase"
                        placeholder="ABC1D23"
                      />
                    )}
                  </InputMask>
                </div>
              </div>
            </section>

            {/* Submit Button */}
            <div className="pt-6 border-t">
              <button
                type="submit"
                disabled={mutation.isPending}
                className="w-full bg-primary-600 hover:bg-primary-700 text-white font-semibold py-4 px-6 rounded-lg flex items-center justify-center gap-2 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {mutation.isPending ? (
                  <>
                    <Loader2 className="w-5 h-5 animate-spin" />
                    Calculando...
                  </>
                ) : (
                  <>
                    <Calculator className="w-5 h-5" />
                    Calcular Prêmio
                  </>
                )}
              </button>
            </div>

            {mutation.isError && (
              <div className="mt-4 p-4 bg-red-50 border border-red-200 rounded-lg flex items-start gap-3">
                <AlertCircle className="w-5 h-5 text-red-600 flex-shrink-0 mt-0.5" />
                <div>
                  <h3 className="font-semibold text-red-900">Erro ao calcular seguro</h3>
                  <p className="text-sm text-red-700 mt-1">
                    Verifique os dados e tente novamente. Se o erro persistir, entre em contato.
                  </p>
                </div>
              </div>
            )}
          </form>
        </div>

        {/* Results */}
        {result && (
          <div className="mt-8">
            <ResultsDisplay result={result} />
          </div>
        )}
      </div>
    </div>
  );
}
