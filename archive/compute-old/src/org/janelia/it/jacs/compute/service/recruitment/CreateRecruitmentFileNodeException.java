
package org.janelia.it.jacs.compute.service.recruitment;

import org.janelia.it.jacs.compute.engine.service.ServiceException;

/**
 * @author Tareq Nabeel
 */
public class CreateRecruitmentFileNodeException extends ServiceException {
    /**
     * Construct a CreateRecruitmentFileNodeException with a descriptive String
     *
     * @param msg The string that describes the error
     */
    public CreateRecruitmentFileNodeException(String msg) {
        super(msg);
    }

    /**
     * Construct a CreateRecruitmentFileNodeException to wrap another exception.
     *
     * @param e The exception to be wrapped.
     */
    public CreateRecruitmentFileNodeException(Throwable e) {
        super(e);
    }

    /**
     * Construct a CreateRecruitmentFileNodeException to wrap another exception.
     *
     * @param e The exception to be wrapped.
     */
    public CreateRecruitmentFileNodeException(String msg, Throwable e) {
        super(msg, e);
    }


    /**
     * Construct a CreateRecruitmentFileNodeException with a descriptive String
     *
     * @param msg       The string that describes the error
     * @param errorCode more description on the error for possible special handling
     */
    public CreateRecruitmentFileNodeException(String msg, int errorCode) {
        super(msg);
        setErrorCode(errorCode);
    }

    /**
     * Construct a CreateRecruitmentFileNodeException to wrap another exception.
     *
     * @param e         The exception to be wrapped.
     * @param errorCode more description on the error for possible special handling
     */
    public CreateRecruitmentFileNodeException(Throwable e, int errorCode) {
        super(e);
        setErrorCode(errorCode);
    }

    /**
     * Construct a CreateRecruitmentFileNodeException to wrap another exception.
     *
     * @param e         The exception to be wrapped.
     * @param errorCode more description on the error for possible special handling
     */
    public CreateRecruitmentFileNodeException(String msg, Throwable e, int errorCode) {
        super(msg, e);
        setErrorCode(errorCode);
    }
}
