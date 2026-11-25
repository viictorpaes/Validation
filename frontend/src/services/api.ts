import axios, { AxiosError } from 'axios';
import {
  ApiErrorResponse,
  DashboardStats,
  DocumentSummary,
  DocumentType,
  DocumentValidationResult,
  ValidationStatus,
} from '../types/document';

const baseURL = import.meta.env.VITE_API_BASE_URL || '/api';

export const apiClient = axios.create({
  baseURL,
  timeout: 90000,
});

export function extractApiErrorMessage(error: unknown): string {
  if (axios.isAxiosError(error)) {
    const axiosErr = error as AxiosError<ApiErrorResponse>;
    if (axiosErr.response?.data?.message) {
      const details = axiosErr.response.data.details;
      if (details && details.length > 0) {
        return `${axiosErr.response.data.message}: ${details.join('; ')}`;
      }
      return axiosErr.response.data.message;
    }
    if (axiosErr.code === 'ECONNABORTED') {
      return 'O tempo limite de processamento foi excedido. Tente novamente com uma imagem mais leve.';
    }
    if (!axiosErr.response) {
      return 'Não foi possível conectar ao servidor de validação. Verifique se o backend está em execução.';
    }
  }
  if (error instanceof Error) {
    return error.message;
  }
  return 'Ocorreu um erro inesperado ao comunicar com o servidor.';
}

export interface UploadDocumentPayload {
  file: File;
  documentType: DocumentType;
  holderName: string;
  cpf?: string;
  onUploadProgress?: (percent: number) => void;
}

export const documentService = {
  async uploadDocument(payload: UploadDocumentPayload): Promise<DocumentValidationResult> {
    const formData = new FormData();
    formData.append('file', payload.file);
    formData.append('documentType', payload.documentType);
    formData.append('holderName', payload.holderName.trim());
    if (payload.cpf && payload.cpf.replace(/\D/g, '').length > 0) {
      formData.append('cpf', payload.cpf.replace(/\D/g, ''));
    }

    const response = await apiClient.post<DocumentValidationResult>('/documents', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
      onUploadProgress: (progressEvent) => {
        if (payload.onUploadProgress && progressEvent.total) {
          const percent = Math.round((progressEvent.loaded * 100) / progressEvent.total);
          payload.onUploadProgress(percent);
        }
      },
    });
    return response.data;
  },

  async listDocuments(status?: ValidationStatus): Promise<DocumentSummary[]> {
    const params = status ? { status } : undefined;
    const response = await apiClient.get<DocumentSummary[]>('/documents', { params });
    return response.data;
  },

  async getDocumentById(id: string): Promise<DocumentValidationResult> {
    const response = await apiClient.get<DocumentValidationResult>(`/documents/${id}`);
    return response.data;
  },

  async getDocumentResult(id: string): Promise<DocumentValidationResult> {
    const response = await apiClient.get<DocumentValidationResult>(`/documents/${id}/result`);
    return response.data;
  },

  async getDashboardStats(): Promise<DashboardStats> {
    const response = await apiClient.get<DashboardStats>('/dashboard');
    return response.data;
  },

  async deleteDocument(id: string): Promise<void> {
    await apiClient.delete(`/documents/${id}`);
  },
};
