import { DocumentType, ProcessingStatus, ValidationStatus } from '../types/document';

export const DOCUMENT_TYPE_LABELS: Record<DocumentType, string> = {
  CONTA_DE_AGUA: 'Conta de água',
  CONTA_DE_ENERGIA: 'Conta de energia elétrica',
  CONTA_DE_TELEFONE: 'Conta de telefone / internet',
  FATURA_CARTAO: 'Fatura de cartão de crédito',
  CONTRATO_ALUGUEL: 'Contrato de aluguel vigente',
  OUTROS: 'Outros comprovantes aceitos',
};

export const VALIDATION_STATUS_LABELS: Record<ValidationStatus, string> = {
  APROVADO: 'Aprovado',
  REPROVADO: 'Reprovado',
  PENDENTE_ANALISE: 'Pendente de análise',
};

export const PROCESSING_STATUS_LABELS: Record<ProcessingStatus, string> = {
  RECEBIDO: 'Recebido',
  PROCESSANDO_IMAGEM: 'Tratando imagem (OpenCV)',
  EXTRAINDO_TEXTO: 'Extraindo texto (OCR)',
  INTERPRETANDO_IA: 'Interpretando dados (IA)',
  VALIDANDO_REGRAS: 'Validando regras',
  CONCLUIDO: 'Concluído',
  ERRO: 'Erro no processamento',
};

export function formatFileSize(bytes: number): string {
  if (!Number.isFinite(bytes) || bytes < 0) return '0 B';
  if (bytes < 1024) return `${bytes} B`;
  const kb = bytes / 1024;
  if (kb < 1024) return `${kb.toFixed(kb >= 100 ? 0 : 1)} KB`;
  const mb = kb / 1024;
  return `${mb.toFixed(2)} MB`;
}

export function formatDateTime(isoString: string | null | undefined): string {
  if (!isoString) return '—';
  const date = new Date(isoString);
  if (Number.isNaN(date.getTime())) return isoString;
  return new Intl.DateTimeFormat('pt-BR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(date);
}

export function formatDateOnly(isoDate: string | null | undefined): string {
  if (!isoDate) return 'Não identificado';
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(isoDate.trim());
  if (match) {
    return `${match[3]}/${match[2]}/${match[1]}`;
  }
  return isoDate;
}

export function maskCpfDisplay(cpf: string | null | undefined): string {
  if (!cpf) return 'Não informado / não identificado';
  const digits = cpf.replace(/\D/g, '');
  if (digits.length !== 11) return cpf;
  return `${digits.slice(0, 3)}.***.***-${digits.slice(9)}`;
}

export function formatCpfInput(value: string): string {
  const digits = value.replace(/\D/g, '').slice(0, 11);
  if (digits.length <= 3) return digits;
  if (digits.length <= 6) return `${digits.slice(0, 3)}.${digits.slice(3)}`;
  if (digits.length <= 9) {
    return `${digits.slice(0, 3)}.${digits.slice(3, 6)}.${digits.slice(6)}`;
  }
  return `${digits.slice(0, 3)}.${digits.slice(3, 6)}.${digits.slice(6, 9)}-${digits.slice(9)}`;
}

export const MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024;
export const ACCEPTED_MIME_TYPES = ['application/pdf', 'image/png', 'image/jpeg'];
export const ACCEPTED_EXTENSIONS = ['.pdf', '.png', '.jpg', '.jpeg'];

export function validateClientFile(file: File): string | null {
  const lowerName = file.name.toLowerCase();
  const validExt = ACCEPTED_EXTENSIONS.some((ext) => lowerName.endsWith(ext));
  const validMime = ACCEPTED_MIME_TYPES.includes(file.type);

  if (!validExt || (file.type && !validMime)) {
    return 'Formato de arquivo inválido. Envie apenas arquivos PDF, PNG ou JPG/JPEG.';
  }
  if (file.size === 0) {
    return 'O arquivo selecionado está vazio.';
  }
  if (file.size > MAX_FILE_SIZE_BYTES) {
    return `O arquivo excede o tamanho máximo permitido de 10 MB (${formatFileSize(file.size)}).`;
  }
  return null;
}
