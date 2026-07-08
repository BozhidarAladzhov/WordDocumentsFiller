package com.example.WordDocumentsFiller.repositories;

import com.example.WordDocumentsFiller.entities.CanadianVehiclePaymentOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CanadianVehiclePaymentOrderRepository extends JpaRepository<CanadianVehiclePaymentOrder, Long> {

    List<CanadianVehiclePaymentOrder> findByCanadianVehicleIdOrderByUploadedAtAscIdAsc(Long canadianVehicleId);
}
