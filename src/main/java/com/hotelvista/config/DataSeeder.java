package com.hotelvista.config;

import com.hotelvista.entity.Room;
import com.hotelvista.entity.User;
import com.hotelvista.repository.RoomRepository;
import com.hotelvista.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Random random = new Random(42);

    @Override
    public void run(String... args) {
        seedAdmin();
        if (roomRepository.count() == 0) {
            seedRooms();
            System.out.println("✅ Seeded " + roomRepository.count() + " rooms");
        }
        System.out.println("✅ Database initialized successfully");
        System.out.println("👤 Admin login: admin@hotelvista.com / admin123");
    }

    private void seedAdmin() {
        if (!userRepository.existsByEmail("admin@hotelvista.com")) {
            User admin = new User();
            admin.setFirstName("Admin");
            admin.setLastName("HotelVista");
            admin.setEmail("admin@hotelvista.com");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setRole(User.Role.ADMIN);
            admin.setPhone("+33 1 23 45 67 89");
            admin.setCountry("France");
            userRepository.save(admin);
            System.out.println("✅ Admin user created: admin@hotelvista.com / admin123");
        }
    }

    private void seedRooms() {
        List<Room> rooms = new ArrayList<>();
        int roomNum = 101;

        for (int i = 0; i < 10; i++) rooms.add(createRoom(roomNum++, "Standard Room " + (i+1), Room.RoomType.STANDARD, 89+random.nextInt(40), 2, 22+random.nextInt(6), 2+random.nextInt(5), i%2==0?"city":"garden", i%2==0?"Queen Bed":"Double Bed", "[\"WiFi\",\"TV\",\"Air Conditioning\",\"Mini Bar\",\"Safe\",\"Hair Dryer\"]", "[\"https://images.unsplash.com/photo-1631049307264-da0ec9d70304?w=800\",\"https://images.unsplash.com/photo-1618773928121-c32242e63f39?w=800\"]"));
        for (int i = 0; i < 10; i++) rooms.add(createRoom(roomNum++, "Deluxe Room " + (i+1), Room.RoomType.DELUXE, 149+random.nextInt(50), 2, 32+random.nextInt(6), 5+random.nextInt(5), i%3==0?"sea":(i%3==1?"city":"garden"), "King Bed", "[\"WiFi\",\"Smart TV\",\"Air Conditioning\",\"Mini Bar\",\"Safe\",\"Hair Dryer\",\"Bathtub\",\"Coffee Machine\"]", "[\"https://images.unsplash.com/photo-1598928506311-c55ded91a20c?w=800\",\"https://images.unsplash.com/photo-1578683010236-d716f9a3f461?w=800\"]"));
        for (int i = 0; i < 10; i++) rooms.add(createRoom(roomNum++, "Family Room " + (i+1), Room.RoomType.FAMILY, 179+random.nextInt(70), 4, 42+random.nextInt(10), 3+random.nextInt(4), i%2==0?"garden":"pool", "King Bed + Twin Beds", "[\"WiFi\",\"Smart TV\",\"Air Conditioning\",\"Mini Bar\",\"Safe\",\"Hair Dryer\",\"Sofa\",\"Kids Amenities\"]", "[\"https://images.unsplash.com/photo-1520250497591-112f2f40a3f4?w=800\",\"https://images.unsplash.com/photo-1591088398332-8a7791972843?w=800\"]"));
        for (int i = 0; i < 8; i++)  rooms.add(createRoom(roomNum++, "Executive Room " + (i+1), Room.RoomType.EXECUTIVE, 229+random.nextInt(70), 2, 42+random.nextInt(10), 8+random.nextInt(4), i%2==0?"sea":"city panoramic", "King Bed", "[\"WiFi\",\"Smart TV\",\"Air Conditioning\",\"Mini Bar\",\"Safe\",\"Bathtub\",\"Lounge Access\",\"Coffee Machine\",\"Work Desk\"]", "[\"https://images.unsplash.com/photo-1590490360182-c33d57733427?w=800\",\"https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?w=800\"]"));
        for (int i = 0; i < 6; i++)  rooms.add(createRoom(roomNum++, "Suite " + (i+1), Room.RoomType.SUITE, 349+random.nextInt(150), 3, 65+random.nextInt(20), 12+random.nextInt(3), i%2==0?"sea panoramic":"city panoramic", "King Bed", "[\"WiFi\",\"Smart TV\",\"Climate Control\",\"Full Bar\",\"Safe\",\"Jacuzzi\",\"Living Room\",\"Butler Service\",\"Dining Area\"]", "[\"https://images.unsplash.com/photo-1571896349842-33c89424de2d?w=800\",\"https://images.unsplash.com/photo-1578774296842-c45e472b3028?w=800\"]"));
        for (int i = 0; i < 3; i++)  rooms.add(createRoom(roomNum++, "Penthouse " + (i+1), Room.RoomType.PENTHOUSE, 699+random.nextInt(300), 4, 120+random.nextInt(80), 15, "360° panoramic", "King Bed", "[\"WiFi\",\"Smart TVs\",\"Climate Control\",\"Full Kitchen\",\"Safe\",\"Private Pool\",\"Terrace\",\"Butler Service\",\"Home Theater\"]", "[\"https://images.unsplash.com/photo-1582719508461-905c673771fd?w=800\",\"https://images.unsplash.com/photo-1630699144867-37acec97df5a?w=800\"]"));

        roomRepository.saveAll(rooms);
    }

    private Room createRoom(int number, String name, Room.RoomType type, int price, int capacity, int size, int floor, String view, String bed, String amenities, String images) {
        Room room = new Room();
        room.setRoomNumber(String.valueOf(number));
        room.setName(name);
        room.setType(type);
        room.setDescription(name + " offering " + view + " view. " + size + "m² with " + bed + ".");
        room.setPricePerNight(BigDecimal.valueOf(price));
        room.setCapacity(capacity);
        room.setSizeSqm(size);
        room.setFloor(floor);
        room.setViewType(view);
        room.setBedType(bed);
        room.setAmenities(amenities);
        room.setImages(images);
        room.setIsAvailable(true);
        room.setRating(BigDecimal.valueOf(3.5 + random.nextDouble() * 1.5).setScale(2, RoundingMode.HALF_UP));
        room.setReviewCount(20 + random.nextInt(280));
        return room;
    }
}
