package com.busbooking.repository;

import com.busbooking.model.CityBusLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CityBusLineRepository extends JpaRepository<CityBusLine, Long> {

    @Query("select distinct l from CityBusLine l left join fetch l.directions")
    List<CityBusLine> findAllWithDirections();

    @Query("select l from CityBusLine l left join fetch l.directions where l.id = :id")
    Optional<CityBusLine> findWithDirections(@Param("id") Long id);
}
