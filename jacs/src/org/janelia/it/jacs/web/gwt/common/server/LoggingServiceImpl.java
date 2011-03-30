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

package org.janelia.it.jacs.web.gwt.common.server;

import org.janelia.it.jacs.web.gwt.common.client.service.log.LoggingService;

public class LoggingServiceImpl extends JcviGWTSpringController implements LoggingService {

    public void log(String msg) {
        log(msg, null);
    }

    public void log(Throwable throwable) {
        log(null, throwable);
    }

    public void log(String msg, Throwable throwable) {
        if (msg != null) {
            System.out.println(msg);
        }
        if (throwable != null) {
            throwable.printStackTrace(System.out);
        }
    }

    public void test(String message) {
        log(message, null);
    }

    public String stringTest(String message) {
        log(message, null);
        return message;
    }

}
