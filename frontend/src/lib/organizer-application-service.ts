import api from "./api";
import { PageResponse } from "@/types/event";

interface ApiResponse<T> {
  code: string;
  message: string;
  requestId?: string;
  timestamp: string;
  data: T;
}

export interface OrganizerApplicationRequest {
  orgName: string;
  taxNumber: string;
  contactPhone: string;
  reason?: string;
}

export interface OrganizerApplicationResponse {
  id: number;
  userId: number;
  userFullName: string;
  userEmail: string;
  orgName: string;
  taxNumber: string;
  contactPhone: string;
  reason?: string;
  status: "PENDING" | "APPROVED" | "REJECTED";
  reviewedByName?: string;
  reviewNote?: string;
  submittedAt: string;
  reviewedAt?: string;
  createdDate: string;
}

export interface ReviewRequest {
  status: "APPROVED" | "REJECTED";
  reviewNote?: string;
}

export const organizerApplicationService = {
  async submit(
    data: OrganizerApplicationRequest
  ): Promise<OrganizerApplicationResponse> {
    const res = await api.post<ApiResponse<OrganizerApplicationResponse>>(
      "/v1/organizer-applications",
      data
    );
    return res.data.data;
  },

  async getMyApplication(): Promise<OrganizerApplicationResponse | null> {
    try {
      const res = await api.get<ApiResponse<OrganizerApplicationResponse>>(
        "/v1/organizer-applications/me"
      );
      return res.data.data;
    } catch (err: unknown) {
      const axiosErr = err as { response?: { status?: number } };
      if (axiosErr?.response?.status === 404) {
        return null;
      }
      throw err;
    }
  },

  async getApplications(
    status?: string,
    page = 0,
    size = 10
  ): Promise<PageResponse<OrganizerApplicationResponse>> {
    const params: Record<string, unknown> = { page, size };
    if (status) params.status = status;
    const res = await api.get<
      ApiResponse<PageResponse<OrganizerApplicationResponse>>
    >("/v1/admin/organizer-applications", { params });
    return res.data.data;
  },

  async reviewApplication(
    id: number,
    data: ReviewRequest
  ): Promise<OrganizerApplicationResponse> {
    const res = await api.patch<ApiResponse<OrganizerApplicationResponse>>(
      `/v1/admin/organizer-applications/${id}/review`,
      data
    );
    return res.data.data;
  },
};
