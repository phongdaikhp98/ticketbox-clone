import api from "./api";
import { ApiResponse } from "@/types/auth";
import { PromoCodeRequest, PromoCodeResponse } from "@/types/promo";

export const promoService = {
  async getAll(): Promise<PromoCodeResponse[]> {
    const res = await api.get<ApiResponse<PromoCodeResponse[]>>("/v1/admin/promo-codes");
    return res.data.data;
  },

  async create(data: PromoCodeRequest): Promise<PromoCodeResponse> {
    const res = await api.post<ApiResponse<PromoCodeResponse>>("/v1/admin/promo-codes", data);
    return res.data.data;
  },

  async update(id: number, data: PromoCodeRequest): Promise<PromoCodeResponse> {
    const res = await api.put<ApiResponse<PromoCodeResponse>>(`/v1/admin/promo-codes/${id}`, data);
    return res.data.data;
  },

  async toggle(id: number): Promise<PromoCodeResponse> {
    const res = await api.patch<ApiResponse<PromoCodeResponse>>(
      `/v1/admin/promo-codes/${id}/toggle`
    );
    return res.data.data;
  },
};
