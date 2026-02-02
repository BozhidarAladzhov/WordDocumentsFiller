package com.example.WordDocumentsFiller.repositories;


import com.example.WordDocumentsFiller.entities.Container;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ContainerRepository extends JpaRepository<Container, Long> {

    Optional<Container> findByContainerNo (String containerNo);

}
