import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { describe, expect, it, vi, beforeEach } from 'vitest';
import { DashboardPage } from '../pages/DashboardPage';
import { ValidationResultsPage } from '../pages/ValidationResultsPage';
import { documentService } from '../services/api';

vi.mock('../services/api', async () => {
  const actual = await vi.importActual<typeof import('../services/api')>('../services/api');
  return {
    ...actual,
    documentService: {
      ...actual.documentService,
      getDashboardStats: vi.fn(),
      listDocuments: vi.fn(),
      getDocumentResult: vi.fn(),
    },
  };
});

describe('Telas de Dashboard e Resultados (carregamento, exibição e erro)', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('exibe indicadores do Dashboard após o carregamento', async () => {
    vi.mocked(documentService.getDashboardStats).mockResolvedValueOnce({
      totalDocuments: 7,
      approvedDocuments: 4,
      rejectedDocuments: 2,
      pendingDocuments: 1,
      recentDocuments: [],
    });

    render(
      <MemoryRouter>
        <DashboardPage />
      </MemoryRouter>,
    );

    expect(screen.getByRole('status')).toHaveTextContent(/Carregando indicadores/i);

    await waitFor(() => {
      expect(screen.getByTestId('stat-primary')).toHaveTextContent('7');
    });
    expect(screen.getByTestId('stat-success')).toHaveTextContent('4');
    expect(screen.getByTestId('stat-error')).toHaveTextContent('2');
    expect(screen.getByTestId('stat-warning')).toHaveTextContent('1');
  });

  it('exibe estado de erro no Dashboard caso a API retorne falha', async () => {
    vi.mocked(documentService.getDashboardStats).mockRejectedValueOnce(
      new Error('Banco de dados indisponível temporariamente'),
    );

    render(
      <MemoryRouter>
        <DashboardPage />
      </MemoryRouter>,
    );

    await waitFor(() => {
      expect(screen.getByRole('alert')).toHaveTextContent(
        'Banco de dados indisponível temporariamente',
      );
    });
  });

  it('exibe detalhes extraídos, regras aplicadas e prévia mascarada do OCR na tela de Resultados', async () => {
    const docId = '99999999-8888-7777-6666-555555555555';
    vi.mocked(documentService.listDocuments).mockResolvedValueOnce([
      {
        id: docId,
        originalFileName: 'conta_agua_reprovada.jpg',
        contentType: 'image/jpeg',
        fileSizeBytes: 85000,
        informedHolderName: 'Paulo Roberto Teste',
        documentType: 'CONTA_DE_AGUA',
        processingStatus: 'CONCLUIDO',
        validationStatus: 'REPROVADO',
        summaryMessage: 'Comprovante reprovado pelas regras de negócio.',
        createdAt: '2026-11-25T03:00:00Z',
        processedAt: '2026-11-25T03:00:03Z',
      },
    ]);

    vi.mocked(documentService.getDocumentResult).mockResolvedValueOnce({
      id: docId,
      originalFileName: 'conta_agua_reprovada.jpg',
      contentType: 'image/jpeg',
      fileSizeBytes: 85000,
      informedHolderName: 'Paulo Roberto Teste',
      informedCpf: null,
      documentType: 'CONTA_DE_AGUA',
      processingStatus: 'CONCLUIDO',
      validationStatus: 'REPROVADO',
      summaryMessage: 'Comprovante reprovado pelas regras de negócio.',
      reasons: ['Comprovante emitido há 140 dias, acima do limite permitido de 90 dias.'],
      extractedData: {
        nomeTitular: 'Paulo Roberto Teste',
        cpf: '529.***.***-25',
        endereco: 'Av. Boa Viagem Sintetica, 500',
        cidade: 'Recife',
        estado: 'PE',
        cep: '51000000',
        dataEmissao: '2026-07-08',
        tipoDocumento: 'CONTA_DE_AGUA',
        camposAusentes: [],
      },
      appliedRules: [
        {
          ruleCode: 'ISSUE_DATE_VALIDITY',
          ruleName: 'Prazo de Emissão do Comprovante',
          outcome: 'FAILED',
          passed: false,
          message: 'Comprovante emitido há 140 dias, acima do limite permitido de 90 dias.',
        },
      ],
      ocrCharacterCount: 112,
      ocrPreview: 'COMPESA TITULAR PAULO ROBERTO TESTE CPF 529.***.***-25',
      aiSource: 'GEMINI_AI',
      createdAt: '2026-11-25T03:00:00Z',
      processedAt: '2026-11-25T03:00:03Z',
    });

    render(
      <MemoryRouter initialEntries={[`/results/${docId}`]}>
        <Routes>
          <Route path="/results/:id" element={<ValidationResultsPage />} />
        </Routes>
      </MemoryRouter>,
    );

    await waitFor(() => {
      expect(screen.getByTestId('result-hero')).toBeInTheDocument();
    });

    expect(screen.getByText('Prazo de Emissão do Comprovante')).toBeInTheDocument();
    expect(screen.getByText('Av. Boa Viagem Sintetica, 500')).toBeInTheDocument();
    expect(screen.getByTestId('ocr-preview-box')).toHaveTextContent('529.***.***-25');
  });
});
