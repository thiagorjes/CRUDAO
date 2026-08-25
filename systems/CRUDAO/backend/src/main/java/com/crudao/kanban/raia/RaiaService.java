package com.crudao.kanban.raia;

import com.crudao.kanban.domain.raia.Raia;
import com.crudao.kanban.domain.raia.RaiaRepository;
import com.crudao.kanban.domain.usuario.ProjetoRepository;
import com.crudao.kanban.rbac.PermissaoGuard;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * CRUD de {@link Raia} (RF-011).
 *
 * <p>Leitura exige apenas vínculo com o projeto ({@code contracts/raias.md}); escrita exige
 * {@code workflow:administrar} e projeto {@code ATIVO} (RN-015). A raia default global ({@code
 * projeto == null}, seed da migration V4) nunca pode ser editada ou excluída por estes endpoints.
 */
@Service
public class RaiaService {

    private static final String PERMISSAO_ADMINISTRAR = "workflow:administrar";

    private final RaiaRepository raiaRepository;
    private final ProjetoRepository projetoRepository;
    private final PermissaoGuard permissaoGuard;

    public RaiaService(
            RaiaRepository raiaRepository, ProjetoRepository projetoRepository, PermissaoGuard permissaoGuard) {
        this.raiaRepository = raiaRepository;
        this.projetoRepository = projetoRepository;
        this.permissaoGuard = permissaoGuard;
    }

    public List<RaiaResponse> listar(UUID projetoId) {
        if (!permissaoGuard.membro(projetoId)) {
            throw new AccessDeniedException("Acesso negado");
        }

        List<Raia> raiasDoProjeto = raiaRepository.findByProjetoId(projetoId);
        if (!raiasDoProjeto.isEmpty()) {
            return raiasDoProjeto.stream().map(r -> toResponse(r, false)).toList();
        }
        return raiaRepository.findByProjetoIdIsNull().stream().map(r -> toResponse(r, true)).toList();
    }

    @Transactional
    public RaiaResponse criar(UUID projetoId, CriarRaiaRequest request) {
        permissaoGuard.exigir(projetoId, PERMISSAO_ADMINISTRAR);
        permissaoGuard.exigirProjetoAtivo(projetoId);
        exigirNomeValido(request.nome());
        exigirOrdemValida(request.ordem());

        Raia raia = new Raia();
        raia.setProjeto(projetoRepository.getReferenceById(projetoId));
        raia.setNome(request.nome());
        raia.setOrdem(request.ordem());
        try {
            raia = raiaRepository.save(raia);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "já existe raia com essa ordem no projeto");
        }
        return toResponse(raia, false);
    }

    @Transactional
    public RaiaResponse editar(UUID id, EditarRaiaRequest request) {
        Raia raia = buscarRaiaDoProjeto(id);
        UUID projetoId = raia.getProjeto().getId();
        permissaoGuard.exigir(projetoId, PERMISSAO_ADMINISTRAR);
        permissaoGuard.exigirProjetoAtivo(projetoId);
        exigirNomeValido(request.nome());
        exigirOrdemValida(request.ordem());

        raia.setNome(request.nome());
        raia.setOrdem(request.ordem());
        try {
            raia = raiaRepository.save(raia);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "já existe raia com essa ordem no projeto");
        }
        return toResponse(raia, false);
    }

    @Transactional
    public void excluir(UUID id) {
        Raia raia = buscarRaiaDoProjeto(id);
        UUID projetoId = raia.getProjeto().getId();
        permissaoGuard.exigir(projetoId, PERMISSAO_ADMINISTRAR);
        permissaoGuard.exigirProjetoAtivo(projetoId);

        if (possuiTarefasAtivasNaRaia(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "raia possui tarefas ativas vinculadas");
        }

        raiaRepository.delete(raia);
    }

    /**
     * Stub RN-005 — {@code Tarefa} só existe a partir de TASK-04.1; até lá, nenhuma raia tem
     * tarefas ativas. Substituído obrigatoriamente pela checagem real em TASK-04.1 (mesma decisão
     * fechada de TASK-03.2, não é opcional).
     */
    private boolean possuiTarefasAtivasNaRaia(UUID raiaId) {
        return false;
    }

    private void exigirNomeValido(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "nome é obrigatório");
        }
    }

    private void exigirOrdemValida(int ordem) {
        if (ordem < 0) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "ordem não pode ser negativa");
        }
    }

    private Raia buscarRaiaDoProjeto(UUID id) {
        Raia raia =
                raiaRepository
                        .findById(id)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "raia não encontrada"));
        if (raia.getProjeto() == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY, "raia default global não pode ser editada ou excluída");
        }
        return raia;
    }

    private RaiaResponse toResponse(Raia raia, boolean global) {
        return new RaiaResponse(raia.getId(), raia.getNome(), raia.getOrdem(), global);
    }
}
