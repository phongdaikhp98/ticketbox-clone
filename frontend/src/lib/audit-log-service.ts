import api from "./api";
import { AuditLog } from "@/types/audit-log";
import { PageResponse } from "@/types/event";

interface ApiResponse<T> {
  code: string;
  message: string;
  requestId?: string;
  timestamp: string;
  data: T;
}

const auditLogService = {
  getLogs: async (params?: {
    entityType?: string;
    action?: string;
    page?: number;
    size?: number;
  }): Promise<PageResponse<AuditLog>> => {
    const res = await api.get<ApiResponse<PageResponse<AuditLog>>>(
      "/v1/admin/audit-logs",
      { params }
    );
    return res.data.data;
  },
};

export default auditLogService;
