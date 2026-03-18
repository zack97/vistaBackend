package com.hotelvista.controller;

import com.hotelvista.entity.*;
import com.hotelvista.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final RoomRepository roomRepository;
    private final ReservationRepository reservationRepository;

    // GET /api/reviews/room/:id — public
    @GetMapping("/room/{roomId}")
    public ResponseEntity<?> getRoomReviews(@PathVariable Long roomId) {
        List<Map<String, Object>> list = reviewRepository
            .findByRoomIdAndStatusOrderByCreatedAtDesc(roomId, Review.Status.PUBLISHED)
            .stream().map(this::toMap).collect(Collectors.toList());

        Double avg = reviewRepository.avgRatingByRoom(roomId);
        Long count = reviewRepository.countByRoom(roomId);

        return ResponseEntity.ok(Map.of(
            "reviews", list,
            "averageRating", avg != null ? Math.round(avg * 10.0) / 10.0 : 0,
            "totalReviews", count
        ));
    }

    // GET /api/reviews/all — public (for homepage)
    @GetMapping("/all")
    public ResponseEntity<?> getAllReviews() {
        List<Map<String, Object>> list = reviewRepository.findAllByOrderByCreatedAtDesc()
            .stream()
            .filter(r -> r.getStatus() == Review.Status.PUBLISHED)
            .limit(20)
            .map(this::toMap)
            .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    // GET /api/reviews/my — auth
    @GetMapping("/my")
    public ResponseEntity<?> getMyReviews(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        List<Map<String, Object>> list = reviewRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
            .stream().map(this::toMap).collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    // POST /api/reviews — auth
    @PostMapping
    public ResponseEntity<?> createReview(@AuthenticationPrincipal UserDetails userDetails,
                                           @RequestBody Map<String, Object> body) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();

        Long roomId = Long.valueOf(body.get("room_id").toString());
        Integer rating = Integer.valueOf(body.get("rating").toString());
        String comment = body.get("comment") != null ? body.get("comment").toString() : null;
        String title = body.get("title") != null ? body.get("title").toString() : null;
        Long reservationId = body.get("reservation_id") != null ? Long.valueOf(body.get("reservation_id").toString()) : null;

        if (rating < 1 || rating > 5)
            return ResponseEntity.badRequest().body(Map.of("message", "Rating must be between 1 and 5."));

        Room room = roomRepository.findById(roomId).orElse(null);
        if (room == null) return ResponseEntity.notFound().build();

        // Check if already reviewed this reservation
        if (reservationId != null && reviewRepository.existsByReservationId(reservationId))
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "You have already reviewed this reservation."));

        Review review = new Review();
        review.setUser(user);
        review.setRoom(room);
        review.setRating(rating);
        review.setComment(comment);
        review.setTitle(title);
        review.setStatus(Review.Status.PUBLISHED);

        if (reservationId != null) {
            reservationRepository.findById(reservationId).ifPresent(review::setReservation);
        }

        reviewRepository.save(review);

        // Update room rating
        Double avg = reviewRepository.avgRatingByRoom(roomId);
        Long count = reviewRepository.countByRoom(roomId);
        if (avg != null) {
            room.setRating(java.math.BigDecimal.valueOf(Math.round(avg * 10.0) / 10.0));
            room.setReviewCount(count.intValue());
            roomRepository.save(room);
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
            "message", "Review submitted successfully.", "review", toMap(review)));
    }

    // DELETE /api/reviews/:id — auth (own review)
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteReview(@AuthenticationPrincipal UserDetails userDetails, @PathVariable Long id) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        Review review = reviewRepository.findById(id).orElse(null);
        if (review == null) return ResponseEntity.notFound().build();
        if (!review.getUser().getId().equals(user.getId()) && user.getRole() != User.Role.ADMIN)
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Not authorized."));
        reviewRepository.delete(review);
        return ResponseEntity.ok(Map.of("message", "Review deleted."));
    }

    private Map<String, Object> toMap(Review r) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", r.getId());
        m.put("user_id", r.getUser().getId());
        m.put("user_name", r.getUser().getFirstName() + " " + r.getUser().getLastName());
        m.put("room_id", r.getRoom().getId());
        m.put("room_name", r.getRoom().getName());
        m.put("rating", r.getRating());
        m.put("title", r.getTitle() != null ? r.getTitle() : "");
        m.put("comment", r.getComment() != null ? r.getComment() : "");
        m.put("status", r.getStatus().name().toLowerCase());
        m.put("reservation_id", r.getReservation() != null ? r.getReservation().getId() : null);
        m.put("created_at", r.getCreatedAt() != null ? r.getCreatedAt().toString() : "");
        return m;
    }
}
