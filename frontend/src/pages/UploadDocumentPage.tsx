import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  ArrowLeft,
  ArrowRight,
  Droplets,
  Zap,
  Phone,
  CreditCard,
  CheckCircle2,
  Info,
  ExternalLink,
  RotateCcw,
} from 'lucide-react';
import { DocumentType, DocumentValidationResult } from '../types/document';
import {
  DOCUMENT_TYPE_LABELS,
  formatCpfInput,
  formatDateOnly,
} from '../utils/formatters';
import { documentService, extractApiErrorMessage } from '../services/api';
import { ProcessStepper } from '../components/ProcessStepper';
import { FileUploadZone } from '../components/FileUploadZone';
import { AlertBanner } from '../components/AlertBanner';
import { StatusBadge } from '../components/StatusBadge';

export const UploadDocumentPage: React.FC = () => {
  const [documentType, setDocumentType] = useState<DocumentType>('CONTA_DE_AGUA');
  const [holderName, setHolderName] = useState('');
  const [cpf, setCpf] = useState('');
  const [selectedFile, setSelectedFile] = useState<File | null>(null);

  const [submitting, setSubmitting] = useState(false);
  const [uploadProgress, setUploadProgress] = useState(0);
  const [currentStep, setCurrentStep] = useState<1 | 2 | 3 | 4>(1);
  const [error, setError] = useState<string | null>(null);
  const [result, setResult] = useState<DocumentValidationResult | null>(null);

  useEffect(() => {
    if (!submitting) return;
    const timer1 = window.setTimeout(() => setCurrentStep(2), 600);
    const timer2 = window.setTimeout(() => setCurrentStep(3), 1800);
    return () => {
      window.clearTimeout(timer1);
      window.clearTimeout(timer2);
    };
  }, [submitting]);

  const handleReset = () => {
    setSelectedFile(null);
    setError(null);
    setResult(null);
    setUploadProgress(0);
    setCurrentStep(1);
  };

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    setError(null);

    if (!holderName.trim() || holderName.trim().length < 3) {
      setError('Informe o nome completo do titular para conferência de titularidade.');
      return;
    }

    const cpfDigits = cpf.replace(/\D/g, '');
    if (cpfDigits.length > 0 && cpfDigits.length !== 11) {
      setError('Quando informado, o CPF do titular deve conter exatamente 11 dígitos.');
      return;
    }

    if (!selectedFile) {
      setError('Selecione um arquivo PDF, PNG ou JPG antes de iniciar o processamento.');
      return;
    }

    setSubmitting(true);
    setResult(null);
    setCurrentStep(2);
    setUploadProgress(0);

    try {
      const validationResult = await documentService.uploadDocument({
        file: selectedFile,
        documentType,
        holderName,
        cpf,
        onUploadProgress: setUploadProgress,
      });
      setResult(validationResult);
      setCurrentStep(4);
    } catch (err) {
      setError(extractApiErrorMessage(err));
      setCurrentStep(1);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div>
      <Link to="/dashboard" className="page-back-link">
        <ArrowLeft size={16} />
        <span>Voltar</span>
      </Link>

      <div className="page-header">
        <div>
          <h1 className="page-title">Enviar comprovante</h1>
          <p className="page-subtitle">
            Faça o upload do documento para iniciarmos a extração das informações.
          </p>
        </div>
      </div>

      <ProcessStepper currentStep={currentStep} />

      {error && <AlertBanner type="error" message={error} onClose={() => setError(null)} />}

      <div className="upload-layout-grid">
        <section className="card">
          <h2 className="card-title">Selecione o documento</h2>

          <form onSubmit={handleSubmit} noValidate>
            <div className="form-group">
              <label htmlFor="documentType" className="form-label">
                Tipo de documento<span className="form-required">*</span>
              </label>
              <select
                id="documentType"
                className="form-control"
                value={documentType}
                onChange={(e) => setDocumentType(e.target.value as DocumentType)}
                disabled={submitting}
              >
                {(Object.keys(DOCUMENT_TYPE_LABELS) as DocumentType[]).map((type) => (
                  <option key={type} value={type}>
                    {DOCUMENT_TYPE_LABELS[type]}
                  </option>
                ))}
              </select>
            </div>

            <div className="form-grid-two">
              <div className="form-group" style={{ marginBottom: 0 }}>
                <label htmlFor="holderName" className="form-label">
                  Nome completo do titular<span className="form-required">*</span>
                </label>
                <input
                  id="holderName"
                  type="text"
                  className="form-control"
                  placeholder="Ex: João da Silva"
                  value={holderName}
                  onChange={(e) => setHolderName(e.target.value)}
                  disabled={submitting}
                  required
                />
              </div>

              <div className="form-group" style={{ marginBottom: 0 }}>
                <label htmlFor="holderCpf" className="form-label">
                  CPF do titular (opcional)
                </label>
                <input
                  id="holderCpf"
                  type="text"
                  className="form-control"
                  placeholder="000.000.000-00"
                  value={cpf}
                  onChange={(e) => setCpf(formatCpfInput(e.target.value))}
                  disabled={submitting}
                  inputMode="numeric"
                />
              </div>
            </div>

            <div className="form-group" style={{ marginTop: '1.15rem', marginBottom: 0 }}>
              <span className="form-label">
                Arquivo do comprovante<span className="form-required">*</span>
              </span>
              <FileUploadZone
                selectedFile={selectedFile}
                onSelectFile={setSelectedFile}
                onValidationError={setError}
                disabled={submitting}
              />
            </div>

            {submitting && (
              <div
                style={{
                  marginTop: '1.1rem',
                  padding: '0.95rem 1rem',
                  borderRadius: 'var(--radius-sm)',
                  backgroundColor: 'var(--color-accent-soft)',
                  border: '1px solid var(--color-accent)',
                }}
                role="status"
              >
                <div
                  style={{
                    display: 'flex',
                    justifyContent: 'space-between',
                    fontSize: '0.84rem',
                    fontWeight: 600,
                    color: 'var(--color-primary)',
                    marginBottom: '0.45rem',
                  }}
                >
                  <span>
                    {currentStep === 2
                      ? 'Tratando imagem com OpenCV e extraindo texto via Tesseract OCR...'
                      : 'Interpretando texto com Google Gemini e validando regras de negócio...'}
                  </span>
                  <span>{uploadProgress > 0 ? `${uploadProgress}%` : ''}</span>
                </div>
                <div
                  style={{
                    height: '6px',
                    borderRadius: '999px',
                    backgroundColor: 'var(--color-border)',
                    overflow: 'hidden',
                  }}
                >
                  <div
                    style={{
                      height: '100%',
                      width: `${Math.max(uploadProgress, currentStep === 3 ? 82 : 45)}%`,
                      backgroundColor: 'var(--color-primary)',
                      transition: 'width 0.35s ease',
                    }}
                  />
                </div>
              </div>
            )}

            {result && (
              <div
                className={`result-hero status-${result.validationStatus}`}
                style={{ marginTop: '1.25rem', marginBottom: 0 }}
                data-testid="upload-result-summary"
              >
                <div style={{ flex: 1, minWidth: '240px' }}>
                  <div style={{ marginBottom: '0.5rem' }}>
                    <StatusBadge status={result.validationStatus} />
                  </div>
                  <p style={{ fontWeight: 600, fontSize: '0.94rem', marginBottom: '0.45rem' }}>
                    {result.summaryMessage}
                  </p>
                  <div style={{ fontSize: '0.82rem', color: 'var(--color-secondary)' }}>
                    Titular extraído:{' '}
                    <strong>{result.extractedData?.nomeTitular || 'Não identificado'}</strong> •
                    Emissão:{' '}
                    <strong>{formatDateOnly(result.extractedData?.dataEmissao)}</strong>
                  </div>
                </div>
                <div style={{ display: 'flex', gap: '0.6rem', alignItems: 'center' }}>
                  <button
                    type="button"
                    className="btn btn-secondary btn-sm"
                    onClick={handleReset}
                  >
                    <RotateCcw size={14} />
                    <span>Novo arquivo</span>
                  </button>
                  <Link to={`/results/${result.id}`} className="btn btn-primary btn-sm">
                    <span>Ver detalhes</span>
                    <ExternalLink size={14} />
                  </Link>
                </div>
              </div>
            )}

            <div className="upload-footer-actions">
              <button
                type="button"
                className="btn btn-secondary"
                onClick={handleReset}
                disabled={submitting}
              >
                Cancelar
              </button>
              <button
                type="submit"
                className="btn btn-primary"
                disabled={submitting || !selectedFile}
              >
                <span>{submitting ? 'Processando...' : 'Processar documento'}</span>
                {!submitting && <ArrowRight size={16} />}
              </button>
            </div>
          </form>
        </section>

        <aside className="upload-side-stack">
          <div className="card" style={{ padding: '1.25rem' }}>
            <h3 className="card-title" style={{ fontSize: '0.98rem', marginBottom: '0.85rem' }}>
              Exemplos de comprovantes
            </h3>
            <div className="example-list">
              <div className="example-item">
                <div className="example-icon">
                  <Droplets size={18} />
                </div>
                <div>
                  <div className="example-title">Conta de água</div>
                  <div className="example-desc">Ex: SABESP, COMPESA, COPASA</div>
                </div>
              </div>

              <div className="example-item">
                <div className="example-icon">
                  <Zap size={18} />
                </div>
                <div>
                  <div className="example-title">Conta de energia</div>
                  <div className="example-desc">Ex: ENEL, NEOENERGIA, CEMIG</div>
                </div>
              </div>

              <div className="example-item">
                <div className="example-icon">
                  <Phone size={18} />
                </div>
                <div>
                  <div className="example-title">Conta de telefone</div>
                  <div className="example-desc">Ex: Vivo, Claro, TIM</div>
                </div>
              </div>

              <div className="example-item">
                <div className="example-icon">
                  <CreditCard size={18} />
                </div>
                <div>
                  <div className="example-title">Outros aceitos</div>
                  <div className="example-desc">Fatura de cartão, contrato de aluguel</div>
                </div>
              </div>
            </div>
          </div>

          <div className="card" style={{ padding: '1.25rem' }}>
            <h3 className="card-title" style={{ fontSize: '0.98rem', marginBottom: '0.85rem' }}>
              Dicas para um melhor resultado
            </h3>
            <ul className="tips-list">
              <li>
                <CheckCircle2 size={16} />
                <span>Imagem nítida e sem cortes</span>
              </li>
              <li>
                <CheckCircle2 size={16} />
                <span>Documento inteiro e legível</span>
              </li>
              <li>
                <CheckCircle2 size={16} />
                <span>Sem reflexos ou sombras</span>
              </li>
              <li>
                <CheckCircle2 size={16} />
                <span>Texto visível e alinhado</span>
              </li>
              <li>
                <CheckCircle2 size={16} />
                <span>Evite arquivos muito grandes (máx. 10MB)</span>
              </li>
            </ul>
          </div>

          <div className="important-callout">
            <div className="important-callout-header">
              <Info size={16} />
              <span>Importante</span>
            </div>
            <p>
              Utilize um documento recente (emitido nos últimos 90 dias) e em nome do titular
              informado. Documentos ilegíveis ou fora da validade serão reprovados pelas regras de
              negócio.
            </p>
          </div>
        </aside>
      </div>
    </div>
  );
};
