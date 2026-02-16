package com.example.WordDocumentsFiller.service;

import com.example.WordDocumentsFiller.entities.Party;
import com.example.WordDocumentsFiller.entities.enums.PartyType;
import com.example.WordDocumentsFiller.repositories.PartyRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PartyService {

    private final PartyRepository partyRepository;

    public PartyService(PartyRepository partyRepository) {
        this.partyRepository = partyRepository;
    }

    @Transactional
    public List<Party> getByType(PartyType type) {
        return partyRepository.findByTypeOrderByNameAscAddressAsc(type);
    }

    @Transactional
    public Party getById(Long id) {
        return partyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Party not found: " + id));
    }

    @Transactional
    public Party resolveAndMaybeCreate(PartyType type,
                                       Long id,
                                       String name,
                                       String address,
                                       String town,
                                       String country) {
        if (id != null) {
            Party existing = getById(id);
            if (existing.getType() != type) {
                throw new IllegalArgumentException("Party type mismatch for id=" + id);
            }
            return existing;
        }

        String safeName = safe(name);
        String safeAddress = safe(address);
        String safeTown = safe(town);
        String safeCountry = safe(country);
        if (safeName.isBlank() || safeAddress.isBlank()) {
            return null;
        }

        var exact = partyRepository.findFirstByTypeAndNameIgnoreCaseAndAddressIgnoreCaseAndTownIgnoreCaseAndCountryIgnoreCase(
                type, safeName, safeAddress, safeTown, safeCountry
        );
        if (exact.isPresent()) {
            return exact.get();
        }

        // Backward compatibility with older DB unique key (type+name+address).
        // If such record exists, enrich it with town/country instead of trying to insert duplicate.
        var legacy = partyRepository.findFirstByTypeAndNameIgnoreCaseAndAddressIgnoreCase(
                type, safeName, safeAddress
        );
        if (legacy.isPresent()) {
            Party existing = legacy.get();
            boolean changed = false;

            if ((existing.getTown() == null || existing.getTown().isBlank()) && !safeTown.isBlank()) {
                existing.setTown(safeTown);
                changed = true;
            }
            if ((existing.getCountry() == null || existing.getCountry().isBlank()) && !safeCountry.isBlank()) {
                existing.setCountry(safeCountry);
                changed = true;
            }

            return changed ? partyRepository.save(existing) : existing;
        }

        Party created = new Party();
        created.setType(type);
        created.setName(safeName);
        created.setAddress(safeAddress);
        created.setTown(safeTown);
        created.setCountry(safeCountry);
        return partyRepository.save(created);
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
