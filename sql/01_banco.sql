CREATE DATABASE IF NOT EXISTS instituto_atendimentos
  CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE instituto_atendimentos;

CREATE TABLE IF NOT EXISTS pessoa_atendida (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  nome VARCHAR(120) NOT NULL,
  data_nascimento DATE NULL,
  contato VARCHAR(120) NULL
);

CREATE TABLE IF NOT EXISTS atendimento (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  pessoa_id BIGINT NOT NULL,
  tipo VARCHAR(80) NOT NULL,
  chegada DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  inicio DATETIME(6) NULL,
  fim DATETIME(6) NULL,
  estado VARCHAR(20) NOT NULL DEFAULT 'AGUARDANDO',
  CONSTRAINT fk_atendimento_pessoa FOREIGN KEY (pessoa_id)
    REFERENCES pessoa_atendida(id),
  CONSTRAINT ck_estado CHECK (estado IN ('AGUARDANDO', 'EM_ATENDIMENTO', 'CONCLUIDO')),
  CONSTRAINT ck_tempos CHECK ((inicio IS NULL OR inicio >= chegada)
    AND (fim IS NULL OR (inicio IS NOT NULL AND fim >= inicio))),
  INDEX idx_fila (estado, chegada, id),
  INDEX idx_pessoa (pessoa_id)
);
