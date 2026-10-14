package lib;

/** Test-data failure raised when an account is routed to another application variant. */
public class AccountVariantMismatchException extends AssertionError {
    public AccountVariantMismatchException(String message) {
        super(message);
    }
}
