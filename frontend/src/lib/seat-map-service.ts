import api from "./api";
import { ApiResponse } from "@/types/auth";
import { SeatMapResponse, CreateSeatMapRequest, SeatResponse } from "@/types/seat-map";

export const seatMapService = {
  async getSeatMapByEvent(eventId: number): Promise<SeatMapResponse> {
    const res = await api.get<ApiResponse<SeatMapResponse>>(
      `/v1/seat-maps/events/${eventId}`
    );
    return res.data.data;
  },

  async createSeatMap(data: CreateSeatMapRequest): Promise<SeatMapResponse> {
    const res = await api.post<ApiResponse<SeatMapResponse>>(
      "/v1/seat-maps",
      data
    );
    return res.data.data;
  },

  async updateSeatStatus(
    seatMapId: number,
    seatId: number,
    status: "AVAILABLE" | "BLOCKED"
  ): Promise<void> {
    await api.patch(`/v1/seat-maps/${seatMapId}/seats/${seatId}`, { status });
  },

  async deleteSeatMap(seatMapId: number): Promise<void> {
    await api.delete(`/v1/seat-maps/${seatMapId}`);
  },

  async reserveSeat(seatId: number): Promise<SeatResponse> {
    const res = await api.post<ApiResponse<SeatResponse>>(
      `/v1/seat-reservations/${seatId}/reserve`
    );
    return res.data.data;
  },

  async releaseSeat(seatId: number): Promise<void> {
    await api.delete(`/v1/seat-reservations/${seatId}/release`);
  },

  async addSeatToCart(seatId: number): Promise<void> {
    await api.post("/v1/cart/seat-items", { seatId });
  },
};
