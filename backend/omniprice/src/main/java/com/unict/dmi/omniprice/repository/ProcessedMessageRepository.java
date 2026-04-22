package com.unict.dmi.omniprice.repository;

import com.unict.dmi.omniprice.model.ProcessedMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProcessedMessageRepository extends JpaRepository<ProcessedMessage, String> {
    boolean existsByMessageId(String messageId);
}
