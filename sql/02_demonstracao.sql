USE instituto_atendimentos;
INSERT INTO pessoa_atendida (nome, data_nascimento, contato) VALUES
  ('Pessoa Exemplo A', '2010-04-12', NULL),
  ('Pessoa Exemplo B', '2011-08-20', NULL),
  ('Pessoa Exemplo C', '2009-02-03', NULL);

INSERT INTO atendimento (pessoa_id, tipo, chegada, estado) VALUES
  (1, 'Orientação', '2026-09-28 09:00:00', 'AGUARDANDO'),
  (2, 'Cadastro', '2026-09-28 09:05:00', 'AGUARDANDO'),
  (3, 'Orientação', '2026-09-28 09:10:00', 'AGUARDANDO');

-- SELECT com JOIN, WHERE e ORDER BY: fila FIFO com desempate por id.
SELECT a.id, p.nome, a.tipo, a.chegada, a.estado
FROM atendimento a JOIN pessoa_atendida p ON p.id = a.pessoa_id
WHERE a.estado = 'AGUARDANDO'
ORDER BY a.chegada ASC, a.id ASC;

-- Consulta do histórico de uma pessoa.
SELECT p.nome, a.id, a.tipo, a.chegada, a.inicio, a.fim, a.estado
FROM pessoa_atendida p JOIN atendimento a ON a.pessoa_id = p.id
WHERE p.id = 1 ORDER BY a.chegada DESC, a.id DESC;

SELECT estado, COUNT(*) AS quantidade FROM atendimento GROUP BY estado ORDER BY estado;

UPDATE atendimento SET estado = 'EM_ATENDIMENTO', inicio = '2026-09-28 09:15:00'
WHERE id = 1 AND estado = 'AGUARDANDO';

INSERT INTO pessoa_atendida (nome) VALUES ('Cadastro Temporário');
DELETE FROM pessoa_atendida WHERE nome = 'Cadastro Temporário'
  AND NOT EXISTS (SELECT 1 FROM atendimento WHERE pessoa_id = pessoa_atendida.id);
