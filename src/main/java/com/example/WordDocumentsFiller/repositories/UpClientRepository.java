package com.example.WordDocumentsFiller.repositories;

import com.example.WordDocumentsFiller.entities.UpClient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UpClientRepository extends JpaRepository<UpClient, Long> {

    List<UpClient> findAllByOrderByNameAscAddressAsc();

    Optional<UpClient> findFirstByNameIgnoreCaseAndAddressIgnoreCaseAndTownIgnoreCaseAndCountryIgnoreCaseAndEoriIgnoreCase(
            String name,
            String address,
            String town,
            String country,
            String eori
    );

    Optional<UpClient> findFirstByNameIgnoreCaseAndAddressIgnoreCase(
            String name,
            String address
    );
}
