package com.busbooking.repository;

import com.busbooking.model.Booking;
import com.busbooking.model.BookingStatus;
import com.busbooking.model.Trip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long>, JpaSpecificationExecutor<Booking> {

    Optional<Booking> findByBookingCode(String bookingCode);

    boolean existsByBookingCode(String bookingCode);

    List<Booking> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    List<Booking> findByTripAndStatus(Trip trip, BookingStatus status);

    @Query("select s from Booking b join b.seats s where b.trip.id = :tripId and b.status = :status")
    List<String> findSeats(@Param("tripId") Long tripId, @Param("status") BookingStatus status);

    /** Booked seat counts for many trips in one query: rows of [tripId, count]. */
    @Query("""
            select b.trip.id, count(s) from Booking b join b.seats s
            where b.trip.id in :tripIds and b.status = :status
            group by b.trip.id
            """)
    List<Object[]> countSeatsByTrip(@Param("tripIds") Collection<Long> tripIds, @Param("status") BookingStatus status);
}
