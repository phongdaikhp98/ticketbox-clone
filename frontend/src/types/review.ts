export interface Review {
  id: number;
  eventId: number;
  userId: number;
  userName: string;
  userAvatarUrl?: string;
  rating: number;
  comment?: string;
  createdDate: string;
  updatedDate?: string;
}

export interface ReviewRequest {
  rating: number;
  comment?: string;
}
