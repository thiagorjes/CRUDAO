package com.crudao.kanban.domain.tarefa;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TarefaObservadorRepository
        extends JpaRepository<TarefaObservador, TarefaObservador.Pk> {}
