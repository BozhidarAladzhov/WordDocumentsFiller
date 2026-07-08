package com.example.WordDocumentsFiller.repositories;

import com.example.WordDocumentsFiller.entities.CanadianVehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CanadianVehicleRepository extends JpaRepository<CanadianVehicle, Long> {

    @Query("""
           select v
           from CanadianVehicle v
           order by v.id asc
           """)
    List<CanadianVehicle> findAllForOverview();
}
