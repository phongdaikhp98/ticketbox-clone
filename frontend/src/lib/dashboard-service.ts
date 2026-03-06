import api from "./api";
import { ApiResponse } from "@/types/auth";
import { PageResponse } from "@/types/event";
import {
  DashboardOverviewResponse,
  EventStatsResponse,
  AttendeeResponse,
} from "@/types/dashboard";

export const dashboardService = {
  async getOverview(): Promise<DashboardOverviewResponse> {
    const res = await api.get<ApiResponse<DashboardOverviewResponse>>(
      "/v1/organizer/dashboard/overview"
    );
    return res.data.data;
  },

  async getEventStats(eventId: number): Promise<EventStatsResponse> {
    const res = await api.get<ApiResponse<EventStatsResponse>>(
      `/v1/organizer/dashboard/events/${eventId}`
    );
    return res.data.data;
  },

  async getAttendees(
    eventId: number,
    params: { page?: number; size?: number; status?: string; search?: string }
  ): Promise<PageResponse<AttendeeResponse>> {
    const res = await api.get<ApiResponse<PageResponse<AttendeeResponse>>>(
      `/v1/organizer/dashboard/events/${eventId}/attendees`,
      { params }
    );
    return res.data.data;
  },
};
