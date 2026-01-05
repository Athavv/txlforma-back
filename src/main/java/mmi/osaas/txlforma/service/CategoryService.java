package mmi.osaas.txlforma.service;

import lombok.RequiredArgsConstructor;
import mmi.osaas.txlforma.dto.CategoryDTO;
import mmi.osaas.txlforma.model.Category;
import mmi.osaas.txlforma.model.Formation;
import mmi.osaas.txlforma.repository.CategoryRepository;
import mmi.osaas.txlforma.repository.FormationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository categoryRepository;
    private final FormationRepository formationRepository;

    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    public Category getCategoryById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Catégorie introuvable"));
    }

    @Transactional
    public Category createCategory(CategoryDTO dto) {
        if (categoryRepository.existsByName(dto.getName())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Une catégorie avec ce nom existe déjà");
        }

        return categoryRepository.save(Category.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .imageUrl(dto.getImageUrl())
                .createdAt(LocalDateTime.now())
                .build());
    }

    @Transactional
    public Category updateCategory(Long id, CategoryDTO dto) {
        Category existing = getCategoryById(id);

        existing.setName(dto.getName());
        existing.setDescription(dto.getDescription());
        existing.setImageUrl(dto.getImageUrl());

        return categoryRepository.save(existing);
    }

    @Transactional
    public void deleteCategory(Long id) {
        getCategoryById(id);
        
        List<Formation> formations = formationRepository.findByCategoryId(id);
        if (!formations.isEmpty()) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT, 
                "Impossible de supprimer cette catégorie car elle contient " + formations.size() + " formation(s). Veuillez d'abord supprimer ou déplacer les formations associées."
            );
        }
        
        categoryRepository.deleteById(id);
    }
}