export type InvestigationState =
  | 'CREATED'
  | 'VALIDATING'
  | 'DISCOVERING'
  | 'ACQUIRING'
  | 'COMPARING'
  | 'ASSESSING'
  | 'REFUTING'
  | 'REPORTING'
  | 'DONE'
  | 'PARTIAL'
  | 'FAILED';

export type InvestigationMode = 'LIVE' | 'REPLAY';

export interface Investigation {
  id: string;
  inputType: string;
  inputSha256: string;
  claimText: string;
  state: InvestigationState;
  mode: InvestigationMode;
  createdAt: string;
  finishedAt?: string;
  agentStepsUsed: number;
}

export interface Task {
  id: string;
  investigationId: string;
  channel: 'SEARCH' | 'FETCH' | 'AGENT' | 'DOWNLOAD' | 'COMPARE' | 'REFUTE';
  status: 'QUEUED' | 'RUNNING' | 'OK' | 'EMPTY' | 'BLOCKED' | 'TIMEOUT' | 'ERROR';
  tinyfishRunId?: string;
  startedAt?: string;
  errorText?: string;
}

export interface Evidence {
  id: string;
  investigationId: string;
  kind: 'SUBMITTED_IMAGE' | 'SOURCE_PAGE' | 'CANDIDATE_IMAGE' | 'COMPARISON_RESULT' | 'SEARCH_RESULT';
  sourceUrl?: string;
  retrievedAt: string;
  contentSha256: string;
  filePath?: string;
  extractionMethod?: string;
  sourceTier?: 'FACT_CHECKER' | 'WIRE' | 'MAJOR_OUTLET' | 'OTHER' | 'SOCIAL' | 'UNKNOWN';
  dateValue?: string;
  dateSource: 'PAGE_METADATA' | 'VISIBLE_TEXT' | 'UNKNOWN';
  mode: InvestigationMode;
  rawJson?: string;
}

export interface Comparison {
  id: string;
  investigationId: string;
  submittedEvidenceId: string;
  candidateEvidenceId: string;
  alignmentStatus: 'ALIGNED' | 'NOT_COMPARABLE';
  inlierCount?: number;
  ssimNormalized?: number;
  diffRegionCount?: number;
  diffImagePath?: string;
  paramsJson?: string;
}

export interface Finding {
  id: string;
  investigationId: string;
  ruleId: string;
  outcome: 'SUPPORTED' | 'CONTRADICTED' | 'INCONCLUSIVE' | 'NO_COMPARABLE_SOURCE';
  confidence: 'LOW' | 'MEDIUM' | 'HIGH';
  explanation: string;
  createdAt: string;
  evidenceIds: string[];
}

export interface SseEvent {
  id: string;
  type: string;
  ts: string;
  payload: any;
}
