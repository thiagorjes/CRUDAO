package com.crudao.kanban.projeto;

import com.crudao.kanban.auth.UsuarioAutenticadoHolder;
import com.crudao.kanban.domain.papel.Papel;
import com.crudao.kanban.domain.papel.PapelPermissao;
import com.crudao.kanban.domain.papel.PapelPermissaoRepository;
import com.crudao.kanban.domain.papel.PapelRepository;
import com.crudao.kanban.domain.papel.PermissaoRepository;
import com.crudao.kanban.domain.papel.UsuarioProjetoPapelRepository;
import com.crudao.kanban.domain.usuario.Projeto;
import com.crudao.kanban.domain.usuario.ProjetoRepository;
import com.crudao.kanban.domain.usuario.Usuario;
import com.crudao.kanban.rbac.PermissaoGuard;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProjetoService {
    private static final List<RoleDefault> DEFAULT_ROLES =
            List.of(
                    new RoleDefault("product_owner", "Product Owner",
                            List.of("tarefa:gerenciar", "tarefa:finalizar", "tarefa:impedimento")),
                    new RoleDefault("project_admin", "Administrador do projeto",
                            List.of("tarefa:gerenciar", "tarefa:finalizar", "tarefa:impedimento",
                                    "tarefa:excluir", "projeto:administrar", "workflow:administrar",
                                    "papel:administrar", "usuario:associar")),
                    new RoleDefault("dev", "Desenvolvedor",
                            List.of("tarefa:gerenciar", "tarefa:impedimento")),
                    new RoleDefault("gestor", "Gestor", List.of("tarefa:gerenciar")));

    private final ProjetoRepository projetoRepository;
    private final PapelRepository papelRepository;
    private final PapelPermissaoRepository papelPermissaoRepository;
    private final PermissaoRepository permissaoRepository;
    private final PermissaoGuard permissaoGuard;
    private final UsuarioProjetoPapelRepository vinculoRepository;

    public ProjetoService(ProjetoRepository projetoRepository, PapelRepository papelRepository,
            PapelPermissaoRepository papelPermissaoRepository, PermissaoRepository permissaoRepository,
            PermissaoGuard permissaoGuard,
            UsuarioProjetoPapelRepository vinculoRepository) {
        this.projetoRepository = projetoRepository;
        this.papelRepository = papelRepository;
        this.papelPermissaoRepository = papelPermissaoRepository;
        this.permissaoRepository = permissaoRepository;
        this.permissaoGuard = permissaoGuard;
        this.vinculoRepository = vinculoRepository;
    }

    @Transactional
    public Projeto criar(String nome, String descricao) {
        Usuario usuario = usuarioObrigatorio();
        if (!usuario.isAdminGlobal()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado");
        }
        validarNome(nome);
        Projeto projeto = new Projeto();
        projeto.setNome(nome.trim());
        projeto.setDescricao(descricao);
        projeto.setCriadoPor(usuario);
        projeto.setStatus(Projeto.Status.ATIVO);
        projeto = projetoRepository.save(projeto);
        for (RoleDefault role : DEFAULT_ROLES) {
            Papel papel = new Papel();
            papel.setProjeto(projeto);
            papel.setChave(role.chave());
            papel.setNome(role.nome());
            papel = papelRepository.save(papel);
            Set<String> habilitadas = Set.copyOf(role.permissoes());
            for (var permissao : permissaoRepository.findAll()) {
                papelPermissaoRepository.save(
                        new PapelPermissao(papel, permissao, habilitadas.contains(permissao.getChave())));
            }
        }
        return projeto;
    }

    @Transactional(readOnly = true)
    public List<Projeto> listar() {
        Usuario usuario = usuarioObrigatorio();
        if (usuario.isAdminGlobal()) return projetoRepository.findAll();
        return vinculoRepository.findByUsuarioId(usuario.getId()).stream()
                .map(v -> v.getProjeto()).distinct().toList();
    }

    @Transactional(readOnly = true)
    public Projeto obter(UUID id) {
        Usuario usuario = usuarioObrigatorio();
        Projeto projeto = projetoRepository.findById(id).orElseThrow(this::naoEncontrado);
        if (!usuario.isAdminGlobal() && !permissaoGuard.membro(id)) throw naoEncontrado();
        return projeto;
    }

    /**
     * TL-10: usuários associados ao projeto (usado também para popular selects de
     * responsável/observador nas telas de tarefa). Mesma regra de acesso de {@link #obter(UUID)}.
     */
    @Transactional(readOnly = true)
    public List<com.crudao.kanban.projeto.dto.UsuarioProjetoResponse> listarUsuarios(UUID projetoId) {
        Usuario usuario = usuarioObrigatorio();
        if (!projetoRepository.existsById(projetoId)) throw naoEncontrado();
        if (!usuario.isAdminGlobal() && !permissaoGuard.membro(projetoId)) throw naoEncontrado();

        return vinculoRepository.findByProjetoId(projetoId).stream()
                .map(v -> com.crudao.kanban.projeto.dto.UsuarioProjetoResponse.builder()
                        .usuarioId(v.getUsuario().getId())
                        .usuarioNome(v.getUsuario().getNome())
                        .papelId(v.getPapel().getId())
                        .papelNome(v.getPapel().getNome())
                        .build())
                .toList();
    }

    @Transactional
    public Projeto atualizar(UUID id, String nome, String descricao) {
        permissaoGuard.exigir(id, "projeto:administrar");
        permissaoGuard.exigirProjetoAtivo(id);
        validarNome(nome);
        Projeto projeto = projetoRepository.findById(id).orElseThrow(this::naoEncontrado);
        projeto.setNome(nome.trim());
        projeto.setDescricao(descricao);
        return projetoRepository.save(projeto);
    }

    @Transactional
    public Projeto finalizar(UUID id) {
        permissaoGuard.exigir(id, "projeto:administrar");
        Projeto projeto = projetoRepository.findById(id).orElseThrow(this::naoEncontrado);
        projeto.setStatus(Projeto.Status.FINALIZADO);
        projeto.setFinalizadoEm(OffsetDateTime.now());
        return projetoRepository.save(projeto);
    }

    @Transactional
    public Projeto reabrir(UUID id) {
        permissaoGuard.exigir(id, "projeto:administrar");
        Projeto projeto = projetoRepository.findById(id).orElseThrow(this::naoEncontrado);
        projeto.setStatus(Projeto.Status.ATIVO);
        projeto.setFinalizadoEm(null);
        return projetoRepository.save(projeto);
    }

    private Usuario usuarioObrigatorio() {
        Usuario usuario = UsuarioAutenticadoHolder.get();
        if (usuario == null || !usuario.isAtivo())
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado");
        return usuario;
    }

    private void validarNome(String nome) {
        if (nome == null || nome.isBlank())
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "nome é obrigatório");
    }

    private ResponseStatusException naoEncontrado() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "projeto não encontrado");
    }

    private record RoleDefault(String chave, String nome, List<String> permissoes) {}
}
