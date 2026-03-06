import api from "./api";
import { ApiResponse } from "@/types/auth";
import {
  CartResponse,
  CartItemResponse,
  AddToCartRequest,
  UpdateCartItemRequest,
} from "@/types/cart";

export const cartService = {
  async getCart(): Promise<CartResponse> {
    const res = await api.get<ApiResponse<CartResponse>>("/v1/cart");
    return res.data.data;
  },

  async addToCart(data: AddToCartRequest): Promise<CartItemResponse> {
    const res = await api.post<ApiResponse<CartItemResponse>>(
      "/v1/cart/items",
      data
    );
    return res.data.data;
  },

  async updateCartItem(
    id: number,
    data: UpdateCartItemRequest
  ): Promise<CartItemResponse> {
    const res = await api.put<ApiResponse<CartItemResponse>>(
      `/v1/cart/items/${id}`,
      data
    );
    return res.data.data;
  },

  async removeCartItem(id: number): Promise<void> {
    await api.delete(`/v1/cart/items/${id}`);
  },

  async clearCart(): Promise<void> {
    await api.delete("/v1/cart");
  },
};
