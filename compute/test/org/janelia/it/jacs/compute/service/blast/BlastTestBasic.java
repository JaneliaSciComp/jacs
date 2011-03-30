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

package org.janelia.it.jacs.compute.service.blast;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Created by IntelliJ IDEA.
 * User: tnabeel
 * Date: Apr 19, 2007
 * Time: 2:58:05 PM
 *
 */
public class BlastTestBasic extends BlastTestBase {

    private static final Long EXPECTED_HITS = 18L;

    public BlastTestBasic() {
        super();
    }

    public BlastTestBasic(String name) {
        super(name);
    }

    protected void setUp() throws Exception {
        super.setUp();
        setExpectedHits(EXPECTED_HITS);
        setBlastInputFileName("test.fasta");
        setBlastDatasetName(GOS_CHESAPEAKE_BAY);
    }


    public void testBlast() throws Exception {
        submitJobAndWaitForCompletion("TestBlast");
        validateHits();
    }

    public void testAllPojoBlast() throws Exception {
        submitJobAndWaitForCompletion("TestAllPojoBlast");
        validateHits();
    }

    public void testBaseServiceMDB() throws Exception {
        submitJobAndWaitForCompletion("TestBaseServiceMDB");
        validateHits();
    }

//    public void testSelfContainedBlast() throws Exception {
//        submitSelfContainedBlastAndWaitForCompletion("TestSelfContainedBlast");
//        validateHits();
//    }

    public void testAllSlsbBlast() throws Exception {
        submitJobAndWaitForCompletion("TestAllSlsbBlast");
        validateHits();
    }

    public void testAllSlbBlastLaunchedByBlastLauncherMdb() throws Exception {
        submitJobAndWaitForCompletion("TestAllSlbBlastLaunchedByBlastLauncherMdb");
        validateHits();
    }

    public void testAllPojoBlastLaunchedByBlastLauncherMdb() throws Exception {
        submitJobAndWaitForCompletion("TestAllPojoBlastLaunchedByBlastLauncherMdb");
        validateHits();
    }

    public void testAllPojoBlastLaunchedByProcessLauncherMdb() throws Exception {
        submitJobAndWaitForCompletion("TestAllPojoBlastLaunchedByProcessLauncherMdb");
        validateHits();
    }

    public void testAllPojoBlastLaunchedBySequenceLauncherMdb() throws Exception {
        submitJobAndWaitForCompletion("TestAllPojoBlastLaunchedBySequenceLauncherMdb");
        validateHits();
    }

    public void testAllMdbBlastControlledByProcessLauncherSlsb() throws Exception {
        submitJobAndWaitForCompletion("TestAllMdbBlastControlledByProcessLauncherSlsb");
        validateHits();
    }

    public void testAllMdbBlastControlledByProcessLauncherPojo() throws Exception {
        submitJobAndWaitForCompletion("TestAllMdbBlastControlledByProcessLauncherPojo");
        validateHits();
    }

    public void testAllMdbBlastControlledByProcessLauncherMdb() throws Exception {
        submitJobAndWaitForCompletion("TestAllMdbBlastControlledByProcessLauncherMdb");
        validateHits();
    }

    public void testMdbControlledBlast() throws Exception {
        submitJobAndWaitForCompletion("TestMdbControlledBlast");
        validateHits();
    }

    public void testSequenceMessageLinking() throws Exception {
        submitJobAndWaitForCompletion("TestSequenceMessageLinking");
        validateHits();
    }

    public void testMixSlsbAndMdbBlast() throws Exception {
        submitJobAndWaitForCompletion("TestMixSlsbAndMdbBlast");
        validateHits();
    }

    public void testAllSlbBlastUsingSequenceLauncherMdbNoWait() throws Exception {
        submitJobAndWaitForCompletion("TestAllSlbBlastUsingSequenceLauncherMdbNoWait");
        validateHits();
    }

    public void testPersistBlastInSeparateLauncherMDB() throws Exception {
        submitJobAndWaitForCompletion("TestPersistBlastInSeparateLauncherMDB");
        validateHits();
    }

    public static Test suite() {
        TestSuite suite = new TestSuite();
//        suite.addTest(new BlastTestBasic("testBlast"));
        suite.addTestSuite(BlastTestBasic.class);
        return suite;
    }

}
