package com.example.WordDocumentsFiller.service;

import com.example.WordDocumentsFiller.entities.UpClient;
import com.example.WordDocumentsFiller.repositories.UpClientRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UpClientService {

    private final UpClientRepository upClientRepository;

    public UpClientService(UpClientRepository upClientRepository) {
        this.upClientRepository = upClientRepository;
    }

    @Transactional
    public List<UpClient> getAll() {
        return upClientRepository.findAllByOrderByNameAscAddressAsc();
    }

    @Transactional
    public UpClient getById(Long id) {
        return upClientRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("UP client not found: " + id));
    }

    @Transactional
    public UpClient resolveAndMaybeCreate(Long id,
                                          String name,
                                          String address,
                                          String town,
                                          String country,
                                          String eori) {
        if (id != null) {
            return getById(id);
        }

        String safeName = safe(name);
        String safeAddress = safe(address);
        String safeTown = safe(town);
        String safeCountry = safe(country);
        String safeEori = safe(eori);

        if (safeName.isBlank() || safeAddress.isBlank()) {
            return null;
        }

        var exact = upClientRepository.findFirstByNameIgnoreCaseAndAddressIgnoreCaseAndTownIgnoreCaseAndCountryIgnoreCaseAndEoriIgnoreCase(
                safeName, safeAddress, safeTown, safeCountry, safeEori
        );
        if (exact.isPresent()) {
            return exact.get();
        }

        var legacy = upClientRepository.findFirstByNameIgnoreCaseAndAddressIgnoreCase(safeName, safeAddress);
        if (legacy.isPresent()) {
            UpClient existing = legacy.get();
            boolean changed = false;
            if ((existing.getTown() == null || existing.getTown().isBlank()) && !safeTown.isBlank()) {
                existing.setTown(safeTown);
                changed = true;
            }
            if ((existing.getCountry() == null || existing.getCountry().isBlank()) && !safeCountry.isBlank()) {
                existing.setCountry(safeCountry);
                changed = true;
            }
            if ((existing.getEori() == null || existing.getEori().isBlank()) && !safeEori.isBlank()) {
                existing.setEori(safeEori);
                changed = true;
            }
            return changed ? upClientRepository.save(existing) : existing;
        }

        UpClient created = new UpClient();
        created.setName(safeName);
        created.setAddress(safeAddress);
        created.setTown(safeTown);
        created.setCountry(safeCountry);
        created.setEori(safeEori);
        return upClientRepository.save(created);
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
