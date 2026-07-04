-- =====================================================================
-- V11 — Tabla email_outbox (change funcional init-notifications, capability notifications).
-- Almacen de reintento de las notificaciones por email cuyo envio SMTP fallo.
--
-- NOTA DE RECONCILIACION DE DOCS: docs/data-model.md NO define esta tabla (task 2.7
-- "[verificar con data-model]"); su esquema se disena aqui de forma sensata y queda
-- pendiente de reflejar en docs/data-model.md por el orquestador.
--
-- Semantica:
--   * Una fila se crea SOLO cuando el envio inmediato (AFTER_COMMIT del evento origen)
--     falla: la operacion funcional ya esta confirmada y el email es best-effort. La
--     fila guarda el mensaje YA RENDERIZADO (recipient, subject, body_html) para que el
--     job de reintento reenvie sin re-resolver destinatarios ni re-renderizar plantillas.
--   * status: PENDING (pendiente de reintento) -> SENT (enviado con exito, terminal) o
--     FAILED (agotada la politica de max reintentos, terminal). El job de reintento SOLO
--     lee PENDING, por lo que un SENT nunca se reenvia (idempotencia).
--   * attempts cuenta los intentos realizados (el inmediato fallido = 1). Al alcanzar el
--     maximo configurado (parking.notifications.max-attempts) la fila pasa a FAILED y deja
--     de reintentarse (evita el bucle infinito ante un destinatario sin email valido).
--   * No hay FK a employees: el destinatario se persiste como email (string) porque el
--     reenvio necesita la direccion literal y el destinatario puede resolverse en el
--     momento del envio, no despues.
-- =====================================================================

CREATE TABLE dbo.email_outbox (
    id               BIGINT IDENTITY(1,1) NOT NULL,
    recipient        NVARCHAR(255) NOT NULL,
    subject          NVARCHAR(255) NOT NULL,
    body_html        NVARCHAR(MAX) NOT NULL,
    status           VARCHAR(20) NOT NULL,
    attempts         INT NOT NULL CONSTRAINT DF_email_outbox_attempts DEFAULT 0,
    last_error       NVARCHAR(500) NULL,
    created_at       DATETIME2(3) NOT NULL,
    last_attempt_at  DATETIME2(3) NULL,
    sent_at          DATETIME2(3) NULL,
    CONSTRAINT PK_email_outbox PRIMARY KEY (id),
    CONSTRAINT CK_email_outbox_status CHECK (status IN ('PENDING', 'SENT', 'FAILED'))
);
GO

-- El job de reintento filtra por status = 'PENDING'; el indice sirve ese barrido.
CREATE INDEX IX_email_outbox_status ON dbo.email_outbox(status);
GO
