package lib;

/** Marks a verified upstream dependency response; it is not an application assertion. */
public class ExternalServiceException extends RuntimeException {
    public ExternalServiceException(String message) {
        super(message);
    }
}
