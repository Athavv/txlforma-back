package mmi.osaas.txlforma.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import mmi.osaas.txlforma.enums.PanierStatus;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PanierDTO {
    private Long id;
    private Long userId;
    private PanierStatus status;
    private List<PanierSessionDTO> sessions;
    private Double totalPrice;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
