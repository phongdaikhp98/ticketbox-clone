import api from "./api";
import { ApiResponse } from "@/types/auth";
import { Review, ReviewRequest } from "@/types/review";
import { PageResponse } from "@/types/event";

const reviewService = {
  async getEventReviews(
    eventId: number,
    page = 0,
    size = 5
  ): Promise<PageResponse<Review>> {
    const res = await api.get<ApiResponse<PageResponse<Review>>>(
      `/v1/events/${eventId}/reviews`,
      { params: { page, size } }
    );
    return res.data.data;
  },

  async createReview(
    eventId: number,
    data: ReviewRequest
  ): Promise<Review> {
    const res = await api.post<ApiResponse<Review>>(
      `/v1/events/${eventId}/reviews`,
      data
    );
    return res.data.data;
  },

  async updateReview(
    eventId: number,
    reviewId: number,
    data: ReviewRequest
  ): Promise<Review> {
    const res = await api.put<ApiResponse<Review>>(
      `/v1/events/${eventId}/reviews/${reviewId}`,
      data
    );
    return res.data.data;
  },

  async deleteReview(eventId: number, reviewId: number): Promise<void> {
    await api.delete(`/v1/events/${eventId}/reviews/${reviewId}`);
  },

  async getMyReview(eventId: number): Promise<Review | null> {
    try {
      const res = await api.get<ApiResponse<Review>>(
        `/v1/events/${eventId}/reviews/my`
      );
      return res.data.data;
    } catch {
      return null;
    }
  },

  async canReview(eventId: number): Promise<boolean> {
    try {
      const res = await api.get<ApiResponse<{ canReview: boolean }>>(
        `/v1/events/${eventId}/reviews/can-review`
      );
      return res.data.data.canReview;
    } catch {
      return false;
    }
  },
};

export default reviewService;
