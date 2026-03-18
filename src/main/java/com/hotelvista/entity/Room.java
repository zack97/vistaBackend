package com.hotelvista.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "rooms")
@Data
@NoArgsConstructor
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_number", unique = true, nullable = false, length = 20)
    private String roomNumber;

    @Column(nullable = false, length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RoomType type;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "price_per_night", nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePerNight;

    @Column(nullable = false)
    private Integer capacity = 2;

    @Column(name = "size_sqm")
    private Integer sizeSqm;

    @Column
    private Integer floor;

    @Column(name = "view_type", length = 100)
    private String viewType;

    @Column(name = "bed_type", length = 100)
    private String bedType;

    @Column(columnDefinition = "JSON")
    private String amenities;

    @Column(columnDefinition = "JSON")
    private String images;

    @Column(name = "is_available")
    private Boolean isAvailable = true;

    @Column(precision = 3, scale = 2)
    private BigDecimal rating = BigDecimal.ZERO;

    @Column(name = "review_count")
    private Integer reviewCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Reservation> reservations;

    public enum RoomType { STANDARD, DELUXE, SUITE, PENTHOUSE, FAMILY, EXECUTIVE }
}
