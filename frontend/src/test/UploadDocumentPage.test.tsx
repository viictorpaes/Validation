import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it, vi, beforeEach } from 'vitest';
import { UploadDocumentPage } from '../pages/UploadDocumentPage';
import { documentService } from '../services/api';
import { DocumentValidationResult } from '../types/document';

vi.mock('../services/api', async () => {
  const actual = await vi.importActual<typeof import('../services/api')>('../services/api');
  return {
    ...actual,
    documentService: {
      ...actual.documentService,
      uploadDocument: vi.fn(),
    },
  };
});

describe('Tela de Upload de Comprovante', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('envia comprovante válido, exibe carregamento e apresenta resumo de aprovação', async () => {
    const mockResult: DocumentValidationResult = {
      id: '11111111-2222-3333-4444-555555555555',
      originalFileName: 'conta_luz.pdf',
      contentType: 'application/pdf',
      fileSizeBytes: 120400,
      informedHolderName: 'Helena Costa Sintetica',
      informedCpf: null,
      documentType: 'CONTA_DE_ENERGIA',
      processingStatus: 'CONCLUIDO',
      validationStatus: 'APROVADO',
      summaryMessage: 'Comprovante de residência validado e aprovado com sucesso.',
      reasons: ['Todas as regras foram atendidas.'],
      extractedData: {
        nomeTitular: 'Helena Costa Sintetica',
        cpf: '529.***.***-25',
        endereco: 'Rua das Palmeiras, 80',
        cidade: 'Recife',
        estado: 'PE',
        cep: '50000000',
        dataEmissao: '2026-11-10',
        tipoDocumento: 'CONTA_DE_ENERGIA',
        camposAusentes: [],
      },
      appliedRules: [],
      ocrCharacterCount: 150,
      ocrPreview: 'CONTA DE ENERGIA HELENA COSTA SINTETICA',
      aiSource: 'GEMINI_AI',
      createdAt: '2026-11-25T04:00:00Z',
      processedAt: '2026-11-25T04:00:02Z',
    };

    vi.mocked(documentService.uploadDocument).mockResolvedValueOnce(mockResult);

    render(
      <MemoryRouter>
        <UploadDocumentPage />
      </MemoryRouter>,
    );

    fireEvent.change(screen.getByLabelText(/Nome completo do titular/i), {
      target: { value: 'Helena Costa Sintetica' },
    });

    const fileInput = screen.getByTestId('file-input');
    const pdfFile = new File(['%PDF-1.4 fake content'], 'conta_luz.pdf', {
      type: 'application/pdf',
    });
    fireEvent.change(fileInput, { target: { files: [pdfFile] } });

    expect(screen.getByTestId('selected-file-bar')).toHaveTextContent('conta_luz.pdf');

    fireEvent.click(screen.getByRole('button', { name: /Processar documento/i }));

    await waitFor(() => {
      expect(screen.getByTestId('upload-result-summary')).toBeInTheDocument();
    });

    expect(
      screen.getByText('Comprovante de residência validado e aprovado com sucesso.'),
    ).toBeInTheDocument();
    expect(documentService.uploadDocument).toHaveBeenCalledTimes(1);
  });

  it('exibe mensagem de erro amigável quando o envio falha na API', async () => {
    vi.mocked(documentService.uploadDocument).mockRejectedValueOnce(
      new Error('Falha simulada na comunicação com o servidor'),
    );

    render(
      <MemoryRouter>
        <UploadDocumentPage />
      </MemoryRouter>,
    );

    fireEvent.change(screen.getByLabelText(/Nome completo do titular/i), {
      target: { value: 'Helena Costa Sintetica' },
    });

    const fileInput = screen.getByTestId('file-input');
    const pngFile = new File(['png-bytes'], 'comprovante.png', { type: 'image/png' });
    fireEvent.change(fileInput, { target: { files: [pngFile] } });

    fireEvent.click(screen.getByRole('button', { name: /Processar documento/i }));

    await waitFor(() => {
      expect(screen.getByRole('alert')).toHaveTextContent(
        'Falha simulada na comunicação com o servidor',
      );
    });
  });
});
