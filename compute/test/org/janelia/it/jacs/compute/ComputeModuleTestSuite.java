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

package org.janelia.it.jacs.compute;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.janelia.it.jacs.compute.access.ComputeDAOTest;
import org.janelia.it.jacs.compute.app.ejb.ComputeBeanImplTest;
import org.janelia.it.jacs.compute.service.blast.*;
import org.janelia.it.jacs.compute.service.search.AccessionSearcherTest;
import org.janelia.it.jacs.compute.service.search.JacsAccessionSearchResultBuilderTest;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Jan 31, 2007
 * Time: 1:39:09 PM
 *
 */
public class ComputeModuleTestSuite extends TestSuite {

    public static Test suite() {
        TestSuite suite = new TestSuite();
        //  BlastTestAll is a suite, not a test. SEE MEMBERS BELOW:
        //suite.addTestSuite(BlastTestAll.class);
        // begin BlastTestAll members
        suite.addTestSuite(BlastTestErrors.class);
        suite.addTestSuite(BlastTestBasic.class);
        suite.addTestSuite(BlastTestMultiFasta.class);
        suite.addTestSuite(BlastTestMultiFastaXMLPersist.class);
        suite.addTestSuite(BlastTestMultiPartitionQDataNode.class);
        // end BlastTestAll members
        suite.addTestSuite(ComputeBeanImplTest.class);
        suite.addTestSuite(ComputeDAOTest.class);
        suite.addTestSuite(JacsAccessionSearchResultBuilderTest.class);
        suite.addTestSuite(AccessionSearcherTest.class);
        
        return suite;
    }


    public static void main(String[] args) {
        junit.textui.TestRunner.run(ComputeModuleTestSuite.suite());
    }
}
