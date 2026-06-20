package com.unict.dmi.omniprice.repository;

import com.unict.dmi.omniprice.model.ProcessedMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface ProcessedMessageRepository extends JpaRepository<ProcessedMessage, String> {
    boolean existsByMessageId(String messageId);

    /**
     * Pattern Idempotent Receiver (ISD §5): i messaggi gia' elaborati vengono
     * cancellati dopo un timeout di ritenzione per evitare la crescita illimitata
     * del registro. Restituisce il numero di record rimossi.
     */
    long deleteByProcessedAtBefore(LocalDateTime cutoff);
}
