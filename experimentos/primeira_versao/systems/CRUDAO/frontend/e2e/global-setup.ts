import { ADMIN, USER, ApiCliente } from './fixtures/api';

/**
 * Provisiona `admin.teste`/`user.teste` no backend uma única vez, de forma serial, antes dos
 * workers paralelos começarem. Evita uma corrida real no primeiro login concorrente de cada
 * usuário: `UsuarioContexto.provisionar` (TASK-04.1) não é idempotente sob concorrência — duas
 * requisições simultâneas com o mesmo `keycloak_sub` (nenhuma encontra o usuário ainda por
 * `findByKeycloakSub`) tentam inserir a mesma linha e uma falha com 500 (unique constraint).
 * Não é um problema introduzido por esta task nem por este diff — mitigado aqui no nível do
 * fixture de teste; correção na origem (upsert/tratamento de `DataIntegrityViolationException`
 * em `UsuarioContexto`) fica fora do escopo da TASK-06.1.
 */
export default async function globalSetup() {
  await ApiCliente.autenticar(ADMIN).then((c) => c.get('/usuarios/me'));
  await ApiCliente.autenticar(USER).then((c) => c.get('/usuarios/me'));
}
