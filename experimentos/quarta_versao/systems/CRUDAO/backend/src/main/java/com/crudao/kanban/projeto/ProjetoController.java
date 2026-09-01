package com.crudao.kanban.projeto;

import com.crudao.kanban.domain.usuario.Projeto;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/projetos")
public class ProjetoController {
    private final ProjetoService service;
    public ProjetoController(ProjetoService service) { this.service = service; }

    @GetMapping
    public List<Response> listar() { return service.listar().stream().map(Response::from).toList(); }

    @GetMapping("/{id}")
    public Response obter(@PathVariable UUID id) { return Response.from(service.obter(id)); }

    /** TL-10 — usuários associados ao projeto (também usado por selects de responsável/observador). */
    @GetMapping("/{id}/usuarios")
    public List<com.crudao.kanban.projeto.dto.UsuarioProjetoResponse> listarUsuarios(@PathVariable UUID id) {
        return service.listarUsuarios(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Response criar(@RequestBody Request request) {
        return Response.from(service.criar(request.nome(), request.descricao()));
    }

    @PutMapping("/{id}")
    public Response atualizar(@PathVariable UUID id, @RequestBody Request request) {
        return Response.from(service.atualizar(id, request.nome(), request.descricao()));
    }

    @PostMapping("/{id}/finalizar")
    public LifecycleResponse finalizar(@PathVariable UUID id) {
        return LifecycleResponse.from(service.finalizar(id));
    }

    @PostMapping("/{id}/reabrir")
    public LifecycleResponse reabrir(@PathVariable UUID id) {
        return LifecycleResponse.from(service.reabrir(id));
    }

    public record Request(String nome, String descricao) {}
    public record Response(UUID id, String nome, String descricao, Projeto.Status status) {
        static Response from(Projeto p) { return new Response(p.getId(), p.getNome(), p.getDescricao(), p.getStatus()); }
    }
    public record LifecycleResponse(UUID id, Projeto.Status status) {
        static LifecycleResponse from(Projeto p) { return new LifecycleResponse(p.getId(), p.getStatus()); }
    }
}
