-- Checklist: keep the order of items and tell defaults from custom items the imam added.
ALTER TABLE checklist_items
    ADD COLUMN sort_order INT     NOT NULL DEFAULT 0,
    ADD COLUMN custom     BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX idx_check_req ON checklist_items (request_id, sort_order);
CREATE INDEX idx_doc_req ON documents (request_id);
