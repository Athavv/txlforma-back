package mmi.osaas.txlforma.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import mmi.osaas.txlforma.enums.AttestationType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttestationDTO {
    private Long id;
    private AttestationType type;
    private LocalDateTime generatedAt;
    private Long participationId;
    private String userFirstname;
    private String userLastname;
    private String formationTitle;
    private LocalDate startDate;
    private LocalTime startTime;
    private Double note;
}

