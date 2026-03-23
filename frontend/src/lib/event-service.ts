import api from "./api";
import { ApiResponse } from "@/types/auth";
import {
  Event,
  CreateEventRequest,
  UpdateEventRequest,
  EventFilterParams,
  PageResponse,
} from "@/types/event";

export const eventService = {
  async getEvents(params: EventFilterParams): Promise<PageResponse<Event>> {
    const res = await api.get<ApiResponse<PageResponse<Event>>>("/v1/events", {
      params,
    });
    return res.data.data;
  },

  async getEventById(id: number): Promise<Event> {
    const res = await api.get<ApiResponse<Event>>(`/v1/events/${id}`);
    return res.data.data;
  },

  async getEventForManage(id: number): Promise<Event> {
    const res = await api.get<ApiResponse<Event>>(`/v1/events/${id}/manage`);
    return res.data.data;
  },

  async getFeaturedEvents(): Promise<Event[]> {
    const res = await api.get<ApiResponse<Event[]>>("/v1/events/featured");
    return res.data.data;
  },

  async createEvent(data: CreateEventRequest): Promise<Event> {
    const res = await api.post<ApiResponse<Event>>("/v1/events", data);
    return res.data.data;
  },

  async updateEvent(id: number, data: UpdateEventRequest): Promise<Event> {
    const res = await api.put<ApiResponse<Event>>(`/v1/events/${id}`, data);
    return res.data.data;
  },

  async deleteEvent(id: number): Promise<void> {
    await api.delete(`/v1/events/${id}`);
  },

  async duplicateEvent(id: number): Promise<Event> {
    const res = await api.post<ApiResponse<Event>>(`/v1/events/${id}/duplicate`);
    return res.data.data;
  },

  async getMyEvents(
    page: number = 0,
    size: number = 10
  ): Promise<PageResponse<Event>> {
    const res = await api.get<ApiResponse<PageResponse<Event>>>(
      "/v1/events/my-events",
      { params: { page, size } }
    );
    return res.data.data;
  },
};
