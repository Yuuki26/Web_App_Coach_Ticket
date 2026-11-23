package com.busbooking.repository;

import com.busbooking.model.Route;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface RouteRepository extends JpaRepository<Route, Long>, JpaSpecificationExecutor<Route> {

    List<Route> findByPopularTrueAndActiveTrueOrderById();

    @Query("select distinct r.origin from Route r where r.active = true order by r.origin")
    List<String> findCities();

    boolean existsByOriginIgnoreCaseAndDestinationIgnoreCase(String origin, String destination);
}
