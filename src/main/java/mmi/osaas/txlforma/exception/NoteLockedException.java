package mmi.osaas.txlforma.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class NoteLockedException extends ResponseStatusException {
    public NoteLockedException(String reason) {
        super(HttpStatus.BAD_REQUEST, reason);
    }
}


