export interface Event {
  id: number;
  title: string;
  description?: string;
  eventDate: string;
  endDate?: string;
  location: string;
  imageUrl?: string;
  category: string;
  status: string;
  isFeatured: boolean;
  organizer: OrganizerInfo;
  ticketTypes: TicketTypeInfo[];
  createdDate: string;
  updatedDate: string;
}

export interface OrganizerInfo {
  id: number;
  fullName: string;
  avatarUrl?: string;
}

export interface TicketTypeInfo {
  id: number;
  name: string;
  price: number;
  capacity: number;
  soldCount: number;
  availableCount: number;
}

export interface CreateEventRequest {
  title: string;
  description?: string;
  eventDate: string;
  endDate?: string;
  location: string;
  imageUrl?: string;
  category: string;
  isFeatured?: boolean;
  ticketTypes: TicketTypeRequest[];
}

export interface TicketTypeRequest {
  name: string;
  price: number;
  capacity: number;
}

export interface UpdateEventRequest {
  title?: string;
  description?: string;
  eventDate?: string;
  endDate?: string;
  location?: string;
  imageUrl?: string;
  category?: string;
  status?: string;
  isFeatured?: boolean;
  ticketTypes?: TicketTypeRequest[];
}

export interface EventFilterParams {
  category?: string;
  dateFrom?: string;
  dateTo?: string;
  priceMin?: number;
  priceMax?: number;
  location?: string;
  search?: string;
  sort?: string;
  direction?: string;
  page?: number;
  size?: number;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export const EVENT_CATEGORIES = [
  "MUSIC",
  "SPORTS",
  "CONFERENCE",
  "THEATER",
  "FILM",
  "WORKSHOP",
  "OTHER",
] as const;

export const EVENT_STATUSES = ["DRAFT", "PUBLISHED", "CANCELLED"] as const;
