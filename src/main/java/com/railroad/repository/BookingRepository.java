package com.railroad.repository;

import com.railroad.entity.Booking;
import com.railroad.entity.Booking.BookingStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    
    @EntityGraph(attributePaths = {"fromStation", "toStation", "selectedTrain", "tickets"})
    List<Booking> findByEmailOrderByCreatedAtDesc(String email);
    
    @EntityGraph(attributePaths = {"fromStation", "toStation", "selectedTrain", "tickets"})
    List<Booking> findByStatus(BookingStatus status);
    
    @EntityGraph(attributePaths = {"fromStation", "toStation", "selectedTrain", "tickets"})
    @Query("SELECT b FROM Booking b WHERE b.id = :id")
    Optional<Booking> findByIdWithDetails(@Param("id") Long id);
}
