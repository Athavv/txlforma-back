package mmi.osaas.txlforma.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmargementDTO {
    private Long id;
    private String signatureData;
    private LocalDateTime signedAt;
    private Boolean present;
    private Long userId;
    private String userFirstname;
    private String userLastname;
    private String userEmail;
    private String userImageUrl;
    private Long participationId;
    private LocalDateTime participationAt;
}