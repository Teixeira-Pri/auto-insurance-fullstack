// Types baseados nos DTOs da API Backend
export interface CalculationRequest {
  // Driver data
  driverName: string;
  driverCpf: string;
  driverBirthDate: string;
  driverGender: Gender;
  driverZipCode: string;
  driverLicenseIssueDate: string;
  driverEmail?: string;
  driverPhone?: string;
  driverClaimsHistory: number;

  // Vehicle data
  vehicleBrand: string;
  vehicleModel: string;
  vehicleManufacturingYear: number;
  vehicleModelYear: number;
  vehicleFuelType: FuelType;
  vehicleCategory: VehicleCategory;
  vehicleCondition: VehicleCondition;
  vehicleMarketValue: number;
  vehicleLicensePlate?: string;
}

export interface CalculationResponse {
  quoteId: string;
  calculatedAt: string;
  basePremium: number;
  totalPremium: number;
  monthlyPremium: number;
  driverName: string;
  driverAge: number;
  drivingExperience: number;
  driverLocation: string;
  vehicleDescription: string;
  vehicleValue: number;
  riskFactors: Record<string, number>;
  totalRiskMultiplier: number;
  riskProfile: RiskProfile;
  message: string;
}

export type Gender = 'MALE' | 'FEMALE' | 'OTHER' | 'PREFER_NOT_TO_SAY';

export type FuelType = 
  | 'GASOLINE' 
  | 'ETHANOL' 
  | 'FLEX' 
  | 'DIESEL' 
  | 'ELECTRIC' 
  | 'HYBRID' 
  | 'PLUGIN_HYBRID' 
  | 'CNG';

export type VehicleCategory = 
  | 'COMPACT' 
  | 'SEDAN' 
  | 'SUV' 
  | 'PICKUP' 
  | 'LUXURY' 
  | 'SPORTS' 
  | 'VAN' 
  | 'MOTORCYCLE' 
  | 'TRUCK';

export type VehicleCondition = 
  | 'NEW' 
  | 'EXCELLENT' 
  | 'GOOD' 
  | 'FAIR' 
  | 'POOR';

export type RiskProfile = 'BAIXO' | 'MÉDIO' | 'ALTO';

export interface ValidationError {
  field: string;
  message: string;
  rejectedValue: any;
}

export interface ApiError {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
  fieldErrors?: ValidationError[];
}
