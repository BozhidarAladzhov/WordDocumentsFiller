package com.example.WordDocumentsFiller.repositories;


import com.example.WordDocumentsFiller.entities.Container;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContainerRepository extends JpaRepository<Container, Long> {

    Optional<Container> findByContainerNo (String containerNo);

    @Query("""
            select distinct c
            from Container c
            left join fetch c.vehicles v
            where c.archived = :archived
            order by c.eta asc, c.id asc, v.id asc
            """)
    List<Container> findAllByArchivedWithVehicles(@Param("archived") boolean archived);

}
