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

import org.janelia.it.jacs.model.download.MooreOrganism;
import org.janelia.it.jacs.server.access.MooreDAO;
import org.janelia.it.jacs.test.JacswebTestCase;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Jan 29, 2008
 * Time: 3:57:19 PM
 *
 */
public class MooreDAOImplTest extends JacswebTestCase {
    private MooreDAO mooreDAO;

    public MooreDAOImplTest() {
        super(MooreDAOImplTest.class.getName());
    }

    public void setMooreDAO(MooreDAO mooreDAO) {
        this.mooreDAO=mooreDAO;
    }

    public MooreDAO getMooreDAO() {
        return mooreDAO;
    }

    public void testFindAllOrganisms() {
        List<MooreOrganism> organismList;
        try {
            organismList= mooreDAO.findAllOrganisms();
            assertNotNull(organismList);
            assertTrue("organismList size must be at least 168",organismList.size()>160);
        } catch (Exception ex) {
            failFromException(ex);
        }
    }

}
