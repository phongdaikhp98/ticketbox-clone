import api from "./api";
import { ApiResponse } from "@/types/auth";
import { WishlistResponse, WishlistCheckResponse } from "@/types/wishlist";
import { PageResponse } from "@/types/event";

export const wishlistService = {
  async getMyWishlist(
    page: number = 0,
    size: number = 10
  ): Promise<PageResponse<WishlistResponse>> {
    const res = await api.get<ApiResponse<PageResponse<WishlistResponse>>>(
      "/v1/wishlists",
      { params: { page, size } }
    );
    return res.data.data;
  },

  async addToWishlist(eventId: number): Promise<WishlistResponse> {
    const res = await api.post<ApiResponse<WishlistResponse>>(
      `/v1/wishlists/${eventId}`
    );
    return res.data.data;
  },

  async removeFromWishlist(eventId: number): Promise<void> {
    await api.delete(`/v1/wishlists/${eventId}`);
  },

  async checkWishlist(eventId: number): Promise<WishlistCheckResponse> {
    const res = await api.get<ApiResponse<WishlistCheckResponse>>(
      `/v1/wishlists/${eventId}/check`
    );
    return res.data.data;
  },
};
