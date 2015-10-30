package org.janelia.it.jacs.compute.wsrest.computeresources;

/**
 * Created by goinac on 9/3/15.
 */
public class ProcessingException extends Exception {
    private int httpStatus;

    public ProcessingException(int httpStatus) {
        super();
        this.httpStatus = httpStatus;
    }

    public ProcessingException(int httpStatus, String message) {
        super(message);
        this.httpStatus = httpStatus;
    }

    public ProcessingException(int httpStatus, Throwable cause) {
        super(cause);
        this.httpStatus = httpStatus;
    }

    public int getHttpStatus() {
        return httpStatus;
    }
}
