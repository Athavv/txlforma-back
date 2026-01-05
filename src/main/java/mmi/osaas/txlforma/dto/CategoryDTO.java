package mmi.osaas.txlforma.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CategoryDTO {

    @NotBlank(message = "Le nom est obligatoire")
    @Size(max = 100, message = "Le nom ne doit pas dépasser 100 caractères")
    private String name;

    @NotBlank(message = "La description est obligatoire")
    @Size(max = 1000, message = "La description ne doit pas dépasser 1000 caractères")
    private String description;

    private String imageUrl;
}