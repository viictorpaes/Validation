CREATE TABLE documents (
    id UUID PRIMARY KEY,
    original_file_name VARCHAR(255) NOT NULL,
    stored_file_path VARCHAR(512),
    content_type VARCHAR(100) NOT NULL,
    file_size_bytes BIGINT NOT NULL,
    informed_holder_name VARCHAR(200) NOT NULL,
    informed_cpf VARCHAR(20),
    document_type VARCHAR(40) NOT NULL,
    processing_status VARCHAR(40) NOT NULL,
    validation_status VARCHAR(40) NOT NULL,
    ocr_text TEXT,
    ocr_char_count INTEGER NOT NULL DEFAULT 0,
    extracted_json TEXT,
    ai_source VARCHAR(60),
    summary_message VARCHAR(500),
    reasons_json TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE document_rule_results (
    id UUID PRIMARY KEY,
    document_id UUID NOT NULL,
    rule_order INTEGER NOT NULL,
    rule_code VARCHAR(80) NOT NULL,
    rule_name VARCHAR(160) NOT NULL,
    outcome VARCHAR(30) NOT NULL,
    passed BOOLEAN NOT NULL,
    message VARCHAR(500) NOT NULL,
    CONSTRAINT fk_document_rule_results_document
        FOREIGN KEY (document_id) REFERENCES documents (id) ON DELETE CASCADE
);

CREATE INDEX idx_documents_created_at ON documents (created_at DESC);
CREATE INDEX idx_documents_validation_status ON documents (validation_status);
CREATE INDEX idx_document_rule_results_doc_id ON document_rule_results (document_id);
