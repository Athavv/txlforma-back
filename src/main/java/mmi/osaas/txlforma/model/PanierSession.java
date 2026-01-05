package mmi.osaas.txlforma.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "panier_sessions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PanierSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "panier_id", nullable = false)
    @JsonIgnoreProperties({"panierSessions"})
    private Panier panier;

    @ManyToOne
    @JoinColumn(name = "session_id", nullable = false)
    private Session session;

    @Column(name = "added_at", nullable = false, updatable = false)
    private LocalDateTime addedAt;
}
