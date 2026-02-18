package com.example.WordDocumentsFiller.service;

import com.example.WordDocumentsFiller.entities.Container;
import com.example.WordDocumentsFiller.entities.enums.ContainerStatus;
import com.example.WordDocumentsFiller.repositories.ContainerRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Sort;
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
    public List<Container> getActive() {
        return containerRepository.findByArchivedFalse(
                Sort.by(Sort.Direction.ASC, "eta")
        );
    }

    @Transactional()
    public List<Container> getArchived() {
        return containerRepository.findByArchivedTrue(
                Sort.by(Sort.Direction.DESC, "eta")
        );
    }

    @Transactional()
    public Container getById(Long id) {
        return containerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Container not found: " + id));
    }

    @Transactional
    public Container create(Container container) {
        container.setArchived(false);
        return containerRepository.save(container);
    }

    @Transactional
    public void updateContainer(Long id,
                                String bol,
                                String carrier,
                                String vesselName,
                                String pol,
                                String pod,
                                String seal,
                                String shippedOnBoard,
                                LocalDate eta,
                                ContainerStatus status) {

        Container c = getById(id);
        c.setBol(bol);
        c.setCarrier(carrier);
        c.setVesselName(vesselName);
        c.setPol(pol);
        c.setPod(pod);
        c.setSeal(seal);
        c.setShippedOnBoard(shippedOnBoard);
        c.setEta(eta);
        c.setStatus(status);

        containerRepository.save(c);
    }

    @Transactional
    public void deleteContainer(Long id) {
        containerRepository.deleteById(id);
    }

    @Transactional
    public void archiveContainer(Long id) {
        Container c = getById(id);
        c.setArchived(true);
        containerRepository.save(c);
    }

    @Transactional
    public void unarchiveContainer(Long id) {
        Container c = getById(id);
        c.setArchived(false);
        containerRepository.save(c);
    }


}
