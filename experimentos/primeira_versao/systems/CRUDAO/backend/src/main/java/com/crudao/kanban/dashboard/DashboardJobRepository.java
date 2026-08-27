package com.crudao.kanban.dashboard;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DashboardJobRepository extends JpaRepository<DashboardJob, UUID> {}
