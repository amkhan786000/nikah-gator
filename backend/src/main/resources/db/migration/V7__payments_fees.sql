-- Manual fee/donation recording (no card processor yet; provider/provider_ref stay for a later Stripe integration).
-- Payments are never deleted: a mistaken one is VOID, a returned one REFUNDED, both with who/when/why.

ALTER TABLE masjids
    ADD COLUMN suggested_donation_cents INT NOT NULL DEFAULT 0 AFTER nikah_fee_cents;

ALTER TABLE payments
    ADD COLUMN masjid_id         BIGINT       NULL AFTER request_id,
    ADD COLUMN method            VARCHAR(20)  NULL,   -- CASH, CHECK, ZELLE, CARD, OTHER
    ADD COLUMN paid_at           DATE         NULL,   -- date the money was received
    ADD COLUMN payer_name        VARCHAR(200) NULL,
    ADD COLUMN note              VARCHAR(500) NULL,
    ADD COLUMN receipt_number    VARCHAR(40)  NULL,
    ADD COLUMN received_by       BIGINT       NULL,
    ADD COLUMN status_reason     VARCHAR(500) NULL,   -- why it was voided / refunded
    ADD COLUMN status_changed_by BIGINT       NULL,
    ADD COLUMN status_changed_at TIMESTAMP    NULL,
    MODIFY COLUMN status VARCHAR(20) NOT NULL DEFAULT 'PAID'; -- PAID, VOID, REFUNDED (card processor later: PENDING, FAILED)

UPDATE payments p JOIN nikah_requests r ON r.id = p.request_id SET p.masjid_id = r.masjid_id;

ALTER TABLE payments
    MODIFY COLUMN masjid_id BIGINT NOT NULL,
    ADD CONSTRAINT fk_pay_masjid FOREIGN KEY (masjid_id) REFERENCES masjids (id),
    ADD UNIQUE INDEX uq_pay_receipt (receipt_number),
    ADD INDEX idx_pay_masjid (masjid_id);

-- Per-nikah fee: snapshot of the masjid default taken at the first payment/waiver, and the waiver.
CREATE TABLE nikah_fees (
    request_id    BIGINT PRIMARY KEY,
    fee_cents     INT          NOT NULL,
    waived        BOOLEAN      NOT NULL DEFAULT FALSE,
    waived_reason VARCHAR(500) NULL,
    updated_by    BIGINT       NULL,
    updated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_fee_req FOREIGN KEY (request_id) REFERENCES nikah_requests (id)
);

-- Gap-free per masjid and year receipt numbers: NGR-<masjidId>-<year>-<seq>.
CREATE TABLE receipt_counters (
    masjid_id BIGINT NOT NULL,
    year      INT    NOT NULL,
    last_seq  INT    NOT NULL,
    PRIMARY KEY (masjid_id, year)
);
