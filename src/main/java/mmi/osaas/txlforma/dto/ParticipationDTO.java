package mmi.osaas.txlforma.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import mmi.osaas.txlforma.enums.PaiementStatus;
import mmi.osaas.txlforma.enums.ParticipationStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParticipationDTO {
    private Long id;
    private ParticipationStatus status;
    private LocalDateTime participationAt;
    private Long sessionId;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String location;
    private Double price;
    private Long formationId;
    private String formationTitle;
    private String formationImageUrl;
    private Long formateurId;
    private String formateurName;
    private PaiementStatus paiementStatus;
    private LocalDateTime createdAt;
}

