package mmi.osaas.txlforma.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class FormationDTO {
    
    @NotBlank(message = "Le titre est obligatoire")
    @Size(max = 200, message = "Le titre ne doit pas dépasser 200 caractères")
    private String title;
    
    @NotBlank(message = "La description est obligatoire")
    @Size(max = 1000, message = "La description ne doit pas dépasser 1000 caractères")
    private String description;
    
    @NotNull(message = "L'ID de la catégorie est obligatoire")
    private Long categoryId;
    
    private String imageUrl;
}
