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

package org.janelia.it.jacs.web.gwt.common.client.panel;

import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.HelpActionLink;
import org.janelia.it.jacs.web.gwt.common.client.util.SystemProps;

/**
 * @author Michael Press
 */
public class TitledBoxActionLinkUtils {
    private static Logger _logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.common.client.panel.TitledBoxActionLinkUtils");

    /**
     * Adds a help link to the TitledBox if and only if the property named urlPropertyName can be retrieved
     * asynchronously and set as the URL on the helpActionLink.
     *
     * @param titledBox       - the TitledBox to add the HelpActionLink to
     * @param helpActionLink  - the HelpActionLink to add (URL not yet set)
     * @param urlPropertyName - the property containing the destionation URL to retrieve
     */
    public static void addHelpActionLink(final TitledBox titledBox, final HelpActionLink helpActionLink,
                                         final String urlPropertyName) {
        String url = SystemProps.getString(urlPropertyName, null);
        if (url != null) {
            helpActionLink.setUrl(url);
            titledBox.addActionLink(helpActionLink);
        }
        else {
            _logger.error("Failed to retrieve " + urlPropertyName + " property; can't provide action link");
        }

//        _propertyService.getProperty(urlPropertyName, new AsyncCallback() {
//            public void onFailure(Throwable throwable) {
//                _logger.error("Failed to retrieve " + urlPropertyName + " property; can't provide action link");
//            }
//
//            public void onSuccess(final Object url) {
//                if (url != null) {
//                    _logger.info(" retrieved property action link url =" + url);
//                    helpActionLink.setUrl((String) url);
//                    titledBox.addActionLink(helpActionLink);
//                }
//            }
//        });
    }
}
