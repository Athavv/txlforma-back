package mmi.osaas.txlforma.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class SessionDTO {
    
    @NotNull(message = "L'ID de la formation est requis")
    private Long formationId;
    
    @NotNull(message = "L'ID du formateur est requis")
    private Long formateurId;
    
    @NotNull(message = "La date de début est requise")
    private LocalDate startDate;
    
    @NotNull(message = "La date de fin est requise")
    private LocalDate endDate;
    
    @NotNull(message = "L'heure de début est requise")
    private LocalTime startTime;
    
    @NotNull(message = "L'heure de fin est requise")
    private LocalTime endTime;
    
    @NotBlank(message = "Le lieu est requis")
    private String location;
    
    @NotNull(message = "La capacité est requise")
    @Min(value = 1, message = "La capacité doit être au moins de 1")
    @Max(value = 50, message = "La capacité ne peut pas dépasser 50")
    private Integer capacity;
    
    @NotNull(message = "Le prix est requis")
    @DecimalMin(value = "0.0", message = "Le prix doit être positif")
    private Double price;
    
    private String formateurName;
    
    private String formationImageUrl;
}
