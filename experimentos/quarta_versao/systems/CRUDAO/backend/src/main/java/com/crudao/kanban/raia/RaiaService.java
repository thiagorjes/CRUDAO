package com.crudao.kanban.raia;

import com.crudao.kanban.domain.raia.Raia;
import com.crudao.kanban.domain.raia.RaiaRepository;
import com.crudao.kanban.domain.usuario.Projeto;
import com.crudao.kanban.domain.usuario.ProjetoRepository;
import com.crudao.kanban.rbac.PermissaoGuard;
import com.crudao.kanban.raia.dto.*;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class RaiaService {

    private final RaiaRepository raiaRepository;
    private final ProjetoRepository projetoRepository;
    private final PermissaoGuard permissaoGuard;

    @Transactional(readOnly = true)
    public List<RaiaResponse> listarRaias(UUID projetoId) {
        if (!permissaoGuard.membro(projetoId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado");
        }

        List<Raia> customRaias = raiaRepository.findByProjetoIdOrderByOrdemAsc(projetoId);
        if (!customRaias.isEmpty()) {
            return customRaias.stream()
                    .map(r -> new RaiaResponse(r.getId(), r.getNome(), r.getOrdem(), false))
                    .collect(Collectors.toList());
        }

        List<Raia> globalRaias = raiaRepository.findByProjetoIdIsNullOrderByOrdemAsc();
        return globalRaias.stream()
                .map(r -> new RaiaResponse(r.getId(), r.getNome(), r.getOrdem(), true))
                .collect(Collectors.toList());
    }

    @Transactional
    public RaiaResponse criarRaia(UUID projetoId, CriarRaiaRequest request) {
        permissaoGuard.exigirProjetoAtivo(projetoId);
        permissaoGuard.exigir(projetoId, "workflow:administrar");

        Projeto projeto = projetoRepository.findById(projetoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Projeto não encontrado"));

        Raia raia = new Raia();
        raia.setProjeto(projeto);
        raia.setNome(request.getNome());
        raia.setOrdem(request.getOrdem());
        raia = raiaRepository.save(raia);

        return new RaiaResponse(raia.getId(), raia.getNome(), raia.getOrdem(), false);
    }

    @Transactional
    public RaiaResponse atualizarRaia(UUID id, AtualizarRaiaRequest request) {
        Raia raia = raiaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Raia não encontrada"));

        if (raia.getProjeto() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "A raia default global não pode ser editada");
        }

        UUID projetoId = raia.getProjeto().getId();
        permissaoGuard.exigirProjetoAtivo(projetoId);
        permissaoGuard.exigir(projetoId, "workflow:administrar");

        raia.setNome(request.getNome());
        raia.setOrdem(request.getOrdem());
        raia = raiaRepository.save(raia);

        return new RaiaResponse(raia.getId(), raia.getNome(), raia.getOrdem(), false);
    }

    @Transactional
    public void excluirRaia(UUID id) {
        Raia raia = raiaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Raia não encontrada"));

        if (raia.getProjeto() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "A raia default global não pode ser excluída");
        }

        UUID projetoId = raia.getProjeto().getId();
        permissaoGuard.exigirProjetoAtivo(projetoId);
        permissaoGuard.exigir(projetoId, "workflow:administrar");

        if (temTarefasAtivasNaRaia(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Exclusão bloqueada: existem tarefas ativas vinculadas à raia");
        }

        raiaRepository.delete(raia);
    }

    /** Stub de RN-005 (será substituído pela verificação real em TASK-04.1) */
    private boolean temTarefasAtivasNaRaia(UUID raiaId) {
        return false;
    }
}

