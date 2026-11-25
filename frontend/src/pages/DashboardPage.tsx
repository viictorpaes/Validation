import React, { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  Files,
  CheckCircle2,
  XCircle,
  Clock,
  FileUp,
  RefreshCw,
} from 'lucide-react';
import { DashboardStats } from '../types/document';
import { documentService, extractApiErrorMessage } from '../services/api';
import { StatCard } from '../components/StatCard';
import { DocumentTable } from '../components/DocumentTable';
import { LoadingState } from '../components/LoadingState';
import { AlertBanner } from '../components/AlertBanner';
import { DocumentDetailModal } from '../components/DocumentDetailModal';
import { useDocumentModal } from '../hooks/useDocumentModal';

export const DashboardPage: React.FC = () => {
  const [stats, setStats] = useState<DashboardStats | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const {
    selectedDoc,
    modalLoading,
    modalError,
    setModalError,
    openDocumentModal,
    closeDocumentModal,
  } = useDocumentModal();

  const fetchDashboard = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await documentService.getDashboardStats();
      setStats(data);
    } catch (err) {
      setError(extractApiErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void fetchDashboard();
  }, [fetchDashboard]);

  const approvalRate =
    stats && stats.totalDocuments > 0
      ? Math.round((stats.approvedDocuments / stats.totalDocuments) * 100)
      : 0;

  return (
    <div>
      <div className="page-header">
        <div>
          <h1 className="page-title">Dashboard de Validações</h1>
          <p className="page-subtitle">
            Visão consolidada dos comprovantes de residência processados via OCR e IA.
          </p>
        </div>
        <div style={{ display: 'flex', gap: '0.65rem' }}>
          <button
            type="button"
            className="btn btn-secondary"
            onClick={() => void fetchDashboard()}
            disabled={loading}
          >
            <RefreshCw size={16} />
            <span>Atualizar</span>
          </button>
          <Link to="/upload" className="btn btn-primary">
            <FileUp size={16} />
            <span>Enviar comprovante</span>
          </Link>
        </div>
      </div>

      {error && <AlertBanner type="error" message={error} onClose={() => setError(null)} />}
      {modalError && (
        <AlertBanner type="error" message={modalError} onClose={() => setModalError(null)} />
      )}

      {loading && !stats ? (
        <LoadingState message="Carregando indicadores do dashboard..." />
      ) : (
        <>
          <section className="stats-grid" aria-label="Indicadores principais">
            <StatCard
              label="Total de documentos"
              value={stats?.totalDocuments ?? 0}
              helper="Comprovantes registrados"
              icon={Files}
              variant="primary"
            />
            <StatCard
              label="Aprovados"
              value={stats?.approvedDocuments ?? 0}
              helper={
                stats && stats.totalDocuments > 0
                  ? `${approvalRate}% de aprovação automática`
                  : 'Regras atendidas'
              }
              icon={CheckCircle2}
              variant="success"
            />
            <StatCard
              label="Reprovados"
              value={stats?.rejectedDocuments ?? 0}
              helper="Inconsistência nas regras"
              icon={XCircle}
              variant="error"
            />
            <StatCard
              label="Pendentes de análise"
              value={stats?.pendingDocuments ?? 0}
              helper="Dados parciais ou inconclusivos"
              icon={Clock}
              variant="warning"
            />
          </section>

          <section className="card">
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
              <h2 className="card-title" style={{ marginBottom: 0 }}>
                Últimos documentos processados
              </h2>
              <Link to="/submissions" style={{ fontSize: '0.86rem', fontWeight: 600 }}>
                Ver todos os envios →
              </Link>
            </div>

            <DocumentTable
              documents={stats?.recentDocuments ?? []}
              onViewModal={(id) => void openDocumentModal(id)}
            />
          </section>
        </>
      )}

      <DocumentDetailModal
        document={selectedDoc}
        loading={modalLoading}
        onClose={closeDocumentModal}
      />
    </div>
  );
};
