
package com.creanga.jsch;

public class SshClientException extends RuntimeException {

    private static final long serialVersionUID = 535647747623832015L;

    public SshClientException() {
    }

    public SshClientException(final String message) {
        super(message);
    }

    public SshClientException(final Throwable cause) {
        super(cause);
    }

    public SshClientException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
