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

import org.janelia.it.jacs.model.download.Project;
import org.janelia.it.jacs.model.metadata.Sample;
import org.janelia.it.jacs.server.access.DownloadDAO;
import org.janelia.it.jacs.test.JacswebTestCase;

import java.util.Iterator;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Feb 7, 2007
 * Time: 10:48:04 AM
 *
 */
public class DownloadDAOImplTest extends JacswebTestCase {

    private DownloadDAO downloadDAO;

    public DownloadDAO getDownloadDAO() {
        return downloadDAO;
    }

    public void setDownloadDAO(DownloadDAO downloadDAO) {
        this.downloadDAO = downloadDAO;
    }

    public DownloadDAOImplTest() {
        super(DownloadDAOImplTest.class.getName());
    }

    public void testFindAllProjects() {
        List projectList=null;
        try {
            projectList=downloadDAO.findAllProjects();
            assertTrue(projectList.size()>0);
            Iterator iter=projectList.iterator();
            while(iter.hasNext()) {
                Project project=(Project)iter.next();
                assertNotNull(project);
            }
        } catch (Exception ex) {
            String message="Exception: "+ex.getMessage();
            logger.warn(message);
            fail(message);
        }
    }

    public void testFindProjectSamples() {
         List sampleList=null;
        try {
            sampleList=downloadDAO.findProjectSamples("CAM_PROJ_GOS");
            assertTrue(sampleList.size()>0);
            Iterator iter=sampleList.iterator();
            while(iter.hasNext()) {
                Sample sample=(Sample)iter.next();
                assertNotNull(sample);
                assertNotNull(sample.getSampleName());
            }
        } catch (Exception ex) {
            String message="Exception: "+ex.getMessage();
            logger.warn(message);
            fail(message);
        }
    }

}
