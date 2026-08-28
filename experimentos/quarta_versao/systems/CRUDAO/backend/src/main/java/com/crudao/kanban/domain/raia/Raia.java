package com.crudao.kanban.domain.raia;

import com.crudao.kanban.domain.usuario.Projeto;
import jakarta.persistence.*;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "raia")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Raia {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "projeto_id")
    private Projeto projeto;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private Integer ordem;
}

