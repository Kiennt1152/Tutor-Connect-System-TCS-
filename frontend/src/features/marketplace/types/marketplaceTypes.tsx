export interface MarketplaceRequest {}

export interface MarketplaceResponse {}

export type ClassTerminationStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'COMPLETED';

export interface CreateClassTerminationRequest {
  assignmentId?: number;
  reason: string;
  effectiveDate?: string;
}

export interface ClassTerminationResponse {
  terminationId: number;
  classId: number;
  assignmentId: number;
  requestedByUserId: number;
  reason: string;
  effectiveDate: string | null;
  status: ClassTerminationStatus;
  createdAt: string;
  processedAt: string | null;
}
