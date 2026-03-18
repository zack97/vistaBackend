package com.hotelvista.repository;

import com.hotelvista.entity.Room;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {

    @Query("""
        SELECT r FROM Room r WHERE r.isAvailable = true
        AND (:type IS NULL OR r.type = :type)
        AND (:minPrice IS NULL OR r.pricePerNight >= :minPrice)
        AND (:maxPrice IS NULL OR r.pricePerNight <= :maxPrice)
        AND (:capacity IS NULL OR r.capacity >= :capacity)
        AND (:viewType IS NULL OR r.viewType LIKE %:viewType%)
        AND r.id NOT IN (
            SELECT res.room.id FROM Reservation res
            WHERE res.status <> 'CANCELLED'
            AND res.checkIn < :checkOut
            AND res.checkOut > :checkIn
        )
        ORDER BY r.rating DESC, r.reviewCount DESC
    """)
    Page<Room> findAvailableRooms(
        @Param("type") Room.RoomType type,
        @Param("minPrice") BigDecimal minPrice,
        @Param("maxPrice") BigDecimal maxPrice,
        @Param("capacity") Integer capacity,
        @Param("viewType") String viewType,
        @Param("checkIn") LocalDate checkIn,
        @Param("checkOut") LocalDate checkOut,
        Pageable pageable
    );
}
