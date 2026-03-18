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
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentRepository paymentRepository;
    private final ReservationRepository reservationRepository;
    private final UserRepository userRepository;

    // POST /api/payments/pay/:reservationId
    @PostMapping("/pay/{reservationId}")
    public ResponseEntity<?> pay(@AuthenticationPrincipal UserDetails userDetails,
                                  @PathVariable Long reservationId,
                                  @RequestBody Map<String, String> body) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        Reservation res = reservationRepository.findById(reservationId).orElse(null);

        if (res == null) return ResponseEntity.notFound().build();
        if (!res.getUser().getId().equals(user.getId()))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Not authorized."));
        if (res.getStatus() != Reservation.Status.CONFIRMED)
            return ResponseEntity.badRequest().body(Map.of("message", "Reservation must be confirmed by admin before payment."));
        if (paymentRepository.existsByReservationId(reservationId))
            return ResponseEntity.badRequest().body(Map.of("message", "This reservation has already been paid."));

        String method = body.getOrDefault("method", "CARD").toUpperCase();

        Payment payment = new Payment();
        payment.setReservation(res);
        payment.setUser(user);
        payment.setAmount(res.getTotalPrice());
        payment.setMethod(Payment.Method.valueOf(method));
        payment.setStatus(Payment.Status.COMPLETED);
        payment.setTransactionId("TXN" + System.currentTimeMillis());
        paymentRepository.save(payment);

        // Update reservation payment status
        res.setPaymentStatus(Reservation.PaymentStatus.PAID);
        reservationRepository.save(res);

        return ResponseEntity.ok(Map.of(
            "message", "Payment successful!",
            "transaction_id", payment.getTransactionId(),
            "amount", payment.getAmount(),
            "method", payment.getMethod().name()
        ));
    }

    // GET /api/payments/my
    @GetMapping("/my")
    public ResponseEntity<?> getMyPayments(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        List<Map<String, Object>> list = paymentRepository.findByUserIdOrderByPaidAtDesc(user.getId())
            .stream().map(this::toMap).collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    // GET /api/payments/reservation/:id
    @GetMapping("/reservation/{reservationId}")
    public ResponseEntity<?> getPaymentByReservation(@AuthenticationPrincipal UserDetails userDetails,
                                                      @PathVariable Long reservationId) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        return paymentRepository.findByReservationId(reservationId)
            .map(p -> ResponseEntity.ok(toMap(p)))
            .orElse(ResponseEntity.notFound().build());
    }

    private Map<String, Object> toMap(Payment p) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", p.getId());
        m.put("reservation_id", p.getReservation().getId());
        m.put("confirmation_code", p.getReservation().getConfirmationCode());
        m.put("room_name", p.getReservation().getRoom().getName());
        m.put("amount", p.getAmount());
        m.put("method", p.getMethod().name().toLowerCase());
        m.put("status", p.getStatus().name().toLowerCase());
        m.put("transaction_id", p.getTransactionId());
        m.put("paid_at", p.getPaidAt() != null ? p.getPaidAt().toString() : "");
        return m;
    }
}
