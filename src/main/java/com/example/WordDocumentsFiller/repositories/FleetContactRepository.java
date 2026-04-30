package com.example.WordDocumentsFiller.repositories;

import com.example.WordDocumentsFiller.entities.FleetContact;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FleetContactRepository extends JpaRepository<FleetContact, Long> {

    List<FleetContact> findAllByOrderByCategoryAscNameAscIdAsc();
}
