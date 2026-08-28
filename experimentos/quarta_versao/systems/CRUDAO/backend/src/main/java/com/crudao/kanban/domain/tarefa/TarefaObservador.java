package com.crudao.kanban.domain.tarefa;

import com.crudao.kanban.domain.usuario.Usuario;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tarefa_observador")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TarefaObservador {

    @EmbeddedId
    private TarefaObservadorId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("tarefaId")
    @JoinColumn(name = "tarefa_id")
    private Tarefa tarefa;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("usuarioId")
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;
}

