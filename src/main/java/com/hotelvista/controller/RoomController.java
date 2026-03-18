package com.hotelvista.controller;

import com.hotelvista.entity.Room;
import com.hotelvista.repository.ReservationRepository;
import com.hotelvista.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomRepository roomRepository;
    private final ReservationRepository reservationRepository;

    @GetMapping
    public ResponseEntity<?> getRooms(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) BigDecimal min_price,
            @RequestParam(required = false) BigDecimal max_price,
            @RequestParam(required = false) Integer capacity,
            @RequestParam(required = false) String view,
            @RequestParam(required = false) String check_in,
            @RequestParam(required = false) String check_out,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "9") int limit) {

        Room.RoomType roomType = null;
        if (type != null && !type.isEmpty()) {
            try { roomType = Room.RoomType.valueOf(type.toUpperCase()); }
            catch (IllegalArgumentException ignored) {}
        }
        LocalDate checkInDate  = (check_in  != null && !check_in.isEmpty())  ? LocalDate.parse(check_in)  : LocalDate.of(2000,1,1);
        LocalDate checkOutDate = (check_out != null && !check_out.isEmpty()) ? LocalDate.parse(check_out) : LocalDate.of(2100,1,1);

        Page<Room> roomPage = roomRepository.findAvailableRooms(
            roomType, min_price, max_price, capacity, view, checkInDate, checkOutDate,
            PageRequest.of(page - 1, limit));

        List<Map<String, Object>> roomList = roomPage.getContent().stream()
            .map(this::toMap).collect(Collectors.toList());

        return ResponseEntity.ok(Map.of(
            "rooms", roomList, "total", roomPage.getTotalElements(),
            "page", page, "totalPages", roomPage.getTotalPages()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getRoom(@PathVariable Long id) {
        return roomRepository.findById(id)
            .map(room -> ResponseEntity.ok(toMap(room)))
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/availability")
    public ResponseEntity<?> checkAvailability(@PathVariable Long id,
            @RequestParam String check_in, @RequestParam String check_out) {
        long conflicts = reservationRepository.countConflicts(id, LocalDate.parse(check_in), LocalDate.parse(check_out));
        return ResponseEntity.ok(Map.of("available", conflicts == 0));
    }

    private Map<String, Object> toMap(Room r) {
        Map<String, Object> m = new HashMap<>();
        m.put("id",              r.getId());
        m.put("room_number",     r.getRoomNumber());
        m.put("name",            r.getName());
        m.put("type",            r.getType().name().toLowerCase());
        m.put("description",     r.getDescription()   != null ? r.getDescription()   : "");
        m.put("price_per_night", r.getPricePerNight());
        m.put("capacity",        r.getCapacity());
        m.put("size_sqm",        r.getSizeSqm()       != null ? r.getSizeSqm()       : 0);
        m.put("floor",           r.getFloor()         != null ? r.getFloor()         : 1);
        m.put("view_type",       r.getViewType()      != null ? r.getViewType()      : "");
        m.put("bed_type",        r.getBedType()       != null ? r.getBedType()       : "");
        m.put("amenities",       r.getAmenities()     != null ? r.getAmenities()     : "[]");
        m.put("images",          r.getImages()        != null ? r.getImages()        : "[]");
        m.put("is_available",    r.getIsAvailable());
        m.put("rating",          r.getRating());
        m.put("review_count",    r.getReviewCount());
        return m;
    }
}
