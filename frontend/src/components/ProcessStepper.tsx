import React from 'react';
import { Check } from 'lucide-react';

interface ProcessStepperProps {
  currentStep: 1 | 2 | 3 | 4;
}

const steps = [
  { step: 1, title: 'Upload', desc: 'Envio do documento' },
  { step: 2, title: 'Processamento', desc: 'OpenCV e OCR' },
  { step: 3, title: 'Validação', desc: 'IA e Regras de negócio' },
  { step: 4, title: 'Resultado', desc: 'Aprovado ou Reprovado' },
] as const;

export const ProcessStepper: React.FC<ProcessStepperProps> = ({ currentStep }) => {
  return (
    <div className="stepper" aria-label="Progresso da validação">
      {steps.map(({ step, title, desc }) => {
        const isCompleted = step < currentStep;
        const isActive = step === currentStep;
        const cls = isCompleted
          ? 'stepper-item completed'
          : isActive
            ? 'stepper-item active'
            : 'stepper-item';

        return (
          <div key={step} className={cls}>
            <div className="stepper-circle">
              {isCompleted ? <Check size={15} /> : step}
            </div>
            <div>
              <div className="stepper-label-title">{title}</div>
              <div className="stepper-label-desc">{desc}</div>
            </div>
          </div>
        );
      })}
    </div>
  );
};
