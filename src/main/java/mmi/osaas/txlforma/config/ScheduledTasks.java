package mmi.osaas.txlforma.config;

import lombok.RequiredArgsConstructor;
import mmi.osaas.txlforma.service.AttestationService;
import mmi.osaas.txlforma.service.NoteService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ScheduledTasks {

    private final NoteService noteService;
    private final AttestationService attestationService;

    @Scheduled(cron = "0 0 2 * * ?")
    public void lockNotesPastDeadline() {
        noteService.lockNotesPastDeadline();
    }

    @Scheduled(cron = "0 0 3 * * ?")
    public void generateSuccessAttestations() {
        attestationService.generateSuccessAttestations();
    }
}


