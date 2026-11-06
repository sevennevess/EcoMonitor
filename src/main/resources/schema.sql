CREATE TABLE IF NOT EXISTS sensores (
    id TEXT PRIMARY KEY,
    nome TEXT NOT NULL,
    tipos TEXT NOT NULL,
    status TEXT NOT NULL,
    data_cadastro TEXT NOT NULL,
    ultima_comunicacao TEXT,
    token_hash TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS medicoes (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    sensor_id TEXT NOT NULL,
    tipo_medicao TEXT NOT NULL,
    valor REAL NOT NULL,
    unidade TEXT NOT NULL,
    data_hora TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_medicoes_busca
    ON medicoes (sensor_id, tipo_medicao, data_hora);

CREATE TABLE IF NOT EXISTS configuracoes (
    tipo_medicao TEXT PRIMARY KEY,
    operador TEXT NOT NULL,
    limite REAL NOT NULL
);

INSERT OR IGNORE INTO configuracoes (tipo_medicao, operador, limite) VALUES ('TEMPERATURA', 'MAIOR', 40);
INSERT OR IGNORE INTO configuracoes (tipo_medicao, operador, limite) VALUES ('UMIDADE', 'MENOR', 30);
INSERT OR IGNORE INTO configuracoes (tipo_medicao, operador, limite) VALUES ('QUALIDADE_AR', 'MAIOR', 80);

CREATE TABLE IF NOT EXISTS alertas (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    sensor_id TEXT NOT NULL,
    tipo_medicao TEXT NOT NULL,
    valor REAL NOT NULL,
    limite REAL NOT NULL,
    operador TEXT NOT NULL,
    mensagem TEXT NOT NULL,
    data_hora TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS usuarios (
    login TEXT PRIMARY KEY,
    senha_hash TEXT NOT NULL,
    papel TEXT NOT NULL,
    criado_em TEXT NOT NULL
);
