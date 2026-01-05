package mmi.osaas.txlforma.service;

import lombok.RequiredArgsConstructor;
import mmi.osaas.txlforma.dto.FormationDTO;
import mmi.osaas.txlforma.model.Category;
import mmi.osaas.txlforma.model.Formation;
import mmi.osaas.txlforma.model.Session;
import mmi.osaas.txlforma.repository.CategoryRepository;
import mmi.osaas.txlforma.repository.FormationRepository;
import mmi.osaas.txlforma.repository.SessionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FormationService {
    private final FormationRepository formationRepository;
    private final CategoryRepository categoryRepository;
    private final SessionRepository sessionRepository;

    public List<Formation> getAllFormations() {
        return formationRepository.findAll();
    }

    public Formation getFormationById(Long id) {
        return formationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Formation introuvable"));
    }

    @Transactional
    public Formation createFormation(FormationDTO dto) {
        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Catégorie introuvable"));

        return formationRepository.save(Formation.builder()
                .title(dto.getTitle())
                .description(dto.getDescription())
                .category(category)
                .imageUrl(dto.getImageUrl())
                .createdAt(LocalDateTime.now())
                .build());
    }

    @Transactional
    public Formation updateFormation(Long id, FormationDTO dto) {
        Formation existing = getFormationById(id);

        existing.setTitle(dto.getTitle());
        existing.setDescription(dto.getDescription());

        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Catégorie introuvable"));
        existing.setCategory(category);
        existing.setImageUrl(dto.getImageUrl());

        return formationRepository.save(existing);
    }

    @Transactional
    public void deleteFormation(Long id) {
        getFormationById(id);
        
        List<Session> sessions = sessionRepository.findByFormationId(id);
        if (!sessions.isEmpty()) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT, 
                "Impossible de supprimer cette formation car elle contient " + sessions.size() + " session(s). Veuillez d'abord supprimer les sessions associées."
            );
        }
        
        formationRepository.deleteById(id);
    }

    public List<Formation> getFormationsByCategory(Long categoryId) {
        return categoryId != null 
            ? formationRepository.findByCategoryId(categoryId)
            : formationRepository.findAll();
    }
}

