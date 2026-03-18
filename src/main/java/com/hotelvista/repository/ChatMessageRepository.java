package com.hotelvista.repository;

import com.hotelvista.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByConversationIdOrderByCreatedAtAsc(String conversationId);

    @Query("""
        SELECT DISTINCT m.conversationId FROM ChatMessage m
        WHERE m.sender.id = :userId OR m.receiver.id = :userId
        ORDER BY m.conversationId
    """)
    List<String> findConversationsByUser(@Param("userId") Long userId);

    @Query("""
        SELECT m FROM ChatMessage m
        WHERE m.conversationId = :convId
        ORDER BY m.createdAt DESC
        LIMIT 1
    """)
    ChatMessage findLastMessageByConversation(@Param("convId") String convId);

    long countByReceiverIdAndIsReadFalse(Long receiverId);

    @Query("SELECT m FROM ChatMessage m WHERE m.conversationId = :convId ORDER BY m.createdAt ASC")
    List<ChatMessage> findAllByConversation(@Param("convId") String convId);
}
