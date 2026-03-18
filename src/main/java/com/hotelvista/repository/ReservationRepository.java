package com.hotelvista.repository;

import com.hotelvista.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    List<Reservation> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<Reservation> findByIdAndUserId(Long id, Long userId);

    long countByStatus(Reservation.Status status);

    @Query("""
        SELECT COUNT(r) FROM Reservation r
        WHERE r.room.id = :roomId
        AND r.status <> 'CANCELLED'
        AND r.checkIn < :checkOut
        AND r.checkOut > :checkIn
    """)
    long countConflicts(
        @Param("roomId") Long roomId,
        @Param("checkIn") LocalDate checkIn,
        @Param("checkOut") LocalDate checkOut
    );
}
