package com.example.WordDocumentsFiller.service;

import com.example.WordDocumentsFiller.entities.Container;
import com.example.WordDocumentsFiller.entities.enums.ContainerStatus;
import com.example.WordDocumentsFiller.repositories.ContainerRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class ContainerService {

    private final ContainerRepository containerRepository;

    public ContainerService(ContainerRepository containerRepository) {
        this.containerRepository = containerRepository;
    }

    @Transactional()
    public List<Container> getAll() {
        return containerRepository.findAll();
    }

    @Transactional()
    public Container getById(Long id) {
        return containerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Container not found: " + id));
    }

    @Transactional
    public Container create(Container container) {
        return containerRepository.save(container);
    }

    @Transactional
    public void updateContainer(Long id,
                                String bol,
                                String carrier,
                                LocalDate eta,
                                ContainerStatus status) {

        Container c = getById(id);
        c.setBol(bol);
        c.setCarrier(carrier);
        c.setEta(eta);
        c.setStatus(status);

        containerRepository.save(c);
    }

    @Transactional
    public void deleteContainer(Long id) {
        containerRepository.deleteById(id);
    }


}
