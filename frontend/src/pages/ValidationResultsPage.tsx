import React, { useCallback, useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import {
  ArrowLeft,
  CheckCircle2,
  XCircle,
  AlertTriangle,
  FileUp,
  Cpu,
} from 'lucide-react';
import { DocumentSummary, DocumentValidationResult } from '../types/document';
import { documentService, extractApiErrorMessage } from '../services/api';
import {
  DOCUMENT_TYPE_LABELS,
  formatDateOnly,
  formatDateTime,
  formatFileSize,
  maskCpfDisplay,
} from '../utils/formatters';
import { ProcessingBadge, StatusBadge } from '../components/StatusBadge';
import { LoadingState } from '../components/LoadingState';
import { AlertBanner } from '../components/AlertBanner';

export const ValidationResultsPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const [documentsList, setDocumentsList] = useState<DocumentSummary[]>([]);
  const [result, setResult] = useState<DocumentValidationResult | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const loadData = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const list = await documentService.listDocuments();
      setDocumentsList(list);

      const targetId = id || list[0]?.id;
      if (targetId) {
        const detail = await documentService.getDocumentResult(targetId);
        setResult(detail);
      } else {
        setResult(null);
      }
    } catch (err) {
      setError(extractApiErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, [id]);

  useEffect(() => {
    void loadData();
  }, [loadData]);

  return (
    <div>
      <Link to="/submissions" className="page-back-link">
        <ArrowLeft size={16} />
        <span>Ir para Meus envios</span>
      </Link>

      <div className="page-header">
        <div>
          <h1 className="page-title">Resultados da Validação</h1>
          <p className="page-subtitle">
            Detalhamento dos dados extraídos via IA e avaliação determinística das regras de
            negócio.
          </p>
        </div>

        <div style={{ display: 'flex', gap: '0.75rem', alignItems: 'center', flexWrap: 'wrap' }}>
          {documentsList.length > 0 && (
            <select
              aria-label="Selecionar comprovante processado"
              className="form-control"
              style={{ width: 'auto', minWidth: '240px' }}
              value={result?.id || ''}
              onChange={(e) => {
                if (e.target.value) {
                  navigate(`/results/${e.target.value}`);
                }
              }}
            >
              {documentsList.map((doc) => (
                <option key={doc.id} value={doc.id}>
                  {doc.originalFileName} ({doc.validationStatus})
                </option>
              ))}
            </select>
          )}

          <Link to="/upload" className="btn btn-primary">
            <FileUp size={16} />
            <span>Enviar novo</span>
          </Link>
        </div>
      </div>

      {error && <AlertBanner type="error" message={error} onClose={() => setError(null)} />}

      {loading ? (
        <LoadingState message="Carregando detalhes do resultado..." />
      ) : !result ? (
        <div className="card empty-state">
          <p style={{ marginBottom: '1rem' }}>
            Nenhum resultado disponível. Envie um comprovante para visualizar a análise completa.
          </p>
          <Link to="/upload" className="btn btn-primary">
            Enviar primeiro comprovante
          </Link>
        </div>
      ) : (
        <>
          <section
            className={`result-hero status-${result.validationStatus}`}
            data-testid="result-hero"
          >
            <div style={{ flex: 1, minWidth: '260px' }}>
              <div
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '0.65rem',
                  marginBottom: '0.65rem',
                  flexWrap: 'wrap',
                }}
              >
                <StatusBadge status={result.validationStatus} />
                <ProcessingBadge status={result.processingStatus} />
                {result.aiSource && (
                  <span className="badge badge-processing">
                    <Cpu size={13} />
                    Motor: {result.aiSource}
                  </span>
                )}
              </div>

              <h2 style={{ fontSize: '1.18rem', fontWeight: 700, marginBottom: '0.35rem' }}>
                {result.originalFileName} ({formatFileSize(result.fileSizeBytes)})
              </h2>
              <p style={{ color: 'var(--color-dark)', fontSize: '0.92rem' }}>
                {result.summaryMessage}
              </p>

              {result.reasons && result.reasons.length > 0 && (
                <ul
                  style={{
                    marginTop: '0.75rem',
                    paddingLeft: '1.2rem',
                    fontSize: '0.85rem',
                    color: 'var(--color-darkest)',
                  }}
                >
                  {result.reasons.map((reason, idx) => (
                    <li key={idx}>{reason}</li>
                  ))}
                </ul>
              )}
            </div>

            <div
              style={{
                fontSize: '0.82rem',
                color: 'var(--color-secondary)',
                textAlign: 'right',
              }}
            >
              <div>
                <strong>Enviado em:</strong> {formatDateTime(result.createdAt)}
              </div>
              <div>
                <strong>Processado em:</strong> {formatDateTime(result.processedAt)}
              </div>
              <div>
                <strong>Caracteres OCR:</strong> {result.ocrCharacterCount}
              </div>
            </div>
          </section>

          <div className="details-grid">
            <section className="card">
              <h3 className="card-title">Informações Extraídas</h3>
              <div className="kv-list">
                <div className="kv-item">
                  <div className="kv-label">Titular Informado</div>
                  <div className="kv-value">{result.informedHolderName}</div>
                </div>

                <div className="kv-item">
                  <div className="kv-label">Titular Extraído (IA)</div>
                  <div className="kv-value">
                    {result.extractedData?.nomeTitular || 'Não identificado'}
                  </div>
                </div>

                <div className="kv-item">
                  <div className="kv-label">CPF Identificado</div>
                  <div className="kv-value">{maskCpfDisplay(result.extractedData?.cpf)}</div>
                </div>

                <div className="kv-item">
                  <div className="kv-label">Data de Emissão</div>
                  <div className="kv-value">
                    {formatDateOnly(result.extractedData?.dataEmissao)}
                  </div>
                </div>

                <div className="kv-item" style={{ gridColumn: '1 / -1' }}>
                  <div className="kv-label">Endereço Identificado</div>
                  <div className="kv-value">
                    {result.extractedData?.endereco || 'Não identificado'}
                  </div>
                </div>

                <div className="kv-item">
                  <div className="kv-label">Cidade / UF</div>
                  <div className="kv-value">
                    {[result.extractedData?.cidade, result.extractedData?.estado]
                      .filter(Boolean)
                      .join(' - ') || 'Não identificado'}
                  </div>
                </div>

                <div className="kv-item">
                  <div className="kv-label">Tipo Classificado</div>
                  <div className="kv-value">
                    {result.extractedData?.tipoDocumento ||
                      DOCUMENT_TYPE_LABELS[result.documentType]}
                  </div>
                </div>
              </div>

              {result.extractedData?.camposAusentes &&
                result.extractedData.camposAusentes.length > 0 && (
                  <div style={{ marginTop: '1rem', fontSize: '0.82rem', color: 'var(--color-warning)' }}>
                    <strong>Campos não encontrados no documento:</strong>{' '}
                    {result.extractedData.camposAusentes.join(', ')}
                  </div>
                )}

              {result.ocrPreview && (
                <div style={{ marginTop: '1.15rem' }}>
                  <div className="kv-label" style={{ marginBottom: '0.35rem' }}>
                    Prévia do Texto Extraído pelo OCR (Dados Sensíveis Mascarados)
                  </div>
                  <div
                    style={{
                      padding: '0.75rem 0.9rem',
                      borderRadius: 'var(--radius-sm)',
                      backgroundColor: 'var(--color-background)',
                      border: '1px solid var(--color-border)',
                      fontSize: '0.8rem',
                      color: 'var(--color-dark)',
                      lineHeight: 1.5,
                    }}
                    data-testid="ocr-preview-box"
                  >
                    {result.ocrPreview}
                  </div>
                </div>
              )}
            </section>

            <section className="card">
              <h3 className="card-title">Regras de Negócio Aplicadas</h3>
              <div className="rules-list">
                {result.appliedRules.map((rule) => (
                  <div key={rule.ruleCode} className={`rule-card outcome-${rule.outcome}`}>
                    {rule.outcome === 'PASSED' ? (
                      <CheckCircle2 size={18} color="var(--color-success)" />
                    ) : rule.outcome === 'FAILED' ? (
                      <XCircle size={18} color="var(--color-error)" />
                    ) : (
                      <AlertTriangle size={18} color="var(--color-warning)" />
                    )}
                    <div>
                      <div className="rule-title">{rule.ruleName}</div>
                      <div className="rule-message">{rule.message}</div>
                    </div>
                  </div>
                ))}
              </div>
            </section>
          </div>
        </>
      )}
    </div>
  );
};
