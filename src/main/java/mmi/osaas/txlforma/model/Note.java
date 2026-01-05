package mmi.osaas.txlforma.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "notes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Note {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "participation_id", nullable = false, unique = true)
    @JsonIgnoreProperties({"paiement", "session"})
    private Participation participation;

    @ManyToOne
    @JoinColumn(name = "given_by", nullable = false)
    @JsonIgnoreProperties({"password"})
    private User givenBy;

    @Column(nullable = false)
    private Double note;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;

    @Column(nullable = false)
    @Builder.Default
    private Boolean locked = false;
}


