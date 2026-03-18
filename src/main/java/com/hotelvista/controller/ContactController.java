package com.hotelvista.controller;

import com.hotelvista.entity.ContactMessage;
import com.hotelvista.repository.ContactMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/contact")
@RequiredArgsConstructor
public class ContactController {

    private final ContactMessageRepository contactMessageRepository;

    @PostMapping
    public ResponseEntity<?> sendMessage(@RequestBody Map<String, String> body) {
        String name    = body.get("name");
        String email   = body.get("email");
        String message = body.get("message");
        if (name == null || name.isBlank() || email == null || email.isBlank() || message == null || message.isBlank())
            return ResponseEntity.badRequest().body(Map.of("message", "Name, email and message are required."));
        ContactMessage contact = new ContactMessage();
        contact.setName(name); contact.setEmail(email);
        contact.setPhone(body.get("phone")); contact.setSubject(body.get("subject"));
        contact.setMessage(message); contact.setStatus(ContactMessage.Status.NEW);
        contactMessageRepository.save(contact);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "Message sent successfully. We will get back to you soon!"));
    }
}
