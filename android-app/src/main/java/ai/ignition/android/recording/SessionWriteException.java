package ai.ignition.android.recording;

import java.io.IOException;

/**
 * Exception thrown when writing or flushing a session event to storage fails.
 */
public class SessionWriteException extends RuntimeException {

    public SessionWriteException(String message, Throwable cause) {
        super(message, cause);
    }

    public SessionWriteException(String message) {
        super(message);
    }
}
