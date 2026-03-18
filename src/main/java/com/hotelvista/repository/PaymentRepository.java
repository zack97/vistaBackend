package com.hotelvista.repository;

import com.hotelvista.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByReservationId(Long reservationId);
    List<Payment> findByUserIdOrderByPaidAtDesc(Long userId);
    boolean existsByReservationId(Long reservationId);
}
