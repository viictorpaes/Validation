import React, { useRef, useState } from 'react';
import { UploadCloud, FileText, CheckCircle2, X } from 'lucide-react';
import { formatFileSize, validateClientFile } from '../utils/formatters';

interface FileUploadZoneProps {
  selectedFile: File | null;
  onSelectFile: (file: File | null) => void;
  onValidationError: (error: string | null) => void;
  disabled?: boolean;
}

export const FileUploadZone: React.FC<FileUploadZoneProps> = ({
  selectedFile,
  onSelectFile,
  onValidationError,
  disabled = false,
}) => {
  const inputRef = useRef<HTMLInputElement | null>(null);
  const [dragging, setDragging] = useState(false);

  const handleCandidateFile = (file: File | undefined) => {
    if (!file) return;
    const error = validateClientFile(file);
    if (error) {
      onValidationError(error);
      onSelectFile(null);
      return;
    }
    onValidationError(null);
    onSelectFile(file);
  };

  const handleDrop = (event: React.DragEvent<HTMLDivElement>) => {
    event.preventDefault();
    setDragging(false);
    if (disabled) return;
    const file = event.dataTransfer.files?.[0];
    handleCandidateFile(file);
  };

  const handleInputChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    handleCandidateFile(file);
    if (inputRef.current) {
      inputRef.current.value = '';
    }
  };

  return (
    <div>
      <div
        className={`dropzone ${dragging ? 'dragging' : ''}`}
        onDragOver={(e) => {
          e.preventDefault();
          if (!disabled) setDragging(true);
        }}
        onDragLeave={() => setDragging(false)}
        onDrop={handleDrop}
        onClick={() => !disabled && inputRef.current?.click()}
        role="button"
        tabIndex={0}
        onKeyDown={(e) => {
          if (!disabled && (e.key === 'Enter' || e.key === ' ')) {
            e.preventDefault();
            inputRef.current?.click();
          }
        }}
        aria-label="Área de upload de comprovante"
      >
        <input
          ref={inputRef}
          type="file"
          accept=".pdf,.png,.jpg,.jpeg,application/pdf,image/png,image/jpeg"
          onChange={handleInputChange}
          disabled={disabled}
          style={{ display: 'none' }}
          data-testid="file-input"
        />
        <div className="dropzone-icon" aria-hidden="true">
          <UploadCloud size={28} />
        </div>
        <div className="dropzone-title">Clique para selecionar ou arraste o arquivo aqui</div>
        <div className="dropzone-hint">Formatos: PDF, PNG, JPG (máx. 10MB)</div>
        <button
          type="button"
          className="btn btn-primary"
          disabled={disabled}
          onClick={(e) => {
            e.stopPropagation();
            inputRef.current?.click();
          }}
        >
          Selecionar arquivo
        </button>
      </div>

      {selectedFile && (
        <div className="uploaded-file-bar" data-testid="selected-file-bar">
          <div className="uploaded-file-meta">
            <div className="uploaded-file-icon" aria-hidden="true">
              <FileText size={20} />
            </div>
            <div style={{ minWidth: 0 }}>
              <div className="uploaded-file-name" title={selectedFile.name}>
                {selectedFile.name}
              </div>
              <div className="uploaded-file-size">{formatFileSize(selectedFile.size)}</div>
            </div>
          </div>

          <div className="uploaded-file-actions">
            <span className="loaded-pill">
              <CheckCircle2 size={16} />
              Arquivo carregado
            </span>
            <button
              type="button"
              className="icon-btn"
              disabled={disabled}
              onClick={() => onSelectFile(null)}
              aria-label="Remover arquivo selecionado"
            >
              <X size={18} />
            </button>
          </div>
        </div>
      )}
    </div>
  );
};
