-- Usuario.adminGlobal — bootstrap do primeiro administrador (RF-008, ADR-007).
--
-- usuario_projeto_papel.projeto_id é NOT NULL (V2) — não há como vincular um usuário ao papel
-- global `admin` (projeto_id NULL) via essa tabela, então nenhum usuário conseguiria criar o
-- primeiro Projeto (POST /api/projetos exige autorização, mas nenhum projeto existe ainda para
-- escopar `projeto:administrar`). admin_global resolve o bootstrap: setado via
-- UsuarioProvisioningService no primeiro login do e-mail configurado em
-- `kanban.bootstrap.admin-email` (ADR-007) — não por seed SQL, pois o keycloak_sub real só existe
-- após o primeiro login OIDC.
ALTER TABLE usuario ADD COLUMN admin_global BOOLEAN NOT NULL DEFAULT FALSE;
