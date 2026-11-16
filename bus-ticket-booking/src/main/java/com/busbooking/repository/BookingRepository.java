package com.busbooking.repository;

import com.busbooking.model.Booking;
import com.busbooking.model.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    Optional<Booking> findByBookingCode(String bookingCode);

    boolean existsByBookingCode(String bookingCode);

    @Query("select s from Booking b join b.seats s where b.trip.id = :tripId and b.status = :status")
    List<String> findSeats(@Param("tripId") Long tripId, @Param("status") BookingStatus status);
}
