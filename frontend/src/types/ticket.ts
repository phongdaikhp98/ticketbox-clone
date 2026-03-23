export interface TicketResponse {
  id: number;
  ticketCode: string;
  status: string;
  ticketTypeName: string;
  eventTitle: string;
  eventDate: string;
  eventLocation: string;
  eventImageUrl?: string;
  usedAt?: string;
  createdDate: string;
}

export interface CheckInRequest {
  ticketCode: string;
}

export interface CheckInResponse {
  ticketCode: string;
  status: string;
  message: string;
  eventTitle: string;
  attendeeName: string;
  ticketTypeName: string;
}

export interface TicketTransferResponse {
  id: number;
  ticketId: number;
  ticketCode: string;
  eventTitle: string;
  eventDate: string;
  fromUserName: string;
  fromUserEmail: string;
  toEmail: string;
  toUserName?: string;
  transferToken: string;
  status: string;
  expiresAt: string;
  completedAt?: string;
  createdDate: string;
}

export const TICKET_STATUSES: Record<string, { label: string; color: string }> = {
  ISSUED: { label: "Chưa sử dụng", color: "bg-blue-100 text-blue-800" },
  USED: { label: "Đã sử dụng", color: "bg-green-100 text-green-800" },
  CANCELLED: { label: "Đã hủy", color: "bg-red-100 text-red-800" },
};
