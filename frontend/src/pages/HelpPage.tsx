import React from 'react';
import { Link } from 'react-router-dom';
import {
  CheckCircle2,
  ShieldCheck,
  FileCheck2,
  Cpu,
  Clock,
  FileUp,
} from 'lucide-react';

export const HelpPage: React.FC = () => {
  return (
    <div>
      <div className="page-header">
        <div>
          <h1 className="page-title">Central de Ajuda e Orientações</h1>
          <p className="page-subtitle">
            Entenda quais comprovantes são aceitos, como funciona o processamento inteligente e os
            critérios de aprovação.
          </p>
        </div>
        <Link to="/upload" className="btn btn-primary">
          <FileUp size={16} />
          <span>Enviar comprovante</span>
        </Link>
      </div>

      <div className="details-grid" style={{ marginBottom: '1.5rem' }}>
        <section className="card">
          <h2 className="card-title" style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <FileCheck2 size={20} color="var(--color-primary)" />
            <span>Documentos Aceitos</span>
          </h2>
          <ul className="tips-list">
            <li>
              <CheckCircle2 size={16} />
              <span>Contas de consumo de água e saneamento (SABESP, COMPESA, COPASA, etc.)</span>
            </li>
            <li>
              <CheckCircle2 size={16} />
              <span>Contas de energia elétrica (ENEL, NEOENERGIA, CEMIG, CPFL, etc.)</span>
            </li>
            <li>
              <CheckCircle2 size={16} />
              <span>Faturas de telefonia fixa, móvel ou internet residencial (Vivo, Claro, TIM)</span>
            </li>
            <li>
              <CheckCircle2 size={16} />
              <span>Faturas de cartão de crédito ou demonstrativos bancários com endereço</span>
            </li>
            <li>
              <CheckCircle2 size={16} />
              <span>Formatos suportados: PDF, PNG e JPG/JPEG com tamanho máximo de 10 MB</span>
            </li>
          </ul>
        </section>

        <section className="card">
          <h2 className="card-title" style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <Clock size={20} color="var(--color-primary)" />
            <span>Regras para Aprovação Automática</span>
          </h2>
          <ul className="tips-list">
            <li>
              <CheckCircle2 size={16} />
              <span>
                <strong>Legibilidade:</strong> O documento deve possuir texto nítido extraível via
                OCR.
              </span>
            </li>
            <li>
              <CheckCircle2 size={16} />
              <span>
                <strong>Titularidade:</strong> O nome do titular identificado deve corresponder ao
                nome informado no envio.
              </span>
            </li>
            <li>
              <CheckCircle2 size={16} />
              <span>
                <strong>Endereço completo:</strong> Logradouro e localidade precisam estar visíveis
                no comprovante.
              </span>
            </li>
            <li>
              <CheckCircle2 size={16} />
              <span>
                <strong>Prazo de emissão:</strong> Emitido nos últimos 90 dias em relação à data de
                validação.
              </span>
            </li>
            <li>
              <CheckCircle2 size={16} />
              <span>
                <strong>Análise manual:</strong> Documentos legíveis com campos ausentes recebem
                status <em>Pendente de Análise</em>.
              </span>
            </li>
          </ul>
        </section>
      </div>

      <div className="details-grid">
        <section className="card">
          <h2 className="card-title" style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <Cpu size={20} color="var(--color-primary)" />
            <span>Etapas do Pipeline Inteligente</span>
          </h2>
          <p style={{ fontSize: '0.88rem', color: 'var(--color-dark)', marginBottom: '0.75rem' }}>
            Cada documento enviado percorre um fluxo auditável em seis etapas no backend:
          </p>
          <ol
            style={{
              paddingLeft: '1.25rem',
              fontSize: '0.86rem',
              color: 'var(--color-dark)',
              display: 'flex',
              flexDirection: 'column',
              gap: '0.45rem',
            }}
          >
            <li>Validação de segurança do arquivo (extensão, MIME type e limite de 10 MB).</li>
            <li>Pré-processamento com OpenCV (escala de cinza, redução de ruído, contraste CLAHE e rotação).</li>
            <li>Reconhecimento óptico de caracteres com Tesseract OCR (português e inglês).</li>
            <li>Estruturação semântica em JSON com Google Gemini seguindo schema estrito.</li>
            <li>Avaliação determinística pelas regras de negócio em Java / Spring Boot.</li>
            <li>Persistência em PostgreSQL e apresentação imediata do parecer.</li>
          </ol>
        </section>

        <section className="card">
          <h2 className="card-title" style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <ShieldCheck size={20} color="var(--color-primary)" />
            <span>Privacidade e Segurança (LGPD)</span>
          </h2>
          <p style={{ fontSize: '0.88rem', color: 'var(--color-dark)', lineHeight: 1.6 }}>
            Os comprovantes de residência contêm dados pessoais protegidos. Por isso, o sistema não
            expõe arquivos publicamente, mascara CPFs na visualização e impede o registro de dados
            sensíveis (CPF ou endereço completo) nos logs do servidor. Você também pode excluir
            qualquer envio a qualquer momento na aba <strong>Meus envios</strong>.
          </p>
        </section>
      </div>
    </div>
  );
};
