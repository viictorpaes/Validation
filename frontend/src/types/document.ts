export type DocumentType =
  | 'CONTA_DE_AGUA'
  | 'CONTA_DE_ENERGIA'
  | 'CONTA_DE_TELEFONE'
  | 'FATURA_CARTAO'
  | 'CONTRATO_ALUGUEL'
  | 'OUTROS';

export type ProcessingStatus =
  | 'RECEBIDO'
  | 'PROCESSANDO_IMAGEM'
  | 'EXTRAINDO_TEXTO'
  | 'INTERPRETANDO_IA'
  | 'VALIDANDO_REGRAS'
  | 'CONCLUIDO'
  | 'ERRO';

export type ValidationStatus = 'APROVADO' | 'REPROVADO' | 'PENDENTE_ANALISE';

export type RuleOutcome = 'PASSED' | 'FAILED' | 'INCONCLUSIVE';

export interface ExtractedDocumentData {
  nomeTitular: string | null;
  cpf: string | null;
  endereco: string | null;
  cidade: string | null;
  estado: string | null;
  cep: string | null;
  dataEmissao: string | null;
  tipoDocumento: string | null;
  camposAusentes: string[];
}

export interface ValidationRuleItem {
  ruleCode: string;
  ruleName: string;
  outcome: RuleOutcome;
  passed: boolean;
  message: string;
}

export interface DocumentSummary {
  id: string;
  originalFileName: string;
  contentType: string;
  fileSizeBytes: number;
  informedHolderName: string;
  documentType: DocumentType;
  processingStatus: ProcessingStatus;
  validationStatus: ValidationStatus;
  summaryMessage: string | null;
  createdAt: string;
  processedAt: string | null;
}

export interface DocumentValidationResult {
  id: string;
  originalFileName: string;
  contentType: string;
  fileSizeBytes: number;
  informedHolderName: string;
  informedCpf: string | null;
  documentType: DocumentType;
  processingStatus: ProcessingStatus;
  validationStatus: ValidationStatus;
  summaryMessage: string;
  reasons: string[];
  extractedData: ExtractedDocumentData | null;
  appliedRules: ValidationRuleItem[];
  ocrCharacterCount: number;
  ocrPreview: string | null;
  aiSource: string | null;
  createdAt: string;
  processedAt: string | null;
}

export interface DashboardStats {
  totalDocuments: number;
  approvedDocuments: number;
  rejectedDocuments: number;
  pendingDocuments: number;
  recentDocuments: DocumentSummary[];
}

export interface ApiErrorResponse {
  timestamp?: string;
  status?: number;
  error?: string;
  message: string;
  details?: string[];
}
