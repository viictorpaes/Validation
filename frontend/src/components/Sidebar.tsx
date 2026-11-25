import React from 'react';
import { NavLink } from 'react-router-dom';
import {
  LayoutDashboard,
  FileUp,
  FolderCheck,
  ClipboardCheck,
  HelpCircle,
  ShieldCheck,
} from 'lucide-react';

interface SidebarProps {
  isOpen: boolean;
  onClose: () => void;
}

const navItems = [
  { to: '/dashboard', label: 'Dashboard', icon: LayoutDashboard },
  { to: '/upload', label: 'Enviar comprovante', icon: FileUp },
  { to: '/submissions', label: 'Meus envios', icon: FolderCheck },
  { to: '/results', label: 'Resultados', icon: ClipboardCheck },
  { to: '/help', label: 'Ajuda', icon: HelpCircle },
];

export const Sidebar: React.FC<SidebarProps> = ({ isOpen, onClose }) => {
  return (
    <>
      <div
        className={`sidebar-backdrop ${isOpen ? 'open' : ''}`}
        onClick={onClose}
        aria-hidden="true"
      />
      <aside className={`sidebar ${isOpen ? 'open' : ''}`} aria-label="Menu lateral">
        <div className="sidebar-brand">
          <div className="sidebar-logo" aria-hidden="true">
            <ShieldCheck size={20} />
          </div>
          <div className="sidebar-brand-text">
            <span className="sidebar-brand-title">ValidaDoc</span>
            <span className="sidebar-brand-sub">Validação IA &amp; OCR</span>
          </div>
        </div>

        <nav className="sidebar-nav">
          {navItems.map(({ to, label, icon: Icon }) => (
            <NavLink
              key={to}
              to={to}
              onClick={onClose}
              className={({ isActive }) => `sidebar-link ${isActive ? 'active' : ''}`}
            >
              <Icon size={18} />
              <span>{label}</span>
            </NavLink>
          ))}
        </nav>

        <div className="sidebar-footer">
          <div className="sidebar-footer-avatar" aria-hidden="true">
            LG
          </div>
          <div className="sidebar-footer-info">
            <div className="sidebar-footer-name">Conformidade LGPD</div>
            <div className="sidebar-footer-role">Dados protegidos</div>
          </div>
        </div>
      </aside>
    </>
  );
};
