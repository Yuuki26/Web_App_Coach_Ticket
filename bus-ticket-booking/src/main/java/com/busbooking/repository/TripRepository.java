package com.busbooking.repository;

import com.busbooking.model.Trip;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TripRepository extends JpaRepository<Trip, Long> {

    @Query("""
            select t from Trip t join fetch t.route r join fetch t.operator
            where lower(r.origin) = lower(:origin)
              and lower(r.destination) = lower(:destination)
              and t.departureTime >= :from and t.departureTime < :to
            order by t.departureTime
            """)
    List<Trip> search(@Param("origin") String origin,
                      @Param("destination") String destination,
                      @Param("from") LocalDateTime from,
                      @Param("to") LocalDateTime to);

    /** True when at least one trip departs in the given time window. */
    boolean existsByDepartureTimeBetween(LocalDateTime from, LocalDateTime to);

    /**
     * Loads a trip and locks its row until the transaction ends, so two people
     * booking the same trip at the same moment are handled one after the other.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Trip t where t.id = :id")
    Optional<Trip> findByIdForUpdate(@Param("id") Long id);
}
