import React from 'react';
import { CheckCircle2, XCircle, Clock, Cpu } from 'lucide-react';
import { ProcessingStatus, ValidationStatus } from '../types/document';
import { PROCESSING_STATUS_LABELS, VALIDATION_STATUS_LABELS } from '../utils/formatters';

interface ValidationBadgeProps {
  status: ValidationStatus;
}

export const StatusBadge: React.FC<ValidationBadgeProps> = ({ status }) => {
  if (status === 'APROVADO') {
    return (
      <span className="badge badge-approved">
        <CheckCircle2 size={14} />
        {VALIDATION_STATUS_LABELS[status]}
      </span>
    );
  }
  if (status === 'REPROVADO') {
    return (
      <span className="badge badge-rejected">
        <XCircle size={14} />
        {VALIDATION_STATUS_LABELS[status]}
      </span>
    );
  }
  return (
    <span className="badge badge-pending">
      <Clock size={14} />
      {VALIDATION_STATUS_LABELS[status]}
    </span>
  );
};

interface ProcessingBadgeProps {
  status: ProcessingStatus;
}

export const ProcessingBadge: React.FC<ProcessingBadgeProps> = ({ status }) => {
  const cls =
    status === 'ERRO'
      ? 'badge badge-rejected'
      : status === 'CONCLUIDO'
        ? 'badge badge-processing'
        : 'badge badge-pending';

  return (
    <span className={cls}>
      <Cpu size={13} />
      {PROCESSING_STATUS_LABELS[status] || status}
    </span>
  );
};
