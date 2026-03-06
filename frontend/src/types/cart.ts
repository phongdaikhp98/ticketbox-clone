export interface CartItemResponse {
  id: number;
  quantity: number;
  ticketType: CartTicketTypeSummary;
  event: CartEventSummary;
  createdDate: string;
}

export interface CartTicketTypeSummary {
  id: number;
  name: string;
  price: number;
  availableCount: number;
}

export interface CartEventSummary {
  id: number;
  title: string;
  imageUrl?: string;
  eventDate: string;
  location: string;
}

export interface CartResponse {
  items: CartItemResponse[];
  totalItems: number;
  totalAmount: number;
}

export interface AddToCartRequest {
  ticketTypeId: number;
  quantity: number;
}

export interface UpdateCartItemRequest {
  quantity: number;
}
