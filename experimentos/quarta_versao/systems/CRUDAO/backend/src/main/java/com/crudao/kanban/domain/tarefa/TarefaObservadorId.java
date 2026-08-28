package com.crudao.kanban.domain.tarefa;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class TarefaObservadorId implements Serializable {

    @Column(name = "tarefa_id")
    private UUID tarefaId;

    @Column(name = "usuario_id")
    private UUID usuarioId;
}

