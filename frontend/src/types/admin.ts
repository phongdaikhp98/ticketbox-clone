export interface AdminOverview {
  totalUsers: number;
  totalEvents: number;
  totalRevenue: number;
  totalOrders: number;
  totalTicketsSold: number;
  totalCheckedIn: number;
  recentOrders: AdminRecentOrder[];
  topEventsByRevenue: TopEventRevenue[];
}

export interface AdminRecentOrder {
  orderId: number;
  customerName: string;
  customerEmail: string;
  eventTitle: string;
  totalAmount: number;
  status: string;
  createdDate: string;
}

export interface TopEventRevenue {
  eventId: number;
  eventTitle: string;
  revenue: number;
}

export interface AdminUser {
  id: number;
  fullName: string;
  email: string;
  phone?: string;
  role: string;
  isActive: boolean;
  emailVerified: boolean;
  createdDate: string;
}

export interface AdminOrder {
  id: number;
  customerName: string;
  customerEmail: string;
  eventTitle: string;
  totalAmount: number;
  status: string;
  paymentStatus: string;
  paymentMethod?: string;
  createdDate: string;
  orderItems: AdminOrderItem[];
}

export interface AdminOrderItem {
  id: number;
  eventTitle: string;
  ticketTypeName: string;
  quantity: number;
  unitPrice: number;
}

export interface AdminEvent {
  id: number;
  title: string;
  organizerName: string;
  organizerId: number;
  category: string;
  status: string;
  isFeatured: boolean;
  totalCapacity: number;
  totalSold: number;
  eventDate: string;
  createdDate: string;
}
