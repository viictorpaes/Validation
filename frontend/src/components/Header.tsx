import React from 'react';
import { Link } from 'react-router-dom';
import { Menu, FilePlus2, Sparkles } from 'lucide-react';

interface HeaderProps {
  onToggleSidebar: () => void;
}

export const Header: React.FC<HeaderProps> = ({ onToggleSidebar }) => {
  return (
    <header className="top-header">
      <div className="header-left">
        <button
          type="button"
          className="mobile-menu-btn"
          onClick={onToggleSidebar}
          aria-label="Abrir menu de navegação"
        >
          <Menu size={20} />
        </button>
        <div>
          <div className="header-program-title">Sistema de Validação Documental</div>
          <div className="header-program-subtitle">
            OpenCV • Tesseract OCR • Google Gemini • Spring Boot
          </div>
        </div>
      </div>

      <div className="header-right">
        <span className="header-badge-pill">
          <Sparkles size={14} />
          <span>Análise Inteligente</span>
        </span>
        <Link to="/upload" className="btn btn-primary btn-sm">
          <FilePlus2 size={16} />
          <span>Novo envio</span>
        </Link>
        <div className="header-avatar" title="Validador Residencial">
          VR
        </div>
      </div>
    </header>
  );
};
