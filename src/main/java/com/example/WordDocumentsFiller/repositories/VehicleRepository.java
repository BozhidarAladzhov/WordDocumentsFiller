package com.example.WordDocumentsFiller.repositories;

import com.example.WordDocumentsFiller.entities.Vehicle;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    List<Vehicle> findByContainerIdOrderByIdAsc(Long containerId);

    @Query("""
           select v
           from Vehicle v
           join fetch v.container c
           where c.archived = false
           order by
             case when coalesce(v.eta, c.eta) is null then 1 else 0 end,
             coalesce(v.eta, c.eta) asc,
             c.containerNo asc,
             v.id desc
           """)
    List<Vehicle> findAllWithContainerOrderByContainerNoAndIdDesc();

}
