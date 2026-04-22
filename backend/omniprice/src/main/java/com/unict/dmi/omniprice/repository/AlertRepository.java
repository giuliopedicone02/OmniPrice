package com.unict.dmi.omniprice.repository;

import com.unict.dmi.omniprice.model.Alert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertRepository extends JpaRepository<Alert, String> {
    List<Alert> findByUserId(String userId);
    List<Alert> findByUserIdAndStatus(String userId, String status);
    List<Alert> findByStatus(String status);
    List<Alert> findByProductIdAndStatus(String productId, String status);
}
