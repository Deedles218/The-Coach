package lib;

/** Stops dependent tests after the app cannot be restored to a known entry state. */
public class SessionStateRecoveryException extends AssertionError {
    public SessionStateRecoveryException(String message, Throwable cause) {
        super(message);
        initCause(cause);
    }
}
