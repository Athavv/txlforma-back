package mmi.osaas.txlforma.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import mmi.osaas.txlforma.enums.ParticipationStatus;

import java.time.LocalDateTime;

@Entity
@Table(name = "participations", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "session_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Participation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({"password", "createdAt"})
    private User user;

    @ManyToOne
    @JoinColumn(name = "session_id", nullable = false)
    private Session session;

    @ManyToOne
    @JoinColumn(name = "paiement_id", nullable = false)
    @JsonIgnoreProperties({"panier"})
    private Paiement paiement;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ParticipationStatus status = ParticipationStatus.INSCRIT;

    @Column(name = "participation_at")
    private LocalDateTime participationAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}

