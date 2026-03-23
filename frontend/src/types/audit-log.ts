export interface AuditLog {
  id: number;
  adminId: number;
  adminName: string;
  adminEmail: string;
  // Actions: CHANGE_ROLE | TOGGLE_ACTIVE | TOGGLE_FEATURED | SET_FEATURED_ORDER
  //          | CHANGE_EVENT_STATUS | APPROVE_ORGANIZER | REJECT_ORGANIZER | CHECK_IN
  action: string;
  // Entity types: USER | EVENT | OrganizerApplication | TICKET
  entityType: string;
  entityId: number;
  entityName: string;
  oldValue: string;
  newValue: string;
  createdDate: string;
}
