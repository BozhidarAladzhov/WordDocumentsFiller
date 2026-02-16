package com.example.WordDocumentsFiller.repositories;

import com.example.WordDocumentsFiller.entities.Party;
import com.example.WordDocumentsFiller.entities.enums.PartyType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PartyRepository extends JpaRepository<Party, Long> {

    List<Party> findByTypeOrderByNameAscAddressAsc(PartyType type);

    Optional<Party> findFirstByTypeAndNameIgnoreCaseAndAddressIgnoreCase(
            PartyType type,
            String name,
            String address
    );

    Optional<Party> findFirstByTypeAndNameIgnoreCaseAndAddressIgnoreCaseAndTownIgnoreCaseAndCountryIgnoreCase(
            PartyType type,
            String name,
            String address,
            String town,
            String country
    );
}
