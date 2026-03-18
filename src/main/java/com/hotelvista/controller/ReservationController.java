package com.hotelvista.controller;

import com.hotelvista.entity.Reservation;
import com.hotelvista.entity.Room;
import com.hotelvista.entity.User;
import com.hotelvista.repository.ReservationRepository;
import com.hotelvista.repository.RoomRepository;
import com.hotelvista.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;

    @PostMapping
    public ResponseEntity<?> create(@AuthenticationPrincipal UserDetails userDetails,
                                     @RequestBody Map<String, Object> body) {
        try {
            Long roomId    = Long.valueOf(body.get("room_id").toString());
            LocalDate checkIn  = LocalDate.parse(body.get("check_in").toString());
            LocalDate checkOut = LocalDate.parse(body.get("check_out").toString());
            int guests     = Integer.parseInt(body.get("guests").toString());
            String special = body.get("special_requests") != null ? body.get("special_requests").toString() : null;

            if (!checkOut.isAfter(checkIn))
                return ResponseEntity.badRequest().body(Map.of("message", "Check-out must be after check-in."));
            if (checkIn.isBefore(LocalDate.now()))
                return ResponseEntity.badRequest().body(Map.of("message", "Check-in cannot be in the past."));

            Room room = roomRepository.findById(roomId).orElseThrow(() -> new RuntimeException("Room not found"));
            if (!room.getIsAvailable())
                return ResponseEntity.badRequest().body(Map.of("message", "Room is not available."));
            if (guests > room.getCapacity())
                return ResponseEntity.badRequest().body(Map.of("message", "Room capacity is " + room.getCapacity() + " guests."));
            if (reservationRepository.countConflicts(roomId, checkIn, checkOut) > 0)
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "Room is not available for these dates."));

            long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
            BigDecimal total = room.getPricePerNight().multiply(BigDecimal.valueOf(nights));
            User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();

            Reservation res = new Reservation();
            res.setUser(user); res.setRoom(room);
            res.setCheckIn(checkIn); res.setCheckOut(checkOut);
            res.setGuests(guests); res.setTotalPrice(total);
            res.setSpecialRequests(special);
            res.setConfirmationCode(generateCode());
            res.setStatus(Reservation.Status.CONFIRMED);
            res.setPaymentStatus(Reservation.PaymentStatus.PENDING);
            reservationRepository.save(res);

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "message", "Reservation confirmed!", "reservation", toMap(res)));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Server error: " + e.getMessage()));
        }
    }

    @GetMapping("/my")
    public ResponseEntity<?> getMyReservations(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        List<Map<String, Object>> list = reservationRepository
                .findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream().map(this::toMap).collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getOne(@AuthenticationPrincipal UserDetails userDetails, @PathVariable Long id) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        return reservationRepository.findByIdAndUserId(id, user.getId())
                .map(r -> ResponseEntity.ok(toMap(r))).orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<?> cancel(@AuthenticationPrincipal UserDetails userDetails, @PathVariable Long id) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        Reservation res = reservationRepository.findByIdAndUserId(id, user.getId()).orElse(null);
        if (res == null) return ResponseEntity.notFound().build();
        if (res.getStatus() == Reservation.Status.CANCELLED)
            return ResponseEntity.badRequest().body(Map.of("message", "Already cancelled."));
        if (res.getStatus() == Reservation.Status.COMPLETED)
            return ResponseEntity.badRequest().body(Map.of("message", "Cannot cancel a completed stay."));
        res.setStatus(Reservation.Status.CANCELLED);
        reservationRepository.save(res);
        return ResponseEntity.ok(Map.of("message", "Reservation cancelled successfully."));
    }

    private String generateCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder("HV");
        Random rnd = new Random();
        for (int i = 0; i < 8; i++) sb.append(chars.charAt(rnd.nextInt(chars.length())));
        return sb.toString();
    }

    private Map<String, Object> toMap(Reservation r) {
        Map<String, Object> m = new HashMap<>();
        m.put("id",                r.getId());
        m.put("room_id",           r.getRoom().getId());
        m.put("room_name",         r.getRoom().getName());
        m.put("room_type",         r.getRoom().getType().name().toLowerCase());
        m.put("images",            r.getRoom().getImages() != null ? r.getRoom().getImages() : "[]");
        m.put("price_per_night",   r.getRoom().getPricePerNight());
        m.put("view_type",         r.getRoom().getViewType() != null ? r.getRoom().getViewType() : "");
        m.put("check_in",          r.getCheckIn().toString());
        m.put("check_out",         r.getCheckOut().toString());
        m.put("guests",            r.getGuests());
        m.put("total_price",       r.getTotalPrice());
        m.put("status",            r.getStatus().name().toLowerCase());
        m.put("special_requests",  r.getSpecialRequests() != null ? r.getSpecialRequests() : "");
        m.put("confirmation_code", r.getConfirmationCode());
        m.put("payment_status",    r.getPaymentStatus().name().toLowerCase());
        m.put("created_at",        r.getCreatedAt() != null ? r.getCreatedAt().toString() : "");
        return m;
    }
}
