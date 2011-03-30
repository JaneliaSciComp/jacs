/*
 * Copyright (c) 2010-2011, J. Craig Venter Institute, Inc.
 *
 * This file is part of JCVI VICS.
 *
 * JCVI VICS is free software; you can redistribute it and/or modify it
 * under the terms and conditions of the Artistic License 2.0.  For
 * details, see the full text of the license in the file LICENSE.txt.  No
 * other rights are granted.  Any and all third party software rights to
 * remain with the original developer.
 *
 * JCVI VICS is distributed in the hope that it will be useful in
 * bioinformatics applications, but it is provided "AS IS" and WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTIES including but not limited to
 * implied warranties of merchantability or fitness for any particular
 * purpose.  For details, see the full text of the license in the file
 * LICENSE.txt.
 *
 * You should have received a copy of the Artistic License 2.0 along with
 * JCVI VICS.  If not, the license can be obtained from
 * "http://www.perlfoundation.org/artistic_license_2_0."
 */

package org.janelia.it.jacs.compute.engine.service;

import org.janelia.it.jacs.compute.api.ComputeException;


/**
 * This class represents the exception that will be thrown by an implementation of IService
 * when it fails during execution of an operation.  Note:  Service writer should wrap any
 * RuntimeExceptions with this exception, so initiator of transaction and decide what to do with
 * the exception.
 *
 * @author Tareq Nabeel
 */
public class ServiceException extends ComputeException {

    /**
     * A code that can be used to enumerate the execption being wrapped and thereby
     * provide the initiator of the transaction more information so it can decide
     * whether or not it wants to rollback the transaction
     */
    private int errorCode;

    /**
     * Construct a ServiceException with a descriptive String
     *
     * @param msg The string that describes the error
     */
    public ServiceException(String msg) {
        super(msg);
    }

    /**
     * Construct a ServiceException to wrap another exception.
     *
     * @param e The exception to be wrapped.
     */
    public ServiceException(Throwable e) {
        super(e);
    }

    /**
     * Construct a ServiceException to wrap another exception.
     *
     * @param e The exception to be wrapped.
     */
    public ServiceException(String msg, Throwable e) {
        super(msg, e);
    }


    /**
     * Construct a ServiceException with a descriptive String
     *
     * @param msg       The string that describes the error
     * @param errorCode more description on the error
     */
    public ServiceException(String msg, int errorCode) {
        super(msg);
        setErrorCode(errorCode);
    }

    /**
     * Construct a ServiceException to wrap another exception.
     *
     * @param e         The exception to be wrapped.
     * @param errorCode more description on the error
     */
    public ServiceException(Throwable e, int errorCode) {
        super(e);
        setErrorCode(errorCode);
    }

    /**
     * Construct a ServiceException to wrap another exception.
     *
     * @param e         The exception to be wrapped.
     * @param errorCode more description on the error
     */
    public ServiceException(String msg, Throwable e, int errorCode) {
        super(msg, e);
        setErrorCode(errorCode);
    }

    /**
     * Return the code that describes exactly what happened
     *
     * @return the code
     */
    public int getErrorCode() {
        return errorCode;
    }

    /**
     * Set the error code that describes exactly what went wrong
     *
     * @param errorCode the code
     */
    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }

}
