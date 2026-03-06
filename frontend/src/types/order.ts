export interface OrderResponse {
  id: number;
  status: string;
  totalAmount: number;
  paymentMethod?: string;
  paymentStatus: string;
  vnpayTxnRef?: string;
  orderItems: OrderItemResponse[];
  createdDate: string;
  updatedDate: string;
}

export interface PaymentUrlResponse {
  orderId: number;
  paymentUrl: string;
}

export interface OrderItemResponse {
  id: number;
  quantity: number;
  unitPrice: number;
  ticketTypeName: string;
  event: OrderEventSummary;
}

export interface OrderEventSummary {
  id: number;
  title: string;
  imageUrl?: string;
  eventDate: string;
  location: string;
}

export interface CheckoutRequest {
  paymentMethod: string;
}

export const ORDER_STATUSES: Record<string, { label: string; color: string }> = {
  PENDING: { label: "Chờ thanh toán", color: "bg-yellow-100 text-yellow-800" },
  COMPLETED: { label: "Hoàn thành", color: "bg-green-100 text-green-800" },
  CANCELLED: { label: "Đã hủy", color: "bg-red-100 text-red-800" },
};

export const PAYMENT_METHODS: Record<string, string> = {
  E_WALLET: "Ví điện tử (VNPay)",
};
