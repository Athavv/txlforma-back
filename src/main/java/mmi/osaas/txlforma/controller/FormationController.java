package mmi.osaas.txlforma.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mmi.osaas.txlforma.dto.FormationDTO;
import mmi.osaas.txlforma.model.Formation;
import mmi.osaas.txlforma.service.FormationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/formations")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class FormationController {
    private final FormationService formationService;

    @GetMapping
    public List<Formation> getFormations(@RequestParam(required = false) Long categoryId) {
        if (categoryId != null) {
            return formationService.getFormationsByCategory(categoryId);
        }
        return formationService.getAllFormations();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Formation> getFormationById(@PathVariable Long id) {
        return ResponseEntity.ok(formationService.getFormationById(id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<Formation> createFormation(@Valid @RequestBody FormationDTO dto) {
        return ResponseEntity.ok(formationService.createFormation(dto));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<Formation> updateFormation(@PathVariable Long id, @Valid @RequestBody FormationDTO dto) {
        return ResponseEntity.ok(formationService.updateFormation(id, dto));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteFormation(@PathVariable Long id) {
        formationService.deleteFormation(id);
        return ResponseEntity.ok("La formation est supprimé avec succès");
    }

}
