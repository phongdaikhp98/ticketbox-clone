export interface WishlistResponse {
  id: number;
  eventId: number;
  eventTitle: string;
  eventImageUrl?: string;
  eventDate: string;
  eventLocation: string;
  eventCategory: string;
  minPrice: number;
  createdDate: string;
}

export interface WishlistCheckResponse {
  wishlisted: boolean;
}
