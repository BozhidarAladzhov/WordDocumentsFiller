package com.example.WordDocumentsFiller.service;

import com.example.WordDocumentsFiller.entities.Container;
import com.example.WordDocumentsFiller.repositories.ContainerRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

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

}
