import { render, screen, fireEvent } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { Files } from 'lucide-react';
import { StatusBadge } from '../components/StatusBadge';
import { StatCard } from '../components/StatCard';
import { ProcessStepper } from '../components/ProcessStepper';
import { FileUploadZone } from '../components/FileUploadZone';

describe('Componentes principais e validação de arquivo no upload', () => {
  it('renderiza badges de status de validação corretamente', () => {
    const { rerender } = render(<StatusBadge status="APROVADO" />);
    expect(screen.getByText('Aprovado')).toBeInTheDocument();

    rerender(<StatusBadge status="REPROVADO" />);
    expect(screen.getByText('Reprovado')).toBeInTheDocument();

    rerender(<StatusBadge status="PENDENTE_ANALISE" />);
    expect(screen.getByText('Pendente de análise')).toBeInTheDocument();
  });

  it('renderiza card de indicador e stepper de etapas', () => {
    render(
      <>
        <StatCard label="Total de documentos" value={12} icon={Files} variant="primary" />
        <ProcessStepper currentStep={2} />
      </>,
    );
    expect(screen.getByText('Total de documentos')).toBeInTheDocument();
    expect(screen.getByTestId('stat-primary')).toHaveTextContent('12');
    expect(screen.getByText('Processamento')).toBeInTheDocument();
  });

  it('aceita arquivo PDF válido e rejeita extensão inválida no FileUploadZone', () => {
    const onSelectFile = vi.fn();
    const onValidationError = vi.fn();

    render(
      <FileUploadZone
        selectedFile={null}
        onSelectFile={onSelectFile}
        onValidationError={onValidationError}
      />,
    );

    const input = screen.getByTestId('file-input') as HTMLInputElement;

    const invalidFile = new File(['abc'], 'virus.exe', { type: 'application/octet-stream' });
    fireEvent.change(input, { target: { files: [invalidFile] } });
    expect(onValidationError).toHaveBeenCalledWith(
      expect.stringContaining('Formato de arquivo inválido'),
    );
    expect(onSelectFile).toHaveBeenCalledWith(null);

    const validPdf = new File(['%PDF-1.4 sample'], 'comprovante.pdf', {
      type: 'application/pdf',
    });
    fireEvent.change(input, { target: { files: [validPdf] } });
    expect(onValidationError).toHaveBeenLastCalledWith(null);
    expect(onSelectFile).toHaveBeenLastCalledWith(validPdf);
  });
});
