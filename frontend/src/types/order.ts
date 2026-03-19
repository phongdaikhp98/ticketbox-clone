export interface OrderResponse {
  id: number;
  status: string;
  totalAmount: number;
  originalAmount?: number;
  discountAmount?: number;
  promoCode?: string;
  paymentMethod?: string;
  paymentStatus: string;
  vnpayTxnRef?: string;
  orderItems: OrderItemResponse[];
  createdDate: string;
  updatedDate: string;
}

export interface ValidatePromoCodeResponse {
  valid: boolean;
  discountType?: "PERCENTAGE" | "FLAT";
  discountValue?: number;
  discountAmount?: number;
  message?: string;
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
  promoCode?: string;
}

export const ORDER_STATUSES: Record<string, { label: string; color: string }> = {
  PENDING: { label: "Chờ thanh toán", color: "bg-yellow-500/20 text-yellow-400" },
  COMPLETED: { label: "Hoàn thành", color: "bg-green-500/20 text-green-400" },
  CANCELLED: { label: "Đã hủy", color: "bg-zinc-600 text-zinc-300" },
};

export const PAYMENT_METHODS: Record<string, string> = {
  E_WALLET: "Ví điện tử (VNPay)",
};
