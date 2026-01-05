package mmi.osaas.txlforma.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "emargements")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Emargement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "participation_id", nullable = false, unique = true)
    @JsonIgnoreProperties({"paiement", "session"})
    private Participation participation;

    @Column(name = "signature_data", nullable = false, columnDefinition = "TEXT")
    private String signatureData; // Base64

    @Column(name = "signed_at", nullable = false, updatable = false)
    private LocalDateTime signedAt;

    @Column(nullable = false)
    @Builder.Default
    private Boolean present = true;
}

