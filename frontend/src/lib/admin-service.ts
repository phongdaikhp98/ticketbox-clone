import api from "./api";
import { PageResponse } from "@/types/event";
import {
  AdminOverview,
  AdminUser,
  AdminOrder,
  AdminEvent,
} from "@/types/admin";

interface ApiResponse<T> {
  code: string;
  message: string;
  requestId?: string;
  timestamp: string;
  data: T;
}

export const adminService = {
  // Dashboard
  async getOverview(): Promise<AdminOverview> {
    const res = await api.get<ApiResponse<AdminOverview>>(
      "/v1/admin/dashboard/overview"
    );
    return res.data.data;
  },

  // Users
  async getUsers(params: {
    page?: number;
    size?: number;
    role?: string;
    isActive?: boolean;
    search?: string;
  }): Promise<PageResponse<AdminUser>> {
    const res = await api.get<ApiResponse<PageResponse<AdminUser>>>(
      "/v1/admin/users",
      { params }
    );
    return res.data.data;
  },

  async changeRole(userId: number, role: string): Promise<AdminUser> {
    const res = await api.patch<ApiResponse<AdminUser>>(
      `/v1/admin/users/${userId}/role`,
      { role }
    );
    return res.data.data;
  },

  async toggleActive(userId: number): Promise<AdminUser> {
    const res = await api.patch<ApiResponse<AdminUser>>(
      `/v1/admin/users/${userId}/toggle-active`
    );
    return res.data.data;
  },

  // Orders
  async getOrders(params: {
    page?: number;
    size?: number;
    status?: string;
    search?: string;
  }): Promise<PageResponse<AdminOrder>> {
    const res = await api.get<ApiResponse<PageResponse<AdminOrder>>>(
      "/v1/admin/orders",
      { params }
    );
    return res.data.data;
  },

  async getOrderDetail(orderId: number): Promise<AdminOrder> {
    const res = await api.get<ApiResponse<AdminOrder>>(
      `/v1/admin/orders/${orderId}`
    );
    return res.data.data;
  },

  // Events
  async getEvents(params: {
    page?: number;
    size?: number;
    status?: string;
    category?: string;
    search?: string;
  }): Promise<PageResponse<AdminEvent>> {
    const res = await api.get<ApiResponse<PageResponse<AdminEvent>>>(
      "/v1/admin/events",
      { params }
    );
    return res.data.data;
  },

  async toggleFeatured(eventId: number): Promise<AdminEvent> {
    const res = await api.patch<ApiResponse<AdminEvent>>(
      `/v1/admin/events/${eventId}/toggle-featured`
    );
    return res.data.data;
  },

  async changeEventStatus(eventId: number, status: string): Promise<AdminEvent> {
    const res = await api.patch<ApiResponse<AdminEvent>>(
      `/v1/admin/events/${eventId}/status`,
      null,
      { params: { status } }
    );
    return res.data.data;
  },
};
