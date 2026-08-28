package com.example.WordDocumentsFiller.repositories;

import com.example.WordDocumentsFiller.entities.CanadianVehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface CanadianVehicleRepository extends JpaRepository<CanadianVehicle, Long> {

    @Query("""
           select v
           from CanadianVehicle v
           order by v.id asc
           """)
    List<CanadianVehicle> findAllForOverview();

    @Query("""
           select v
           from CanadianVehicle v
           where v.paymentDate is not null
             and (:exactDate is null or v.paymentDate = :exactDate)
             and (:fromDate is null or v.paymentDate >= :fromDate)
             and (:toDate is null or v.paymentDate <= :toDate)
           order by v.paymentDate asc, v.id asc
           """)
    List<CanadianVehicle> findByPaymentDateFilter(@Param("exactDate") LocalDate exactDate,
                                                  @Param("fromDate") LocalDate fromDate,
                                                  @Param("toDate") LocalDate toDate);
}
