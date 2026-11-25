import React from 'react';
import { AlertCircle, CheckCircle2, AlertTriangle, Info, X } from 'lucide-react';

interface AlertBannerProps {
  type: 'error' | 'success' | 'warning' | 'info';
  message: string;
  onClose?: () => void;
}

export const AlertBanner: React.FC<AlertBannerProps> = ({ type, message, onClose }) => {
  const Icon =
    type === 'error'
      ? AlertCircle
      : type === 'success'
        ? CheckCircle2
        : type === 'warning'
          ? AlertTriangle
          : Info;

  return (
    <div className={`alert-banner alert-${type}`} role="alert">
      <div className="alert-content">
        <Icon size={18} style={{ flexShrink: 0, marginTop: '2px' }} />
        <span>{message}</span>
      </div>
      {onClose && (
        <button type="button" className="icon-btn" onClick={onClose} aria-label="Fechar aviso">
          <X size={16} />
        </button>
      )}
    </div>
  );
};
