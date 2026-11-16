package com.busbooking.repository;

import com.busbooking.model.Route;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface RouteRepository extends JpaRepository<Route, Long> {

    List<Route> findByPopularTrueOrderById();

    @Query("select distinct r.origin from Route r order by r.origin")
    List<String> findCities();
}
