package mmi.osaas.txlforma.service;

import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Div;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import lombok.RequiredArgsConstructor;
import mmi.osaas.txlforma.dto.AttestationDTO;
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
import java.time.ZoneId;
import java.time.ZonedDateTime;
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
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Impossible de générer l'attestation : conditions non remplies");
        }

        if (attestationRepository.findByParticipationIdAndType(participationId, type).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Une attestation de type " + type + " existe déjà pour cette participation");
        }

        try {
            return createAttestation(participation, type);
        } catch (IOException ioException) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur lors de la génération du PDF");
        }
    }

    public List<AttestationDTO> getMyAttestations(Long userId) {
        return attestationRepository.findByParticipationUserId(userId).stream()
                .map(this::toAttestationDTO)
                .toList();
    }

    private AttestationDTO toAttestationDTO(Attestation attestation) {
        Participation participation = attestation.getParticipation();
        AttestationDTO.AttestationDTOBuilder builder = AttestationDTO.builder()
                .id(attestation.getId())
                .type(attestation.getType())
                .generatedAt(attestation.getGeneratedAt())
                .participationId(participation.getId())
                .userFirstname(participation.getUser().getFirstname())
                .userLastname(participation.getUser().getLastname())
                .formationTitle(participation.getSession().getFormation().getTitle())
                .startDate(participation.getSession().getStartDate())
                .startTime(participation.getSession().getStartTime());

        if (attestation.getType() == AttestationType.SUCCES) {
            noteRepository.findByParticipationId(participation.getId())
                    .ifPresent(note -> builder.note(note.getNote()));
        }

        return builder.build();
    }

    @Transactional
    public byte[] downloadAttestation(Long attestationId) {
        Attestation attestation = attestationRepository.findById(attestationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Attestation introuvable"));

        try {
            Path filePath = Paths.get(attestation.getFilePath());
            if (!Files.exists(filePath)) {
                try {
                    regeneratePdfIfMissing(attestation);
                    attestation = attestationRepository.findById(attestationId)
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Attestation introuvable"));
                    filePath = Paths.get(attestation.getFilePath());
                    if (!Files.exists(filePath)) {
                        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Fichier PDF introuvable et impossible à régénérer");
                    }
                } catch (IOException ioException) {
                    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur lors de la régénération du PDF");
                }
            }
            return Files.readAllBytes(filePath);
        } catch (IOException ioException) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur lors de la lecture du fichier");
        }
    }

    private void regeneratePdfIfMissing(Attestation attestation) throws IOException {
        Path directory = Paths.get(attestationsDirectory);
        Files.createDirectories(directory);
        String fileName = String.format("attestation_%d_%s_%d.pdf", attestation.getParticipation().getId(), attestation.getType(), System.currentTimeMillis());
        Path filePath = directory.resolve(fileName);
        generatePdfContent(attestation.getParticipation(), attestation.getType(), filePath);
        attestation.setFilePath(filePath.toString());
        attestationRepository.save(attestation);
    }

    private void generatePdfContent(Participation participation, AttestationType type, Path filePath) throws IOException {
        try (PdfWriter writer = new PdfWriter(filePath.toFile());
             PdfDocument pdf = new PdfDocument(writer);
             Document document = new Document(pdf)) {

            document.setMargins(50, 50, 50, 50);

            Div borderDiv = new Div()
                    .setBorder(new SolidBorder(new DeviceRgb(0, 0, 0), 2))
                    .setPadding(50)
                    .setMarginBottom(20);

            DeviceRgb darkGreen = new DeviceRgb(83, 242, 106);
            try {
                Path logoPath = Paths.get("front/public/logo.png");
                if (Files.exists(logoPath)) {
                    Image logo = new Image(ImageDataFactory.create(logoPath.toAbsolutePath().toString()));
                    logo.setWidth(120);
                    logo.setHorizontalAlignment(HorizontalAlignment.CENTER);
                    borderDiv.add(logo);
                } else {
                    Paragraph logoTitle = new Paragraph("TXLFORMA")
                            .setFontSize(36)
                            .setBold()
                            .setTextAlignment(TextAlignment.CENTER)
                            .setMarginBottom(15)
                            .setFontColor(darkGreen);
                    borderDiv.add(logoTitle);
                }
            } catch (Exception e) {
                Paragraph logoTitle = new Paragraph("TXLFORMA")
                        .setFontSize(36)
                        .setBold()
                        .setTextAlignment(TextAlignment.CENTER)
                        .setMarginBottom(15)
                        .setFontColor(darkGreen);
                borderDiv.add(logoTitle);
            }

            Paragraph title = new Paragraph("ATTESTATION")
                    .setFontSize(30)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(10);
            borderDiv.add(title);

            Paragraph typeParagraph = new Paragraph(type == AttestationType.PRESENCE ? "DE PRÉSENCE" : "DE SUCCÈS")
                    .setFontSize(22)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(40)
                    .setFontColor(darkGreen);
            borderDiv.add(typeParagraph);

            Paragraph text = new Paragraph();
            text.add(new Text("Je soussigné(e), représentant de TXLFORMA, certifie que :\n\n").setFontSize(13));
            text.add(new Text(participation.getUser().getFirstname() + " " + participation.getUser().getLastname())
                    .setBold()
                    .setFontSize(15));
            text.add(new Text("\n\n").setFontSize(13));
            text.add(new Text("a " + (type == AttestationType.PRESENCE ? "assisté à" : "réussi") + " la formation :\n\n").setFontSize(13));
            text.add(new Text(participation.getSession().getFormation().getTitle())
                    .setBold()
                    .setFontSize(15));
            text.add(new Text("\n\n").setFontSize(13));
            text.add(new Text("Session du " + participation.getSession().getStartDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) +
                    " au " + participation.getSession().getEndDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))).setFontSize(13));
            text.add(new Text("\n\n").setFontSize(13));
            text.add(new Text("Lieu : " + participation.getSession().getLocation()).setFontSize(13));

            if (type == AttestationType.SUCCES) {
                Note note = noteRepository.findByParticipationId(participation.getId()).orElse(null);
                if (note != null) {
                    text.add(new Text("\n\n").setFontSize(13));
                    text.add(new Text("Note obtenue : " + note.getNote() + "/20").setBold().setFontSize(14));
                }
            }

            text.add(new Text("\n\n\n").setFontSize(13));
            text.add(new Text("Fait à " + ZonedDateTime.now(ZoneId.of("Europe/Paris")).toLocalDateTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))).setFontSize(13));
            text.add(new Text("\n\n").setFontSize(13));
            text.add(new Text("TXLFORMA").setBold().setFontSize(16).setFontColor(darkGreen));

            borderDiv.add(text);
            document.add(borderDiv);
        }
    }

    @Transactional
    public void generateSuccessAttestations() {
        LocalDate today = ZonedDateTime.now(ZoneId.of("Europe/Paris")).toLocalDate();
        List<Participation> validParticipations = participationRepository.findAll().stream()
                .filter(participation -> isDeadlinePassed(participation.getSession().getEndDate(), today))
                .filter(participation -> hasValidNote(participation.getId()))
                .filter(participation -> !hasSuccessAttestation(participation.getId()))
                .toList();

        for (Participation participation : validParticipations) {
            try {
                if (participation.getStatus() != ParticipationStatus.VALIDE) {
                    participation.setStatus(ParticipationStatus.VALIDE);
                    participationRepository.save(participation);
                }
                createAttestation(participation, AttestationType.SUCCES);
            } catch (IOException ignored) {
            }
        }
    }

    private boolean isDeadlinePassed(LocalDate endDate, LocalDate today) {
        if (endDate == null) return false;
        LocalDate deadline = endDate.plusDays(14);
        return !today.isBefore(deadline);
    }

    private boolean hasValidNote(Long participationId) {
        return noteRepository.findByParticipationId(participationId)
                .map(note -> note.getNote() >= 10)
                .orElse(false);
    }

    private boolean hasSuccessAttestation(Long participationId) {
        return attestationRepository.findByParticipationIdAndType(participationId, AttestationType.SUCCES).isPresent();
    }

    private Attestation createAttestation(Participation participation, AttestationType type) throws IOException {
        String filePath = generatePdf(participation, type);
        Attestation attestation = Attestation.builder()
                .participation(participation)
                .type(type)
                .filePath(filePath)
                .generatedAt(ZonedDateTime.now(ZoneId.of("Europe/Paris")).toLocalDateTime())
                .build();
        return attestationRepository.save(attestation);
    }

    private AttestationType determineAttestationType(Participation participation) {
        if (participation.getStatus() == ParticipationStatus.PRESENT && emargementRepository.existsByParticipationId(participation.getId())) {
            return AttestationType.PRESENCE;
        }

        if (participation.getStatus() == ParticipationStatus.VALIDE) {
            LocalDate deadline = participation.getSession().getEndDate().plusDays(14);
            LocalDate today = ZonedDateTime.now(ZoneId.of("Europe/Paris")).toLocalDate();
            if (today.isAfter(deadline) || today.isEqual(deadline)) {
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
        String fileName = String.format("attestation_%d_%s_%d.pdf", participation.getId(), type, System.currentTimeMillis());
        Path filePath = directory.resolve(fileName);
        generatePdfContent(participation, type, filePath);
        return filePath.toString();
    }
}
