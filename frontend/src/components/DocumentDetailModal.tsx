import React from 'react';
import { Link } from 'react-router-dom';
import { X, CheckCircle2, XCircle, AlertTriangle } from 'lucide-react';
import { DocumentValidationResult } from '../types/document';
import {
  DOCUMENT_TYPE_LABELS,
  formatDateOnly,
  formatDateTime,
  maskCpfDisplay,
} from '../utils/formatters';
import { StatusBadge } from './StatusBadge';

interface DocumentDetailModalProps {
  document: DocumentValidationResult | null;
  loading?: boolean;
  onClose: () => void;
}

export const DocumentDetailModal: React.FC<DocumentDetailModalProps> = ({
  document,
  loading,
  onClose,
}) => {
  if (!document && !loading) return null;

  return (
    <div className="modal-overlay" onClick={onClose} role="dialog" aria-modal="true">
      <div className="modal-content" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <div>
            <h3 style={{ fontSize: '1.1rem', fontWeight: 700 }}>
              {document ? document.originalFileName : 'Carregando detalhes...'}
            </h3>
            {document && (
              <span style={{ fontSize: '0.8rem', color: 'var(--color-secondary)' }}>
                Processado em {formatDateTime(document.processedAt || document.createdAt)}
              </span>
            )}
          </div>
          <button type="button" className="icon-btn" onClick={onClose} aria-label="Fechar modal">
            <X size={20} />
          </button>
        </div>

        {loading || !document ? (
          <div className="loading-box">
            <div className="spinner" />
            <p>Buscando dados da validação...</p>
          </div>
        ) : (
          <div>
            <div
              style={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                marginBottom: '1rem',
                gap: '0.75rem',
                flexWrap: 'wrap',
              }}
            >
              <StatusBadge status={document.validationStatus} />
              <span style={{ fontSize: '0.84rem', color: 'var(--color-dark)' }}>
                {document.summaryMessage}
              </span>
            </div>

            <h4 style={{ fontSize: '0.92rem', marginBottom: '0.6rem', color: 'var(--color-dark)' }}>
              Dados Extraídos
            </h4>
            <div className="kv-list" style={{ marginBottom: '1.25rem' }}>
              <div className="kv-item">
                <div className="kv-label">Titular Extraído</div>
                <div className="kv-value">
                  {document.extractedData?.nomeTitular || 'Não identificado'}
                </div>
              </div>
              <div className="kv-item">
                <div className="kv-label">CPF</div>
                <div className="kv-value">{maskCpfDisplay(document.extractedData?.cpf)}</div>
              </div>
              <div className="kv-item">
                <div className="kv-label">Endereço</div>
                <div className="kv-value">
                  {document.extractedData?.endereco || 'Não identificado'}
                </div>
              </div>
              <div className="kv-item">
                <div className="kv-label">Cidade / UF • Emissão</div>
                <div className="kv-value">
                  {[document.extractedData?.cidade, document.extractedData?.estado]
                    .filter(Boolean)
                    .join(' - ') || '—'}{' '}
                  • {formatDateOnly(document.extractedData?.dataEmissao)}
                </div>
              </div>
            </div>

            <h4 style={{ fontSize: '0.92rem', marginBottom: '0.6rem', color: 'var(--color-dark)' }}>
              Regras Aplicadas ({DOCUMENT_TYPE_LABELS[document.documentType]})
            </h4>
            <div className="rules-list" style={{ marginBottom: '1.25rem' }}>
              {document.appliedRules.map((rule) => (
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

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem' }}>
              <button type="button" className="btn btn-secondary" onClick={onClose}>
                Fechar
              </button>
              <Link to={`/results/${document.id}`} className="btn btn-primary" onClick={onClose}>
                Abrir relatório completo
              </Link>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};
