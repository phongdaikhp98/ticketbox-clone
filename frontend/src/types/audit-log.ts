export interface AuditLog {
  id: number;
  adminId: number;
  adminName: string;
  adminEmail: string;
  action: string; // CHANGE_ROLE | TOGGLE_ACTIVE | TOGGLE_FEATURED | CHANGE_EVENT_STATUS
  entityType: string; // USER | EVENT
  entityId: number;
  entityName: string; // email or event title
  oldValue: string;
  newValue: string;
  createdDate: string;
}
