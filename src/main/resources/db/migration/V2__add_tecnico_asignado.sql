ALTER TABLE documents ADD COLUMN IF NOT EXISTS tecnico_asignado VARCHAR(255);

CREATE INDEX IF NOT EXISTS idx_documents_tecnico_asignado ON documents (tecnico_asignado);
