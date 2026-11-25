package com.busbooking.repository;

import com.busbooking.model.CityBusStop;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CityBusStopRepository extends JpaRepository<CityBusStop, Long> {

    List<CityBusStop> findByLatBetweenAndLngBetween(double minLat, double maxLat, double minLng, double maxLng);
}
