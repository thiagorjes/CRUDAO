-- Permissão dedicada `tarefa:auditoria` (RF-017) — GET /api/tarefas/{id}/auditoria exige "papel
-- gestor ou admin" (contracts/tarefas.md), o que não é representável por `tarefa:gerenciar`
-- (também concedida a dev por padrão em projetos que permitem criar/mover card). Achado de code
-- review (agent QA, TASK-04.4).

INSERT INTO permissao (chave, descricao) VALUES
    ('tarefa:auditoria', 'Ver histórico de auditoria da tarefa');

-- Papel admin (global, protegido) recebe todas as permissões — mesmo padrão de seed da V2.
INSERT INTO papel_permissao (papel_id, permissao_id, habilitada)
    SELECT p.id, perm.id, TRUE
    FROM papel p
    CROSS JOIN permissao perm
    WHERE p.chave = 'admin' AND p.projeto_id IS NULL AND perm.chave = 'tarefa:auditoria';
