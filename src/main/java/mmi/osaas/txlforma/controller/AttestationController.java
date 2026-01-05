package mmi.osaas.txlforma.controller;

import lombok.RequiredArgsConstructor;
import mmi.osaas.txlforma.model.Attestation;
import mmi.osaas.txlforma.security.UserPrincipal;
import mmi.osaas.txlforma.service.AttestationService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/attestations")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AttestationController {

    private final AttestationService attestationService;

    @GetMapping("/generate/{participationId}")
    public ResponseEntity<Attestation> generateAttestation(
            @PathVariable Long participationId,
            @AuthenticationPrincipal UserPrincipal principal) {
        Attestation attestation = attestationService.generateAttestation(participationId);
        return ResponseEntity.ok(attestation);
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<byte[]> downloadAttestation(@PathVariable Long id) {
        byte[] pdfBytes = attestationService.downloadAttestation(id);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "attestation.pdf");
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
    }

    @GetMapping("/me")
    public ResponseEntity<List<Attestation>> getMyAttestations(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<Attestation> attestations = attestationService.getMyAttestations(principal.getId());
        return ResponseEntity.ok(attestations);
    }
}

