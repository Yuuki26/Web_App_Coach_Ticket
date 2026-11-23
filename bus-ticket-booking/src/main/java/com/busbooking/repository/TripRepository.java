package com.busbooking.repository;

import com.busbooking.model.Coach;
import com.busbooking.model.Route;
import com.busbooking.model.Trip;
import com.busbooking.model.TripStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TripRepository extends JpaRepository<Trip, Long>, JpaSpecificationExecutor<Trip> {

    /** Scheduled trips on active routes for the public search. */
    @Query("""
            select t from Trip t join fetch t.route r join fetch t.coach c join fetch c.operator
            where lower(r.origin) = lower(:origin)
              and lower(r.destination) = lower(:destination)
              and r.active = true
              and t.status = com.busbooking.model.TripStatus.SCHEDULED
              and t.departureTime >= :from and t.departureTime < :to
            order by t.departureTime
            """)
    List<Trip> search(@Param("origin") String origin,
                      @Param("destination") String destination,
                      @Param("from") LocalDateTime from,
                      @Param("to") LocalDateTime to);

    /** True when at least one trip departs in the given time window. */
    boolean existsByDepartureTimeBetween(LocalDateTime from, LocalDateTime to);

    /** Upcoming trips still running on a route or coach: these block removing it. */
    long countByRouteAndStatusAndDepartureTimeAfter(Route route, TripStatus status, LocalDateTime after);

    long countByCoachAndStatusAndDepartureTimeAfter(Coach coach, TripStatus status, LocalDateTime after);

    /**
     * Scheduled trips of this coach whose time overlaps [departure, arrival), ignoring trip
     * {@code excludeId} (the one being edited; pass -1 when creating).
     */
    @Query("""
            select t from Trip t
            where t.coach = :coach and t.status = com.busbooking.model.TripStatus.SCHEDULED and t.id <> :excludeId
              and t.departureTime < :arrival and t.arrivalTime > :departure
            order by t.departureTime
            """)
    List<Trip> findOverlapping(@Param("coach") Coach coach, @Param("departure") LocalDateTime departure,
                               @Param("arrival") LocalDateTime arrival, @Param("excludeId") Long excludeId);

    /**
     * Loads a trip and locks its row until the transaction ends, so two people
     * booking the same trip at the same moment are handled one after the other.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Trip t where t.id = :id")
    Optional<Trip> findByIdForUpdate(@Param("id") Long id);
}
