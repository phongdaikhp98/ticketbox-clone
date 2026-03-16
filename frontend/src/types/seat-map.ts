export interface SeatResponse {
  id: number;
  seatCode: string;
  rowLabel: string;
  seatNumber: number;
  status: "AVAILABLE" | "SOLD" | "BLOCKED" | "RESERVED";
  reservedByMe: boolean;
}

export interface SectionResponse {
  id: number;
  name: string;
  color: string;
  ticketTypeId: number;
  ticketTypeName: string;
  price: number;
  seats: SeatResponse[];
}

export interface SeatMapResponse {
  id: number;
  eventId: number;
  name: string;
  sections: SectionResponse[];
}

export interface SectionConfig {
  name: string;
  color: string;
  ticketTypeId: number;
  rowLabels: string[];
  seatsPerRow: number;
}

export interface CreateSeatMapRequest {
  eventId: number;
  name: string;
  sections: SectionConfig[];
}
