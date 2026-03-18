package com.hotelvista.repository;

import com.hotelvista.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByRoomIdAndStatusOrderByCreatedAtDesc(Long roomId, Review.Status status);
    List<Review> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<Review> findAllByOrderByCreatedAtDesc();
    Optional<Review> findByReservationId(Long reservationId);
    boolean existsByReservationId(Long reservationId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.room.id = :roomId AND r.status = 'PUBLISHED'")
    Double avgRatingByRoom(@Param("roomId") Long roomId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.room.id = :roomId AND r.status = 'PUBLISHED'")
    Long countByRoom(@Param("roomId") Long roomId);
}
