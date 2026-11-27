package com.busbooking.repository;

import com.busbooking.model.CityBusDirectionStop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface CityBusDirectionStopRepository extends JpaRepository<CityBusDirectionStop, Long> {

    /** Every (direction id, stop name) pair, for searching lines by the stops they pass. */
    @Query("select ds.direction.id, s.name from CityBusDirectionStop ds join ds.stop s")
    List<Object[]> findDirectionStopNames();

    /** Stops of the given directions, in order, with the stop loaded. */
    @Query("select ds from CityBusDirectionStop ds join fetch ds.stop where ds.direction.id in :ids order by ds.direction.id, ds.seq")
    List<CityBusDirectionStop> findByDirectionIds(@Param("ids") Collection<Long> ids);

    /** (stop id, line id, line ref, line colour) for every line serving the given stops. */
    @Query("select distinct ds.stop.id, l.id, l.ref, l.colour from CityBusDirectionStop ds join ds.direction d join d.line l "
            + "where ds.stop.id in :stopIds")
    List<Object[]> findLinesAtStops(@Param("stopIds") Collection<Long> stopIds);
}
