package mmi.osaas.txlforma.config;

import lombok.RequiredArgsConstructor;
import mmi.osaas.txlforma.service.NoteService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ScheduledTasks {

    private final NoteService noteService;

    @Scheduled(cron = "0 0 2 * * ?")
    public void lockNotesPastDeadline() {
        noteService.lockNotesPastDeadline();
    }
}


