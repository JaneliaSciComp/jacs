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

import org.apache.log4j.Logger;
import org.janelia.it.jacs.web.gwt.common.client.service.DownloadEchoService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

public class DownloadEchoServiceImpl extends JcviGWTSpringController implements DownloadEchoService {

    static Logger logger = Logger.getLogger(DownloadEchoServiceImpl.class.getName());

    public static final String POSTED_DATA_ATTRIBUTE_NAME = "postedData";

    /**
     * Accepts data that can be streamed back later;  used to allow user to download data that
     * is available on the client side (in the browser).
     *
     * @return unique identifier for retrieval of the data
     * @data data to cache
     */
    public String postData(String data) {
        logger.debug("DownloadEchoService.postData()");

        // Stuff the data in the session
        HttpServletRequest request = getThreadLocalRequest();
        HttpSession session = request.getSession(false);
        session.setAttribute(POSTED_DATA_ATTRIBUTE_NAME, data);

        return "ok";// TODO: change return type to void
    }
}
