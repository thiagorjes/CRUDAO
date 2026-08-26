package com.crudao.kanban.domain.tarefa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Observador explícito da tarefa (RF-005) — responsável e criador são observadores implícitos. */
@Entity
@Table(name = "tarefa_observador")
@IdClass(TarefaObservador.Pk.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TarefaObservador {

    @Id
    @Column(name = "tarefa_id")
    private UUID tarefaId;

    @Id
    @Column(name = "usuario_id")
    private UUID usuarioId;

    public static class Pk implements Serializable {
        private UUID tarefaId;
        private UUID usuarioId;

        public Pk() {}

        public Pk(UUID tarefaId, UUID usuarioId) {
            this.tarefaId = tarefaId;
            this.usuarioId = usuarioId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Pk pk)) return false;
            return Objects.equals(tarefaId, pk.tarefaId) && Objects.equals(usuarioId, pk.usuarioId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(tarefaId, usuarioId);
        }
    }
}
