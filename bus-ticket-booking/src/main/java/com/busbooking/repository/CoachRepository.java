package com.busbooking.repository;

import com.busbooking.model.Coach;
import com.busbooking.model.CoachStatus;
import com.busbooking.model.Operator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CoachRepository extends JpaRepository<Coach, Long>, JpaSpecificationExecutor<Coach> {

    boolean existsByPlateNumberIgnoreCase(String plateNumber);

    long countByOperatorAndStatusNot(Operator operator, CoachStatus status);
}
