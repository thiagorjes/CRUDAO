package com.crudao.kanban.domain.notificacao;

import com.crudao.kanban.domain.tarefa.Tarefa;
import com.crudao.kanban.domain.usuario.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

/**
 * Notificação interna (RF-005) — gerada ao mudar etapa/marcar-desmarcar impedimento, para o
 * responsável, o criador e os {@link com.crudao.kanban.domain.tarefa.TarefaObservador} explícitos
 * da tarefa.
 */
@Entity
@Table(name = "notificacao")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Notificacao {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tarefa_id", nullable = false)
    private Tarefa tarefa;

    /** {@code TRANSICAO_ETAPA}, {@code IMPEDIMENTO_MARCADO}, {@code IMPEDIMENTO_DESMARCADO}. */
    @Column(nullable = false, length = 30)
    private String tipo;

    @Column(nullable = false, length = 500)
    private String mensagem;

    @Column(nullable = false)
    private boolean lida;

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm;
}
