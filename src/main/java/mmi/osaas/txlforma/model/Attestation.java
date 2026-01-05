package mmi.osaas.txlforma.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import mmi.osaas.txlforma.enums.AttestationType;

import java.time.LocalDateTime;

@Entity
@Table(name = "attestations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Attestation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "participation_id", nullable = false)
    @JsonIgnoreProperties({"paiement", "session"})
    private Participation participation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AttestationType type;

    @Column(name = "file_path", nullable = false)
    private String filePath;

    @Column(name = "generated_at", nullable = false, updatable = false)
    private LocalDateTime generatedAt;
}

