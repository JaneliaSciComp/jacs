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

package org.janelia.it.jacs.compute.service.blast.persist.query;

import org.janelia.it.jacs.compute.engine.service.ServiceException;

/**
 * @author Tareq Nabeel
 */
public class PersistQNodeException extends ServiceException {

    public static final int USER_DOES_NOT_EXIST = 1;

    /**
     * Construct a PersistQNodeException with a descriptive String
     *
     * @param msg The string that describes the error
     */
    public PersistQNodeException(String msg) {
        super(msg);
    }

    /**
     * Construct a PersistQNodeException to wrap another exception.
     *
     * @param e The exception to be wrapped.
     */
    public PersistQNodeException(Throwable e) {
        super(e);
    }

    /**
     * Construct a PersistQNodeException to wrap another exception.
     *
     * @param e The exception to be wrapped.
     */
    public PersistQNodeException(String msg, Throwable e) {
        super(msg, e);
    }


    /**
     * Construct a PersistQNodeException with a descriptive String
     *
     * @param msg       The string that describes the error
     * @param errorCode more description on the error for possible special handling
     */
    public PersistQNodeException(String msg, int errorCode) {
        super(msg);
        setErrorCode(errorCode);
    }

    /**
     * Construct a PersistQNodeException to wrap another exception.
     *
     * @param e         The exception to be wrapped.
     * @param errorCode more description on the error for possible special handling
     */
    public PersistQNodeException(Throwable e, int errorCode) {
        super(e);
        setErrorCode(errorCode);
    }

    /**
     * Construct a PersistQNodeException to wrap another exception.
     *
     * @param e         The exception to be wrapped.
     * @param errorCode more description on the error for possible special handling
     */
    public PersistQNodeException(String msg, Throwable e, int errorCode) {
        super(msg, e);
        setErrorCode(errorCode);
    }
}
