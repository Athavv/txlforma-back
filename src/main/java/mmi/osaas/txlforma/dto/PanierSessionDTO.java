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
public class PanierSessionDTO {
    private Long id;
    private Long sessionId;
    private SessionDTO session;
    private LocalDateTime addedAt;
}

