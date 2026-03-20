export interface CategoryInfo {
  id: number;
  name: string;
  slug: string;
  icon?: string;
  displayOrder: number;
}

export interface TagInfo {
  id: number;
  name: string;
  slug: string;
  usageCount: number;
}

export interface Event {
  id: number;
  title: string;
  description?: string;
  eventDate: string;
  endDate?: string;
  location: string;
  imageUrl?: string;
  category?: CategoryInfo;
  tags: TagInfo[];
  status: string;
  isFeatured: boolean;
  hasSeatMap: boolean;
  organizer: OrganizerInfo;
  ticketTypes: TicketTypeInfo[];
  createdDate: string;
  updatedDate: string;
  averageRating?: number;
  reviewCount?: number;
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
  categoryId: number;
  tags?: string[];
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
  categoryId?: number;
  tags?: string[];
  status?: string;
  isFeatured?: boolean;
  ticketTypes?: TicketTypeRequest[];
}

export interface EventFilterParams {
  categoryId?: number;
  tag?: string;
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

export const EVENT_STATUSES = ["DRAFT", "PUBLISHED", "CANCELLED"] as const;
