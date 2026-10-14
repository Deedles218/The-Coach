package lib;

/** Test-data failure raised when an existing-progress account still requires onboarding. */
public class PostAuthQuestionnaireException extends AssertionError {
    public PostAuthQuestionnaireException(String message) {
        super(message);
    }
}
