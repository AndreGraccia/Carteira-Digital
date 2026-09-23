-- How often each kind of question reaches "Pergunte ao Wall.ia". Only the detected intent and a
-- daily count are stored: never the question text, never the user. Decides when the deterministic
-- parser needs an LLM classifier (share of UNKNOWN), see docs/adr/0002.
CREATE TABLE ask_metric
(
    day    DATE        NOT NULL,
    intent VARCHAR(20) NOT NULL,
    count  INTEGER     NOT NULL DEFAULT 0,
    CONSTRAINT pk_ask_metric PRIMARY KEY (day, intent),
    CONSTRAINT ask_metric_count_check CHECK (count >= 0)
);

COMMENT ON TABLE ask_metric IS
    'Contagem diaria de intencoes do Pergunte ao Wall.ia. Sem texto e sem usuario.';
