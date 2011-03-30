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

package org.janelia.it.jacs.compute.jtc;

public class SystemErrorException extends Exception {

    Error[] errors;
    private static final String NEWLINE = System.getProperty("line.separator");

    public SystemErrorException(Throwable cause) {
        super(cause);
        if (SystemErrorException.class.isInstance(cause)) {
            SystemErrorException e = (SystemErrorException) cause;
            if (e.getErrors() != null) {
                int length = e.getErrors().length;
                errors = new Error[e.getErrors().length];
                System.arraycopy(e.getErrors(), 0, errors, 0, length);
            }
        }
    }

    public SystemErrorException(String msg) {
        super(msg);
    }

    public SystemErrorException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public SystemErrorException(Error[] errors) {
        this.errors = errors;
    }

    public SystemErrorException(String msg, Error[] errors) {
        super(msg);
        this.errors = errors;
    }

    public SystemErrorException(String msg, Throwable cause, Error[] errors) {
        super(msg, cause);
        this.errors = errors;
    }

    public SystemErrorException(Throwable cause, Error[] errors) {
        super(cause);
        this.errors = errors;
    }

    public Error[] getErrors() {
        return errors;
    }
//    /**
//     * Returns a error String in SystemErrorException
//     * the result is the concatenation of three strings:
//     * <ul>
//     * <li>The name of the actual class of this object
//     * <li>": " (a colon and a space)
//     * <li>The result of the {@link #getMessage} method for this object
//     * <li>": "(a colon and a space)
//     * <li>All the error messages in errors object delimited by ": "
//     * </ul>
//     *
//     * @return a string representation of this SystemErrorException.

    //     */
    public String toString() {
        StringBuffer s = new StringBuffer();
        String sName = getClass().getName();
        String message = getLocalizedMessage();
        s.append(sName);
        if (message != null) {
            s.append(": ");
            s.append(message);
        }
        if (errors != null) {
            for (int i = 0; i < errors.length; i++) {
                s.append(i).append(": ");
                s.append(errors[i].getErrorCodeMessage());
            }
        }
        return s.toString();
    }

    public String getErrorMessage() {
        StringBuffer sb = new StringBuffer();
        sb.append(this.getMessage());
        if (errors != null) {
            for (int i = 0; i < errors.length; i++) {
                sb.append(i);
                sb.append(":");
                sb.append(errors[i].getErrorCodeMessage());
                sb.append(":");
                sb.append(errors[i].getErrorGroupId());
                sb.append(NEWLINE);
            }
        }
        return sb.toString();
    }
}
