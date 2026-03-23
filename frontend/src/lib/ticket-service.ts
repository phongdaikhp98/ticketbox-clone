import api from "./api";
import { ApiResponse } from "@/types/auth";
import { TicketResponse, CheckInRequest, CheckInResponse, TicketTransferResponse } from "@/types/ticket";
import { PageResponse } from "@/types/event";

export const ticketService = {
  async getMyTickets(
    page: number = 0,
    size: number = 10,
    eventId?: number,
    status?: string
  ): Promise<PageResponse<TicketResponse>> {
    const params: Record<string, unknown> = { page, size };
    if (eventId) params.eventId = eventId;
    if (status) params.status = status;

    const res = await api.get<ApiResponse<PageResponse<TicketResponse>>>(
      "/v1/tickets",
      { params }
    );
    return res.data.data;
  },

  async getTicketDetail(id: number): Promise<TicketResponse> {
    const res = await api.get<ApiResponse<TicketResponse>>(`/v1/tickets/${id}`);
    return res.data.data;
  },

  async getQrBlob(id: number): Promise<Blob> {
    const res = await api.get(`/v1/tickets/${id}/qr`, {
      responseType: "blob",
    });
    return new Blob([res.data], { type: "image/png" });
  },

  async downloadPdf(id: number): Promise<void> {
    const res = await api.get(`/v1/tickets/${id}/pdf`, {
      responseType: "blob",
    });
    const url = window.URL.createObjectURL(new Blob([res.data]));
    const link = document.createElement("a");
    link.href = url;
    link.setAttribute("download", `ticket-${id}.pdf`);
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.URL.revokeObjectURL(url);
  },

  async checkIn(data: CheckInRequest): Promise<CheckInResponse> {
    const res = await api.post<ApiResponse<CheckInResponse>>(
      "/v1/tickets/check-in",
      data
    );
    return res.data.data;
  },

  async initiateTransfer(ticketId: number, toEmail: string): Promise<TicketTransferResponse> {
    const res = await api.post<ApiResponse<TicketTransferResponse>>(
      `/v1/tickets/${ticketId}/transfer`,
      { toEmail }
    );
    return res.data.data;
  },

  async getTransferByToken(token: string): Promise<TicketTransferResponse> {
    const res = await api.get<ApiResponse<TicketTransferResponse>>(
      `/v1/ticket-transfers/${token}`
    );
    return res.data.data;
  },

  async acceptTransfer(token: string): Promise<TicketTransferResponse> {
    const res = await api.post<ApiResponse<TicketTransferResponse>>(
      `/v1/ticket-transfers/${token}/accept`
    );
    return res.data.data;
  },

  async cancelTransfer(transferId: number): Promise<void> {
    await api.delete(`/v1/ticket-transfers/${transferId}`);
  },

  async getMyTransfers(): Promise<TicketTransferResponse[]> {
    const res = await api.get<ApiResponse<TicketTransferResponse[]>>(
      "/v1/ticket-transfers/my"
    );
    return res.data.data;
  },
};
