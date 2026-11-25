import React from 'react';
import { Link } from 'react-router-dom';
import { Eye, ExternalLink, Trash2 } from 'lucide-react';
import { DocumentSummary } from '../types/document';
import { DOCUMENT_TYPE_LABELS, formatDateTime, formatFileSize } from '../utils/formatters';
import { ProcessingBadge, StatusBadge } from './StatusBadge';

interface DocumentTableProps {
  documents: DocumentSummary[];
  onViewModal?: (id: string) => void;
  onDelete?: (id: string) => void;
  deletingId?: string | null;
}

export const DocumentTable: React.FC<DocumentTableProps> = ({
  documents,
  onViewModal,
  onDelete,
  deletingId,
}) => {
  if (documents.length === 0) {
    return (
      <div className="empty-state">
        <p>Nenhum comprovante registrado até o momento.</p>
      </div>
    );
  }

  return (
    <div className="table-wrapper">
      <table className="data-table">
        <thead>
          <tr>
            <th>Arquivo</th>
            <th>Titular Informado</th>
            <th>Tipo</th>
            <th>Data do Envio</th>
            <th>Processamento</th>
            <th>Resultado</th>
            <th>Ações</th>
          </tr>
        </thead>
        <tbody>
          {documents.map((doc) => (
            <tr key={doc.id}>
              <td>
                <div style={{ fontWeight: 600 }}>{doc.originalFileName}</div>
                <div style={{ fontSize: '0.75rem', color: 'var(--color-secondary)' }}>
                  {formatFileSize(doc.fileSizeBytes)}
                </div>
              </td>
              <td>{doc.informedHolderName}</td>
              <td>{DOCUMENT_TYPE_LABELS[doc.documentType] || doc.documentType}</td>
              <td>{formatDateTime(doc.createdAt)}</td>
              <td>
                <ProcessingBadge status={doc.processingStatus} />
              </td>
              <td>
                <StatusBadge status={doc.validationStatus} />
              </td>
              <td>
                <div className="table-actions">
                  {onViewModal && (
                    <button
                      type="button"
                      className="btn btn-secondary btn-sm"
                      onClick={() => onViewModal(doc.id)}
                      title="Resumo rápido"
                    >
                      <Eye size={14} />
                      <span>Resumo</span>
                    </button>
                  )}
                  <Link
                    to={`/results/${doc.id}`}
                    className="btn btn-secondary btn-sm"
                    title="Ver página completa de resultado"
                  >
                    <ExternalLink size={14} />
                    <span>Detalhes</span>
                  </Link>
                  {onDelete && (
                    <button
                      type="button"
                      className="btn btn-danger-ghost btn-sm"
                      disabled={deletingId === doc.id}
                      onClick={() => onDelete(doc.id)}
                      aria-label={`Remover ${doc.originalFileName}`}
                      title="Remover comprovante"
                    >
                      <Trash2 size={15} />
                    </button>
                  )}
                </div>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
};
