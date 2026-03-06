export interface DashboardOverviewResponse {
  totalEvents: number;
  totalRevenue: number;
  totalTicketsSold: number;
  totalCheckedIn: number;
  recentOrders: RecentOrderDto[];
}

export interface RecentOrderDto {
  orderId: number;
  customerName: string;
  customerEmail: string;
  eventTitle: string;
  totalAmount: number;
  status: string;
  createdDate: string;
}

export interface EventStatsResponse {
  event: EventSummaryDto;
  totalRevenue: number;
  totalTicketsSold: number;
  totalCapacity: number;
  totalCheckedIn: number;
  totalIssued: number;
  totalCancelled: number;
  ticketTypeStats: TicketTypeStatsDto[];
}

export interface EventSummaryDto {
  id: number;
  title: string;
  eventDate: string;
  endDate?: string;
  location: string;
  imageUrl?: string;
  status: string;
}

export interface TicketTypeStatsDto {
  ticketTypeId: number;
  name: string;
  price: number;
  capacity: number;
  soldCount: number;
  revenue: number;
  checkedInCount: number;
}

export interface AttendeeResponse {
  ticketId: number;
  ticketCode: string;
  attendeeName: string;
  attendeeEmail: string;
  ticketTypeName: string;
  status: string;
  usedAt?: string;
  createdDate: string;
}
