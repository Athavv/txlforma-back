package mmi.osaas.txlforma.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import mmi.osaas.txlforma.enums.PaiementStatus;
import mmi.osaas.txlforma.enums.ParticipationStatus;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionParticipantDTO {
    private Long id;
    private ParticipationStatus status;
    private LocalDateTime participationAt;
    
    private Long userId;
    private String userFirstname;
    private String userLastname;
    private String userEmail;
    private String userImageUrl;
    
    private PaiementStatus paiementStatus;
    private Double paiementAmount;
    private String paiementCurrency;
    
    private LocalDateTime createdAt;
}
