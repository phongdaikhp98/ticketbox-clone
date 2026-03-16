import api from "./api";
import { ApiResponse } from "@/types/auth";
import { OrderResponse, CheckoutRequest, PaymentUrlResponse } from "@/types/order";
import { PageResponse } from "@/types/event";

export const orderService = {
  async checkout(data: CheckoutRequest): Promise<OrderResponse> {
    const res = await api.post<ApiResponse<OrderResponse>>(
      "/v1/orders/checkout",
      data
    );
    return res.data.data;
  },

  async createPaymentUrl(orderId: number): Promise<PaymentUrlResponse> {
    const res = await api.post<ApiResponse<PaymentUrlResponse>>(
      `/v1/payment/vnpay/create/${orderId}`
    );
    return res.data.data;
  },

  async getMyOrders(
    page: number = 0,
    size: number = 10
  ): Promise<PageResponse<OrderResponse>> {
    const res = await api.get<ApiResponse<PageResponse<OrderResponse>>>(
      "/v1/orders",
      { params: { page, size } }
    );
    return res.data.data;
  },

  async getOrderDetail(id: number): Promise<OrderResponse> {
    const res = await api.get<ApiResponse<OrderResponse>>(`/v1/orders/${id}`);
    return res.data.data;
  },

  async cancelOrder(id: number): Promise<void> {
    await api.delete(`/v1/orders/${id}/cancel`);
  },

  async verifyVnPayReturn(params: Record<string, string>): Promise<{ RspCode: string; Message: string }> {
    const res = await api.post<ApiResponse<{ RspCode: string; Message: string }>>(
      "/v1/payment/vnpay/verify-return",
      params
    );
    return res.data.data;
  },
};
