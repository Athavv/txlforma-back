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
    private LocalDateTime createdAt;


    private Double note;

    private Long sessionId;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String location;
    private Double price;

    private Long formationId;
    private String formationTitle;
    private String formationDescription;
    private String formationImageUrl;
    private String categoryName;

    private Long formateurId;
    private String formateurFirstname;
    private String formateurLastname;
    private String formateurImageUrl;


    private PaiementStatus paiementStatus;
}
