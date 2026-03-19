export interface PromoCodeRequest {
  code: string;
  discountType: "PERCENTAGE" | "FLAT";
  discountValue: number;
  minOrderAmount?: number;
  usageLimit?: number;
  startDate?: string;
  endDate?: string;
  active: boolean;
}

export interface PromoCodeResponse {
  id: number;
  code: string;
  discountType: "PERCENTAGE" | "FLAT";
  discountValue: number;
  minOrderAmount?: number;
  usageLimit?: number;
  usedCount: number;
  startDate?: string;
  endDate?: string;
  active: boolean;
  createdDate: string;
  updatedDate: string;
}
