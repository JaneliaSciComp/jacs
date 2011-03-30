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

package org.janelia.it.jacs.compute.app.ejb;

import org.janelia.it.jacs.compute.ComputeTestCase;
import org.janelia.it.jacs.compute.api.ComputeBeanRemote;
import org.janelia.it.jacs.compute.api.EJBFactory;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Feb 9, 2007
 * Time: 4:19:39 PM
 *
 */
public class ComputeBeanImplTest extends ComputeTestCase {
    ComputeBeanRemote computeBean;

    public ComputeBeanImplTest() {
        super(ComputeBeanImplTest.class.getName());
    }

    public void setUp() throws Exception {
        super.setUp();
        computeBean= EJBFactory.getRemoteComputeBean();
    }

    public void testComputeBean() {
        if (computeBean==null)
            fail("ComputeBeanImpl is null");
    }

}
