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

package org.janelia.it.jacs.web.gwt.detail.client.bse.read;

import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.detail.client.DetailSubPanel;
import org.janelia.it.jacs.web.gwt.detail.client.bse.BSEntityPanelBuilder;

/**
 * Responsible for controlling the sequence and timing of operations need to build a ReadPanel
 *
 * @author Tareq Nabeel
 */
public class ReadPanelBuilder extends BSEntityPanelBuilder {
    private static Logger logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.detail.client.bse.read.ReadPanelBuilder");

    /**
     * Go through DetailSubPanelBuilderFactory for getting instance
     */
    public ReadPanelBuilder(DetailSubPanel subPanel) {
        super(subPanel);
    }

    /**
     * Retrieves the panel specific data
     */
    protected void retrieveAndPopulatePanelData() {
        super.retrieveAndPopulatePanelData();
    }

}
