import React from 'react';
import { LucideIcon } from 'lucide-react';

interface StatCardProps {
  label: string;
  value: number;
  helper?: string;
  icon: LucideIcon;
  variant?: 'primary' | 'success' | 'error' | 'warning';
}

export const StatCard: React.FC<StatCardProps> = ({
  label,
  value,
  helper,
  icon: Icon,
  variant = 'primary',
}) => {
  return (
    <article className="stat-card">
      <div>
        <div className="stat-label">{label}</div>
        <div className="stat-value" data-testid={`stat-${variant}`}>
          {value}
        </div>
        {helper && <div className="stat-helper">{helper}</div>}
      </div>
      <div className={`stat-icon-box stat-icon-${variant}`} aria-hidden="true">
        <Icon size={20} />
      </div>
    </article>
  );
};
