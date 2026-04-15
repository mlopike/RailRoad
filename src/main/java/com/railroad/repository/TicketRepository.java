package com.railroad.repository;

import com.railroad.entity.Ticket;
import com.railroad.entity.Ticket.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {
    List<Ticket> findByBookingId(Long bookingId);
    List<Ticket> findByStatus(TicketStatus status);
}
