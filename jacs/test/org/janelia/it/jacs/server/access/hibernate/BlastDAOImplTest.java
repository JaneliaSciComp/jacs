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

package org.janelia.it.jacs.server.access.hibernate;

import org.janelia.it.jacs.server.access.BlastDAO;
import org.janelia.it.jacs.test.JacswebTestCase;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Feb 8, 2007
 * Time: 1:44:39 PM
 *
 */
public class BlastDAOImplTest extends JacswebTestCase {

    private BlastDAO blastDAO;

    public BlastDAO getBlastDAO() {
        return blastDAO;
    }

    public void setBlastDAO(BlastDAO blastDAO) {
        this.blastDAO = blastDAO;
    }

    public BlastDAOImplTest() {
        super(BlastDAOImplTest.class.getName());
    }

    public void testBlastDAO() {
        assertNotNull(blastDAO);
    }

    public void testGetNodeIdVsSiteLocation() {
        Map nodeSiteMap=null;
        try {
            nodeSiteMap=blastDAO.getNodeIdVsSiteLocation("CAM_PROJ_GOS");
            assertNotNull(nodeSiteMap);
            Set keySet=nodeSiteMap.keySet();
            assertNotNull(keySet);
            Iterator iter=keySet.iterator();
            int nullCount=0;
            int nonNullCount=0;
            while(iter.hasNext()) {
                String nodeId=(String)iter.next();
                String siteName=(String)nodeSiteMap.get(nodeId);
                if (siteName==null)
                    nullCount++;
                if (siteName!=null)
                    nonNullCount++;
            }
            assertTrue(nonNullCount > nullCount);
        } catch (Exception ex) {
            String message="Exception: "+ex.getMessage();
            logger.warn(message);
            fail(message);
        }
    }

    public void testGetSiteLocations() {
        List siteLocationList=null;
        try {
            siteLocationList=blastDAO.getSiteLocations("CAM_PROJ_GOS");
            assertNotNull(siteLocationList);
            assertTrue(siteLocationList.size()>0);
            assertTrue(((String)siteLocationList.get(0)).trim().length()>0);
        } catch (Exception ex) {
            String message="Exception: "+ex.getMessage();
            logger.warn(message);
            fail(message);
        }
    }

}
