package mmi.osaas.txlforma.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import mmi.osaas.txlforma.enums.ParticipationStatus;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NoteDTO {
    private Long id;
    private Double note;
    private Boolean locked;
    private LocalDateTime createdAt;
    private LocalDateTime modifiedAt;
    private Long participationId;
    private ParticipationStatus participationStatus;
    private Long userId;
    private String userFirstname;
    private String userLastname;
    private String userEmail;
    private Long formateurId;
    private String formateurName;
}