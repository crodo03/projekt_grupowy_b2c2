package exceptions;

public class NodeConfigException extends RuntimeException {
    public NodeConfigException(String message) {
        super(message);
    }

    public NodeConfigException(String message, Throwable cause) {
        super(message, cause);
    }
}
