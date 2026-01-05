package mmi.osaas.txlforma.service;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.TextAlignment;
import lombok.RequiredArgsConstructor;
import mmi.osaas.txlforma.enums.AttestationType;
import mmi.osaas.txlforma.enums.ParticipationStatus;
import mmi.osaas.txlforma.model.Attestation;
import mmi.osaas.txlforma.model.Note;
import mmi.osaas.txlforma.model.Participation;
import mmi.osaas.txlforma.repository.AttestationRepository;
import mmi.osaas.txlforma.repository.EmargementRepository;
import mmi.osaas.txlforma.repository.NoteRepository;
import mmi.osaas.txlforma.repository.ParticipationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AttestationService {

    private final AttestationRepository attestationRepository;
    private final ParticipationRepository participationRepository;
    private final EmargementRepository emargementRepository;
    private final NoteRepository noteRepository;

    @Value("${app.attestations.directory:attestations}")
    private String attestationsDirectory;

    @Transactional
    public Attestation generateAttestation(Long participationId) {
        Participation participation = participationRepository.findById(participationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Participation introuvable"));

        AttestationType type = determineAttestationType(participation);
        
        if (type == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                    "Impossible de générer l'attestation : conditions non remplies");
        }

        if (attestationRepository.findByParticipationIdAndType(participationId, type).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                    "Une attestation de type " + type + " existe déjà pour cette participation");
        }

        try {
            return createAttestation(participation, type);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur lors de la génération du PDF");
        }
    }

    public List<Attestation> getMyAttestations(Long userId) {
        return attestationRepository.findByParticipationUserId(userId);
    }

    public byte[] downloadAttestation(Long attestationId) {
        Attestation attestation = attestationRepository.findById(attestationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Attestation introuvable"));

        try {
            Path filePath = Paths.get(attestation.getFilePath());
            if (!Files.exists(filePath)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Fichier PDF introuvable");
            }
            return Files.readAllBytes(filePath);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur lors de la lecture du fichier");
        }
    }

    @Transactional
    public void generateSuccessAttestations() {
        LocalDate today = LocalDate.now();
        List<Participation> validParticipations = participationRepository.findAll().stream()
                .filter(participation -> participation.getStatus() == ParticipationStatus.VALIDE)
                .filter(participation -> isDeadlinePassed(participation.getSession().getEndDate(), today))
                .filter(participation -> hasValidNote(participation.getId()))
                .filter(participation -> !hasSuccessAttestation(participation.getId()))
                .toList();
        
        for (Participation participation : validParticipations) {
            try {
                createAttestation(participation, AttestationType.SUCCES);
            } catch (IOException ignored) {
                // Ignorer les erreurs de génération pour continuer avec les autres
            }
        }
    }

    private boolean isDeadlinePassed(LocalDate endDate, LocalDate today) {
        LocalDate deadline = endDate.plusDays(14);
        return !today.isBefore(deadline);
    }

    private boolean hasValidNote(Long participationId) {
        return noteRepository.findByParticipationId(participationId)
                .map(note -> note.getNote() >= 10)
                .orElse(false);
    }

    private boolean hasSuccessAttestation(Long participationId) {
        return attestationRepository.findByParticipationIdAndType(participationId, AttestationType.SUCCES)
                .isPresent();
    }

    private Attestation createAttestation(Participation participation, AttestationType type) throws IOException {
        String filePath = generatePdf(participation, type);
        Attestation attestation = Attestation.builder()
                .participation(participation)
                .type(type)
                .filePath(filePath)
                .generatedAt(LocalDateTime.now())
                .build();
        return attestationRepository.save(attestation);
    }

    private AttestationType determineAttestationType(Participation participation) {
        if (participation.getStatus() == ParticipationStatus.PRESENT 
                && emargementRepository.existsByParticipationId(participation.getId())) {
            return AttestationType.PRESENCE;
        }
        
        if (participation.getStatus() == ParticipationStatus.VALIDE) {
            LocalDate deadline = participation.getSession().getEndDate().plusDays(14);
            if (!LocalDate.now().isBefore(deadline)) {
                return noteRepository.findByParticipationId(participation.getId())
                        .filter(note -> note.getNote() >= 10)
                        .map(note -> AttestationType.SUCCES)
                        .orElse(null);
            }
        }
        
        return null;
    }

    private String generatePdf(Participation participation, AttestationType type) throws IOException {
        Path directory = Paths.get(attestationsDirectory);
        Files.createDirectories(directory);

        String fileName = String.format("attestation_%d_%s_%d.pdf", 
                participation.getId(), type, System.currentTimeMillis());
        Path filePath = directory.resolve(fileName);

        try (PdfWriter writer = new PdfWriter(filePath.toFile());
             PdfDocument pdf = new PdfDocument(writer);
             Document document = new Document(pdf)) {

            document.setMargins(50, 50, 50, 50);

            Paragraph title = new Paragraph("ATTESTATION")
                    .setFontSize(24)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(20);
            document.add(title);

            Paragraph typeParagraph = new Paragraph(type == AttestationType.PRESENCE ? 
                    "DE PRÉSENCE" : "DE SUCCÈS")
                    .setFontSize(18)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(30);
            document.add(typeParagraph);

            Paragraph text = new Paragraph();
            text.add("Je soussigné(e), représentant de TXLFORMA, certifie que :\n\n");
            text.add(new Text(participation.getUser().getFirstname() + " " + 
                             participation.getUser().getLastname())
                    .setBold());
            text.add("\n\n");
            text.add("a " + (type == AttestationType.PRESENCE ? "assisté à" : "réussi") + 
                    " la formation :\n\n");
            text.add(new Text(participation.getSession().getFormation().getTitle())
                    .setBold());
            text.add("\n\n");
            text.add("Session du " + 
                    participation.getSession().getStartDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + 
                    " au " + 
                    participation.getSession().getEndDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            text.add("\n\n");
            text.add("Lieu : " + participation.getSession().getLocation());
            
            if (type == AttestationType.SUCCES) {
                Note note = noteRepository.findByParticipationId(participation.getId()).orElse(null);
                if (note != null) {
                    text.add("\n\n");
                    text.add("Note obtenue : " + note.getNote() + "/20");
                }
            }
            
            text.add("\n\n");
            text.add("Fait à " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            text.add("\n\n");
            text.add("TXLFORMA");
            
            document.add(text);
        }

        return filePath.toString();
    }
}

