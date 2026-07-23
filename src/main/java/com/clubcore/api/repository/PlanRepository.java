package com.clubcore.api.repository;

import com.clubcore.api.entity.Plan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlanRepository extends JpaRepository<Plan, Long> {

    List<Plan> findByActivoTrue();
}
