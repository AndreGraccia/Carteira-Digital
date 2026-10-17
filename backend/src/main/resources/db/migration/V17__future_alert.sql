-- Opt-in e-mail alert: a daily job recalculates the projection and writes once per new negative window.
-- last_window_start remembers which window was already announced, so the user is not written to every day.
CREATE TABLE future_alert
(
    user_id           BIGINT                   NOT NULL,
    enabled           BOOLEAN                  NOT NULL DEFAULT FALSE,
    last_window_start DATE,
    last_sent_at      TIMESTAMP WITH TIME ZONE,
    updated_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_future_alert PRIMARY KEY (user_id),
    CONSTRAINT fk_future_alert_user FOREIGN KEY (user_id) REFERENCES public."user" (id)
);

COMMENT ON TABLE future_alert IS
    'Aviso por e-mail de saldo negativo previsto. So envia com enabled = true, escolhido pelo usuario.';
