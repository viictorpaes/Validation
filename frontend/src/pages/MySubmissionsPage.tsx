import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { FileUp, RefreshCw } from 'lucide-react';
import { DocumentSummary, ValidationStatus } from '../types/document';
import { documentService, extractApiErrorMessage } from '../services/api';
import { DocumentTable } from '../components/DocumentTable';
import { LoadingState } from '../components/LoadingState';
import { AlertBanner } from '../components/AlertBanner';
import { DocumentDetailModal } from '../components/DocumentDetailModal';
import { useDocumentModal } from '../hooks/useDocumentModal';

type StatusFilter = 'ALL' | ValidationStatus;

export const MySubmissionsPage: React.FC = () => {
  const [documents, setDocuments] = useState<DocumentSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [statusFilter, setStatusFilter] = useState<StatusFilter>('ALL');
  const [searchTerm, setSearchTerm] = useState('');
  const [deletingId, setDeletingId] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  const {
    selectedDoc,
    modalLoading,
    modalError,
    setModalError,
    openDocumentModal,
    closeDocumentModal,
  } = useDocumentModal();

  const loadDocuments = useCallback(async (filter: StatusFilter) => {
    setLoading(true);
    setError(null);
    try {
      const list = await documentService.listDocuments(filter === 'ALL' ? undefined : filter);
      setDocuments(list);
    } catch (err) {
      setError(extractApiErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadDocuments(statusFilter);
  }, [loadDocuments, statusFilter]);

  const handleDelete = async (id: string) => {
    setDeletingId(id);
    setError(null);
    setSuccessMessage(null);
    try {
      await documentService.deleteDocument(id);
      setDocuments((prev) => prev.filter((item) => item.id !== id));
      setSuccessMessage('Comprovante removido com segurança.');
    } catch (err) {
      setError(extractApiErrorMessage(err));
    } finally {
      setDeletingId(null);
    }
  };

  const filteredDocs = useMemo(() => {
    const query = searchTerm.trim().toLowerCase();
    if (!query) return documents;
    return documents.filter(
      (doc) =>
        doc.originalFileName.toLowerCase().includes(query) ||
        doc.informedHolderName.toLowerCase().includes(query),
    );
  }, [documents, searchTerm]);

  return (
    <div>
      <div className="page-header">
        <div>
          <h1 className="page-title">Meus envios</h1>
          <p className="page-subtitle">
            Histórico completo dos comprovantes enviados, status de processamento e validação.
          </p>
        </div>
        <div style={{ display: 'flex', gap: '0.65rem' }}>
          <button
            type="button"
            className="btn btn-secondary"
            onClick={() => void loadDocuments(statusFilter)}
            disabled={loading}
          >
            <RefreshCw size={16} />
            <span>Atualizar</span>
          </button>
          <Link to="/upload" className="btn btn-primary">
            <FileUp size={16} />
            <span>Novo envio</span>
          </Link>
        </div>
      </div>

      {error && <AlertBanner type="error" message={error} onClose={() => setError(null)} />}
      {modalError && (
        <AlertBanner type="error" message={modalError} onClose={() => setModalError(null)} />
      )}
      {successMessage && (
        <AlertBanner
          type="success"
          message={successMessage}
          onClose={() => setSuccessMessage(null)}
        />
      )}

      <section className="card">
        <div className="filter-bar">
          <div className="filter-pills" role="group" aria-label="Filtrar por status">
            {(
              [
                ['ALL', 'Todos'],
                ['APROVADO', 'Aprovados'],
                ['PENDENTE_ANALISE', 'Pendentes'],
                ['REPROVADO', 'Reprovados'],
              ] as const
            ).map(([key, label]) => (
              <button
                key={key}
                type="button"
                className={`filter-pill ${statusFilter === key ? 'active' : ''}`}
                onClick={() => setStatusFilter(key)}
              >
                {label}
              </button>
            ))}
          </div>

          <input
            type="search"
            className="form-control"
            style={{ maxWidth: '280px' }}
            placeholder="Buscar por arquivo ou titular..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            aria-label="Buscar comprovante"
          />
        </div>

        {loading ? (
          <LoadingState message="Consultando envios realizados..." />
        ) : (
          <DocumentTable
            documents={filteredDocs}
            onViewModal={(id) => void openDocumentModal(id)}
            onDelete={(id) => void handleDelete(id)}
            deletingId={deletingId}
          />
        )}
      </section>

      <DocumentDetailModal
        document={selectedDoc}
        loading={modalLoading}
        onClose={closeDocumentModal}
      />
    </div>
  );
};
