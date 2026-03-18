package com.hotelvista.controller;

import com.hotelvista.entity.*;
import com.hotelvista.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserRepository userRepository;
    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private final ReviewRepository reviewRepository;
    private final PaymentRepository paymentRepository;

    // ===== DASHBOARD =====
    @GetMapping("/stats")
    public ResponseEntity<?> getStats() {
        long totalUsers        = userRepository.count();
        long totalReservations = reservationRepository.count();
        long totalRooms        = roomRepository.count();
        long totalReviews      = reviewRepository.count();
        long pendingRes        = reservationRepository.countByStatus(Reservation.Status.PENDING);
        long confirmedRes      = reservationRepository.countByStatus(Reservation.Status.CONFIRMED);
        long cancelledRes      = reservationRepository.countByStatus(Reservation.Status.CANCELLED);
        long completedRes      = reservationRepository.countByStatus(Reservation.Status.COMPLETED);

        BigDecimal totalRevenue = paymentRepository.findAll().stream()
            .filter(p -> p.getStatus() == Payment.Status.COMPLETED)
            .map(Payment::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", totalUsers);
        stats.put("totalReservations", totalReservations);
        stats.put("totalRooms", totalRooms);
        stats.put("totalReviews", totalReviews);
        stats.put("pendingReservations", pendingRes);
        stats.put("confirmedReservations", confirmedRes);
        stats.put("cancelledReservations", cancelledRes);
        stats.put("completedReservations", completedRes);
        stats.put("totalRevenue", totalRevenue);
        return ResponseEntity.ok(stats);
    }

    // ===== USERS =====
    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers() {
        return ResponseEntity.ok(userRepository.findAll().stream().map(this::userToMap).collect(Collectors.toList()));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        if (!userRepository.existsById(id)) return ResponseEntity.notFound().build();
        userRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "User deleted successfully."));
    }

    @PutMapping("/users/{id}/role")
    public ResponseEntity<?> updateUserRole(@PathVariable Long id, @RequestBody Map<String, String> body) {
        User user = userRepository.findById(id).orElse(null);
        if (user == null) return ResponseEntity.notFound().build();
        try {
            user.setRole(User.Role.valueOf(body.get("role").toUpperCase()));
            userRepository.save(user);
            return ResponseEntity.ok(Map.of("message", "Role updated.", "user", userToMap(user)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid role."));
        }
    }

    // ===== ROOMS =====
    @GetMapping("/rooms")
    public ResponseEntity<?> getAllRooms() {
        return ResponseEntity.ok(roomRepository.findAll().stream().map(this::roomToMap).collect(Collectors.toList()));
    }

    @PostMapping("/rooms")
    public ResponseEntity<?> createRoom(@RequestBody Map<String, Object> body) {
        try {
            Room room = buildRoomFromBody(new Room(), body);
            roomRepository.save(room);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "Room created.", "room", roomToMap(room)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Error: " + e.getMessage()));
        }
    }

    @PutMapping("/rooms/{id}")
    public ResponseEntity<?> updateRoom(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Room room = roomRepository.findById(id).orElse(null);
        if (room == null) return ResponseEntity.notFound().build();
        try {
            buildRoomFromBody(room, body);
            roomRepository.save(room);
            return ResponseEntity.ok(Map.of("message", "Room updated.", "room", roomToMap(room)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Error: " + e.getMessage()));
        }
    }

    @DeleteMapping("/rooms/{id}")
    public ResponseEntity<?> deleteRoom(@PathVariable Long id) {
        if (!roomRepository.existsById(id)) return ResponseEntity.notFound().build();
        roomRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Room deleted."));
    }

    @PutMapping("/rooms/{id}/toggle-availability")
    public ResponseEntity<?> toggleAvailability(@PathVariable Long id) {
        Room room = roomRepository.findById(id).orElse(null);
        if (room == null) return ResponseEntity.notFound().build();
        room.setIsAvailable(!room.getIsAvailable());
        roomRepository.save(room);
        return ResponseEntity.ok(Map.of("message", "Availability updated.", "is_available", room.getIsAvailable()));
    }

    // ===== RESERVATIONS =====
    @GetMapping("/reservations")
    public ResponseEntity<?> getAllReservations() {
        return ResponseEntity.ok(reservationRepository.findAll().stream()
            .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
            .map(this::reservationToMap).collect(Collectors.toList()));
    }

    @PutMapping("/reservations/{id}/validate")
    public ResponseEntity<?> validateReservation(@PathVariable Long id) {
        Reservation res = reservationRepository.findById(id).orElse(null);
        if (res == null) return ResponseEntity.notFound().build();
        if (res.getStatus() != Reservation.Status.PENDING)
            return ResponseEntity.badRequest().body(Map.of("message", "Only pending reservations can be validated."));
        res.setStatus(Reservation.Status.CONFIRMED);
        reservationRepository.save(res);
        return ResponseEntity.ok(Map.of("message", "Reservation validated.", "reservation", reservationToMap(res)));
    }

    @PutMapping("/reservations/{id}/cancel")
    public ResponseEntity<?> cancelReservation(@PathVariable Long id) {
        Reservation res = reservationRepository.findById(id).orElse(null);
        if (res == null) return ResponseEntity.notFound().build();
        res.setStatus(Reservation.Status.CANCELLED);
        reservationRepository.save(res);
        return ResponseEntity.ok(Map.of("message", "Reservation cancelled."));
    }

    @PutMapping("/reservations/{id}/complete")
    public ResponseEntity<?> completeReservation(@PathVariable Long id) {
        Reservation res = reservationRepository.findById(id).orElse(null);
        if (res == null) return ResponseEntity.notFound().build();
        res.setStatus(Reservation.Status.COMPLETED);
        reservationRepository.save(res);
        return ResponseEntity.ok(Map.of("message", "Reservation completed."));
    }

    // ===== REVIEWS =====
    @GetMapping("/reviews")
    public ResponseEntity<?> getAllReviews() {
        return ResponseEntity.ok(reviewRepository.findAllByOrderByCreatedAtDesc().stream().map(this::reviewToMap).collect(Collectors.toList()));
    }

    @PutMapping("/reviews/{id}/hide")
    public ResponseEntity<?> hideReview(@PathVariable Long id) {
        Review review = reviewRepository.findById(id).orElse(null);
        if (review == null) return ResponseEntity.notFound().build();
        review.setStatus(Review.Status.HIDDEN);
        reviewRepository.save(review);
        return ResponseEntity.ok(Map.of("message", "Review hidden."));
    }

    @PutMapping("/reviews/{id}/publish")
    public ResponseEntity<?> publishReview(@PathVariable Long id) {
        Review review = reviewRepository.findById(id).orElse(null);
        if (review == null) return ResponseEntity.notFound().build();
        review.setStatus(Review.Status.PUBLISHED);
        reviewRepository.save(review);
        return ResponseEntity.ok(Map.of("message", "Review published."));
    }

    @DeleteMapping("/reviews/{id}")
    public ResponseEntity<?> deleteReview(@PathVariable Long id) {
        if (!reviewRepository.existsById(id)) return ResponseEntity.notFound().build();
        reviewRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Review deleted."));
    }

    // ===== HELPERS =====
    private Room buildRoomFromBody(Room room, Map<String, Object> body) {
        if (body.get("room_number") != null) room.setRoomNumber(body.get("room_number").toString());
        if (body.get("name") != null) room.setName(body.get("name").toString());
        if (body.get("type") != null) room.setType(Room.RoomType.valueOf(body.get("type").toString().toUpperCase()));
        if (body.get("description") != null) room.setDescription(body.get("description").toString());
        if (body.get("price_per_night") != null) room.setPricePerNight(new BigDecimal(body.get("price_per_night").toString()));
        if (body.get("capacity") != null) room.setCapacity(Integer.valueOf(body.get("capacity").toString()));
        if (body.get("size_sqm") != null) room.setSizeSqm(Integer.valueOf(body.get("size_sqm").toString()));
        if (body.get("floor") != null) room.setFloor(Integer.valueOf(body.get("floor").toString()));
        if (body.get("view_type") != null) room.setViewType(body.get("view_type").toString());
        if (body.get("bed_type") != null) room.setBedType(body.get("bed_type").toString());
        if (body.get("amenities") != null) room.setAmenities(body.get("amenities").toString());
        if (body.get("images") != null) room.setImages(body.get("images").toString());
        if (body.get("is_available") != null) room.setIsAvailable(Boolean.valueOf(body.get("is_available").toString()));
        if (room.getRating() == null) room.setRating(BigDecimal.valueOf(0));
        if (room.getReviewCount() == null) room.setReviewCount(0);
        return room;
    }

    private Map<String, Object> userToMap(User u) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", u.getId()); m.put("first_name", u.getFirstName()); m.put("last_name", u.getLastName());
        m.put("email", u.getEmail()); m.put("phone", u.getPhone() != null ? u.getPhone() : "");
        m.put("country", u.getCountry() != null ? u.getCountry() : "");
        m.put("role", u.getRole().name().toLowerCase());
        m.put("created_at", u.getCreatedAt() != null ? u.getCreatedAt().toString() : "");
        return m;
    }

    private Map<String, Object> roomToMap(Room r) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", r.getId()); m.put("room_number", r.getRoomNumber()); m.put("name", r.getName());
        m.put("type", r.getType().name().toLowerCase()); m.put("description", r.getDescription() != null ? r.getDescription() : "");
        m.put("price_per_night", r.getPricePerNight()); m.put("capacity", r.getCapacity());
        m.put("size_sqm", r.getSizeSqm() != null ? r.getSizeSqm() : 0);
        m.put("floor", r.getFloor() != null ? r.getFloor() : 1);
        m.put("view_type", r.getViewType() != null ? r.getViewType() : "");
        m.put("bed_type", r.getBedType() != null ? r.getBedType() : "");
        m.put("amenities", r.getAmenities() != null ? r.getAmenities() : "[]");
        m.put("images", r.getImages() != null ? r.getImages() : "[]");
        m.put("is_available", r.getIsAvailable()); m.put("rating", r.getRating()); m.put("review_count", r.getReviewCount());
        return m;
    }

    private Map<String, Object> reservationToMap(Reservation r) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", r.getId()); m.put("confirmation_code", r.getConfirmationCode());
        m.put("user_id", r.getUser().getId()); m.put("user_name", r.getUser().getFirstName() + " " + r.getUser().getLastName());
        m.put("user_email", r.getUser().getEmail()); m.put("room_id", r.getRoom().getId());
        m.put("room_name", r.getRoom().getName()); m.put("room_type", r.getRoom().getType().name().toLowerCase());
        m.put("images", r.getRoom().getImages() != null ? r.getRoom().getImages() : "[]");
        m.put("check_in", r.getCheckIn().toString()); m.put("check_out", r.getCheckOut().toString());
        m.put("guests", r.getGuests()); m.put("total_price", r.getTotalPrice());
        m.put("status", r.getStatus().name().toLowerCase()); m.put("payment_status", r.getPaymentStatus().name().toLowerCase());
        m.put("special_requests", r.getSpecialRequests() != null ? r.getSpecialRequests() : "");
        m.put("created_at", r.getCreatedAt() != null ? r.getCreatedAt().toString() : "");
        return m;
    }

    private Map<String, Object> reviewToMap(Review r) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", r.getId()); m.put("user_id", r.getUser().getId());
        m.put("user_name", r.getUser().getFirstName() + " " + r.getUser().getLastName());
        m.put("room_id", r.getRoom().getId()); m.put("room_name", r.getRoom().getName());
        m.put("rating", r.getRating()); m.put("title", r.getTitle() != null ? r.getTitle() : "");
        m.put("comment", r.getComment() != null ? r.getComment() : "");
        m.put("status", r.getStatus().name().toLowerCase());
        m.put("created_at", r.getCreatedAt() != null ? r.getCreatedAt().toString() : "");
        return m;
    }
}
