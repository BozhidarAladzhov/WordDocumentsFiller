package com.example.WordDocumentsFiller.service;

import com.example.WordDocumentsFiller.entities.FleetContact;
import com.example.WordDocumentsFiller.repositories.FleetContactRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FleetContactService {

    private final FleetContactRepository fleetContactRepository;

    public FleetContactService(FleetContactRepository fleetContactRepository) {
        this.fleetContactRepository = fleetContactRepository;
    }

    public List<FleetContact> getAll() {
        return fleetContactRepository.findAllByOrderByCategoryAscNameAscIdAsc();
    }

    public FleetContact create(FleetContact source) {
        FleetContact contact = new FleetContact();
        apply(contact, source);
        return fleetContactRepository.save(contact);
    }

    public FleetContact update(Long id, FleetContact source) {
        FleetContact target = getById(id);
        apply(target, source);
        return fleetContactRepository.save(target);
    }

    public void delete(Long id) {
        fleetContactRepository.delete(getById(id));
    }

    private FleetContact getById(Long id) {
        return fleetContactRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Fleet contact not found: " + id));
    }

    private void apply(FleetContact target, FleetContact source) {
        target.setName(normalize(source.getName()));
        target.setPhone(normalize(source.getPhone()));
        target.setCategory(normalize(source.getCategory()));
    }

    private String normalize(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
