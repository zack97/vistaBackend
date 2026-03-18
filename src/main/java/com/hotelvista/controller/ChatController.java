package com.hotelvista.controller;

import com.hotelvista.entity.ChatMessage;
import com.hotelvista.entity.User;
import com.hotelvista.repository.ChatMessageRepository;
import com.hotelvista.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;

    private String buildConvId(Long a, Long b) {
        return Math.min(a, b) + "_" + Math.max(a, b);
    }

    // POST /api/chat/send
    @PostMapping("/send")
    public ResponseEntity<?> sendMessage(@AuthenticationPrincipal UserDetails userDetails,
                                         @RequestBody Map<String, Object> body) {
        User sender = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        Long receiverId = Long.valueOf(body.get("receiver_id").toString());
        String content  = body.get("content")      != null ? body.get("content").toString()      : null;
        String fileData = body.get("file_data")     != null ? body.get("file_data").toString()     : null;
        String fileName = body.get("file_name")     != null ? body.get("file_name").toString()     : null;
        String fileType = body.get("file_type")     != null ? body.get("file_type").toString()     : null;
        String typeStr  = body.get("message_type")  != null ? body.get("message_type").toString()  : "TEXT";

        User receiver = userRepository.findById(receiverId).orElse(null);
        if (receiver == null) return ResponseEntity.notFound().build();

        ChatMessage msg = new ChatMessage();
        msg.setSender(sender);
        msg.setReceiver(receiver);
        msg.setContent(content);
        msg.setFileData(fileData);
        msg.setFileName(fileName);
        msg.setFileType(fileType);
        msg.setMessageType(ChatMessage.MessageType.valueOf(typeStr.toUpperCase()));
        msg.setIsRead(false);
        msg.setIsDeleted(false);
        msg.setIsEdited(false);
        msg.setConversationId(buildConvId(sender.getId(), receiverId));
        chatMessageRepository.save(msg);
        return ResponseEntity.ok(toMap(msg));
    }

    // PUT /api/chat/messages/:id — modifier un message (seulement si pas encore lu)
    @PutMapping("/messages/{id}")
    public ResponseEntity<?> editMessage(@AuthenticationPrincipal UserDetails userDetails,
                                         @PathVariable Long id,
                                         @RequestBody Map<String, String> body) {
        User me = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        ChatMessage msg = chatMessageRepository.findById(id).orElse(null);
        if (msg == null) return ResponseEntity.notFound().build();
        if (!msg.getSender().getId().equals(me.getId()))
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Vous ne pouvez modifier que vos propres messages."));
        if (msg.getIsRead())
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Impossible de modifier un message déjà lu."));
        if (msg.getIsDeleted())
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Impossible de modifier un message supprimé."));
        String newContent = body.get("content");
        if (newContent == null || newContent.isBlank())
            return ResponseEntity.badRequest().body(Map.of("message", "Le contenu ne peut pas être vide."));
        msg.setContent(newContent);
        msg.setIsEdited(true);
        chatMessageRepository.save(msg);
        return ResponseEntity.ok(toMap(msg));
    }

    // DELETE /api/chat/messages/:id — supprimer un message
    @DeleteMapping("/messages/{id}")
    public ResponseEntity<?> deleteMessage(@AuthenticationPrincipal UserDetails userDetails,
                                           @PathVariable Long id) {
        User me = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        ChatMessage msg = chatMessageRepository.findById(id).orElse(null);
        if (msg == null) return ResponseEntity.notFound().build();
        if (!msg.getSender().getId().equals(me.getId()) && me.getRole() != User.Role.ADMIN)
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Non autorisé."));
        msg.setIsDeleted(true);
        msg.setContent("🗑 Ce message a été supprimé");
        msg.setFileData(null);
        msg.setFileName(null);
        chatMessageRepository.save(msg);
        return ResponseEntity.ok(Map.of("message", "Message supprimé."));
    }

    // DELETE /api/chat/messages/bulk — supprimer plusieurs messages
    @DeleteMapping("/messages/bulk")
    public ResponseEntity<?> deleteMessages(@AuthenticationPrincipal UserDetails userDetails,
                                            @RequestBody Map<String, Object> body) {
        User me = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        @SuppressWarnings("unchecked")
        List<Integer> ids = (List<Integer>) body.get("ids");
        if (ids == null || ids.isEmpty())
            return ResponseEntity.badRequest().body(Map.of("message", "Aucun ID fourni."));
        int count = 0;
        for (Integer rawId : ids) {
            ChatMessage msg = chatMessageRepository.findById(rawId.longValue()).orElse(null);
            if (msg == null) continue;
            if (!msg.getSender().getId().equals(me.getId()) && me.getRole() != User.Role.ADMIN) continue;
            msg.setIsDeleted(true);
            msg.setContent("🗑 Ce message a été supprimé");
            msg.setFileData(null);
            msg.setFileName(null);
            chatMessageRepository.save(msg);
            count++;
        }
        return ResponseEntity.ok(Map.of("message", count + " message(s) supprimé(s).", "count", count));
    }

    // GET /api/chat/conversation/:userId
    @GetMapping("/conversation/{userId}")
    public ResponseEntity<?> getConversation(@AuthenticationPrincipal UserDetails userDetails,
                                             @PathVariable Long userId) {
        User me = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        String convId = buildConvId(me.getId(), userId);
        List<ChatMessage> msgs = chatMessageRepository.findAllByConversation(convId);
        msgs.stream()
                .filter(m -> m.getReceiver().getId().equals(me.getId()) && !m.getIsRead())
                .forEach(m -> { m.setIsRead(true); chatMessageRepository.save(m); });
        return ResponseEntity.ok(msgs.stream().map(this::toMap).collect(Collectors.toList()));
    }

    // GET /api/chat/conversations
    @GetMapping("/conversations")
    public ResponseEntity<?> getConversations(@AuthenticationPrincipal UserDetails userDetails) {
        User me = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        List<String> convIds = chatMessageRepository.findConversationsByUser(me.getId());
        List<Map<String, Object>> result = new ArrayList<>();
        for (String convId : convIds) {
            List<ChatMessage> msgs = chatMessageRepository.findAllByConversation(convId);
            if (msgs.isEmpty()) continue;
            ChatMessage last = msgs.get(msgs.size() - 1);
            User other = last.getSender().getId().equals(me.getId()) ? last.getReceiver() : last.getSender();
            long unread = msgs.stream()
                    .filter(m -> m.getReceiver().getId().equals(me.getId()) && !m.getIsRead())
                    .count();
            String lastContent = last.getIsDeleted() ? "🗑 Message supprimé" :
                    last.getMessageType() == ChatMessage.MessageType.FILE ?
                            "📎 " + (last.getFileName() != null ? last.getFileName() : "Fichier") :
                            last.getContent();
            Map<String, Object> conv = new HashMap<>();
            conv.put("conversation_id", convId);
            conv.put("other_user_id", other.getId());
            conv.put("other_user_name", other.getFirstName() + " " + other.getLastName());
            conv.put("other_user_role", other.getRole().name().toLowerCase());
            conv.put("last_message", lastContent);
            conv.put("last_message_time", last.getCreatedAt().toString());
            conv.put("unread_count", unread);
            result.add(conv);
        }
        result.sort((a, b) -> b.get("last_message_time").toString()
                .compareTo(a.get("last_message_time").toString()));
        return ResponseEntity.ok(result);
    }

    // GET /api/chat/unread-count
    @GetMapping("/unread-count")
    public ResponseEntity<?> getUnreadCount(@AuthenticationPrincipal UserDetails userDetails) {
        User me = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        long count = chatMessageRepository.countByReceiverIdAndIsReadFalse(me.getId());
        return ResponseEntity.ok(Map.of("unread", count));
    }

    // GET /api/chat/admin-id
    @GetMapping("/admin-id")
    public ResponseEntity<?> getAdminId() {
        return userRepository.findAll().stream()
                .filter(u -> u.getRole() == User.Role.ADMIN)
                .findFirst()
                .map(u -> ResponseEntity.ok(Map.of(
                        "admin_id", u.getId(),
                        "admin_name", u.getFirstName() + " " + u.getLastName())))
                .orElse(ResponseEntity.notFound().build());
    }

    private Map<String, Object> toMap(ChatMessage m) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", m.getId());
        map.put("sender_id", m.getSender().getId());
        map.put("sender_name", m.getSender().getFirstName() + " " + m.getSender().getLastName());
        map.put("sender_role", m.getSender().getRole().name().toLowerCase());
        map.put("receiver_id", m.getReceiver().getId());
        map.put("receiver_name", m.getReceiver().getFirstName() + " " + m.getReceiver().getLastName());
        map.put("content", m.getContent() != null ? m.getContent() : "");
        map.put("file_data", m.getFileData() != null && !m.getIsDeleted() ? m.getFileData() : "");
        map.put("file_name", m.getFileName() != null ? m.getFileName() : "");
        map.put("file_type", m.getFileType() != null ? m.getFileType() : "");
        map.put("message_type", m.getMessageType() != null ? m.getMessageType().name().toLowerCase() : "text");
        map.put("is_read", m.getIsRead());
        map.put("is_deleted", m.getIsDeleted());
        map.put("is_edited", m.getIsEdited());
        map.put("conversation_id", m.getConversationId());
        map.put("created_at", m.getCreatedAt() != null ? m.getCreatedAt().toString() : "");
        map.put("updated_at", m.getUpdatedAt() != null ? m.getUpdatedAt().toString() : "");
        return map;
    }
}