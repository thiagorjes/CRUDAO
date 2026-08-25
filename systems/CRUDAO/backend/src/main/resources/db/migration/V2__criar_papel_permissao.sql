-- Papel, Permissao, PapelPermissao, UsuarioProjetoPapel — RBAC (RF-013, RF-015, RF-016).
--
-- Decisão (TASK-01.2): apenas o papel `admin` (global, protegido) é semeado nesta migration.
-- Os papéis default por projeto (product_owner, project_admin, dev, gestor) são escopados por
-- Projeto (projeto_id NOT NULL) e não podem ser semeados aqui pois nenhum Projeto existe ainda —
-- serão instanciados programaticamente pelo ProjetoService ao criar um novo projeto (TASK-03.1),
-- usando os defaults de RN-011/RN-012/RN-013/RN-CB-001/RN-CB-002 como constante no código.

CREATE TABLE papel (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    projeto_id  UUID REFERENCES projeto (id),
    chave       VARCHAR(50) NOT NULL,
    nome        VARCHAR(255) NOT NULL,
    protegido   BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_papel_projeto_chave UNIQUE (projeto_id, chave)
);

-- Garante um único papel global por chave (ex.: um único `admin`) — UNIQUE(projeto_id, chave)
-- sozinho não bastaria, pois Postgres trata NULL como distinto em constraints UNIQUE comuns.
CREATE UNIQUE INDEX uk_papel_global_chave ON papel (chave) WHERE projeto_id IS NULL;

CREATE TABLE permissao (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    chave       VARCHAR(50) NOT NULL,
    descricao   VARCHAR(255) NOT NULL,
    CONSTRAINT uk_permissao_chave UNIQUE (chave)
);

CREATE TABLE papel_permissao (
    papel_id     UUID NOT NULL REFERENCES papel (id),
    permissao_id UUID NOT NULL REFERENCES permissao (id),
    habilitada   BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (papel_id, permissao_id)
);

CREATE TABLE usuario_projeto_papel (
    usuario_id    UUID NOT NULL REFERENCES usuario (id),
    projeto_id    UUID NOT NULL REFERENCES projeto (id),
    papel_id      UUID NOT NULL REFERENCES papel (id),
    associado_em  TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (usuario_id, projeto_id, papel_id)
);

-- Catálogo fixo de permissões (não editável em runtime). `tarefa:excluir` incluído aqui por
-- decisão do Comitê de Análise (ver TASK-04.4) — permissão dedicada, não flag de contexto sobre
-- `tarefa:gerenciar`.
INSERT INTO permissao (chave, descricao) VALUES
    ('tarefa:gerenciar', 'Criar/excluir card'),
    ('tarefa:finalizar', 'Mover para/reabrir etapa final'),
    ('tarefa:impedimento', 'Marcar/desmarcar impedimento'),
    ('tarefa:excluir', 'Excluir card quando o papel for dev (RN-CB-002)'),
    ('projeto:administrar', 'Administrar projeto'),
    ('workflow:administrar', 'Administrar workflow/etapa/transição'),
    ('papel:administrar', 'Administrar papéis e permissões'),
    ('usuario:associar', 'Associar usuário a projeto/papel');

-- Papel admin: global (projeto_id NULL), protegido (RN-006), com todas as permissões habilitadas.
INSERT INTO papel (projeto_id, chave, nome, protegido)
    VALUES (NULL, 'admin', 'Administrador', TRUE);

INSERT INTO papel_permissao (papel_id, permissao_id, habilitada)
    SELECT p.id, perm.id, TRUE
    FROM papel p
    CROSS JOIN permissao perm
    WHERE p.chave = 'admin' AND p.projeto_id IS NULL;
