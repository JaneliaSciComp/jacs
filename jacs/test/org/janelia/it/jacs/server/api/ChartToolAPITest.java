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

package org.janelia.it.jacs.server.api;

import org.janelia.it.jacs.test.JacswebTestCase;
import org.janelia.it.jacs.web.gwt.common.shared.data.ChartData;
import org.janelia.it.jacs.web.gwt.common.shared.data.ChartDataEntry;
import org.janelia.it.jacs.web.gwt.common.shared.data.ImageModel;

/**
 * Created by IntelliJ IDEA.
 * User: tdolafi
 * Date: Aug 4, 2006
 * Time: 11:25:09 AM
 *
 */
public class ChartToolAPITest extends JacswebTestCase {

    private JFreeChartTool jFreeChartTool;

    public ChartToolAPITest() {
        super(ChartToolAPITest.class.getName());
    }

    public JFreeChartTool getJFreeChartTool() {
        return jFreeChartTool;
    }

    public void setJFreeChartTool(JFreeChartTool jFreeChartTool) {
        this.jFreeChartTool = jFreeChartTool;
    }

    public void testCreateBarChart() {
        try {
            String baseTestDirectory = "jacs/test";
            ChartData chartValues = new ChartData();
            chartValues.addChartDataEntry(new ChartDataEntry("Cluster","CAM_CRCL_14436204",2));
            chartValues.addChartDataEntry(new ChartDataEntry("Cluster","CAM_CRCL_14436223",2));
            chartValues.addChartDataEntry(new ChartDataEntry("Cluster","CAM_CRCL_45436223",2));
            chartValues.addChartDataEntry(new ChartDataEntry("Cluster","CAM_CRCL_454363709",1));
            chartValues.addChartDataEntry(new ChartDataEntry("Cluster","CAM_CRCL_9874543623",1));
            chartValues.setTotal(100);
            ImageModel testClusterChart  =
                    jFreeChartTool.createBarChart("Top protein clusters",
                            "Protein Cluster",
                            "Percentage",
                            chartValues,
                            0, 0, baseTestDirectory,
                            "testbarchart");
            assertTrue(testClusterChart != null);
        } catch (Exception ex) {
            failFromException(ex);
        }
    }

}
