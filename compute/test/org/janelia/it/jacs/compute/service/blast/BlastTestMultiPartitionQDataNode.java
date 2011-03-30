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
public class BlastTestMultiPartitionQDataNode extends BlastTestBase {

    private static final Long EXPECTED_HITS = 496L;

    public BlastTestMultiPartitionQDataNode() {
        super();
    }

    public BlastTestMultiPartitionQDataNode(String name) {
        super(name);
    }

    protected void setUp() throws Exception {
        super.setUp();
        setExpectedHits(EXPECTED_HITS);
        setBlastInputFileName("clean-multi-fasta-32.fasta");
        setBlastDatasetName(MARINE_VIROMES);
    }

    public void testBlast32Seq() throws Exception {
        submitJobAndWaitForCompletion("TestBlast");
        validateHits();
    }

    public void testAllPojoBlast32Seq() throws Exception {
        submitJobAndWaitForCompletion("TestAllPojoBlast");
        validateHits();
    }

    public void testBaseServiceMDB32Seq() throws Exception {
        submitJobAndWaitForCompletion("TestBaseServiceMDB");
        validateHits();
    }

    public void testAllSlsbBlast32Seq() throws Exception {
        submitJobAndWaitForCompletion("TestAllSlsbBlast");
        validateHits();
    }

    public void testAllSlbBlastLaunchedByBlastLauncherMdb32Seq() throws Exception {
        submitJobAndWaitForCompletion("TestAllSlbBlastLaunchedByBlastLauncherMdb");
        validateHits();
    }

    public void testAllPojoBlastLaunchedByBlastLauncherMdb32Seq() throws Exception {
        submitJobAndWaitForCompletion("TestAllPojoBlastLaunchedByBlastLauncherMdb");
        validateHits();
    }

    public void testAllPojoBlastLaunchedByProcessLauncherMdb32Seq() throws Exception {
        submitJobAndWaitForCompletion("TestAllPojoBlastLaunchedByProcessLauncherMdb");
        validateHits();
    }

    public void testAllPojoBlastLaunchedBySequenceLauncherMdb32Seq() throws Exception {
        submitJobAndWaitForCompletion("TestAllPojoBlastLaunchedBySequenceLauncherMdb");
        validateHits();
    }

    public void testAllMdbBlastControlledByProcessLauncherSlsb32Seq() throws Exception {
        submitJobAndWaitForCompletion("TestAllMdbBlastControlledByProcessLauncherSlsb");
        validateHits();
    }

    public void testAllMdbBlastControlledByProcessLauncherPojo32Seq() throws Exception {
        submitJobAndWaitForCompletion("TestAllMdbBlastControlledByProcessLauncherPojo");
        validateHits();
    }

    public void testAllMdbBlastControlledByProcessLauncherMdb32Seq() throws Exception {
        submitJobAndWaitForCompletion("TestAllMdbBlastControlledByProcessLauncherMdb");
        validateHits();
    }


    public void testMdbControlledBlast32Seq() throws Exception {
        submitJobAndWaitForCompletion("TestMdbControlledBlast");
        validateHits();
    }

    public void testSequenceMessageLinking32Seq() throws Exception {
        submitJobAndWaitForCompletion("TestSequenceMessageLinking");
        validateHits();
    }

    public void testMixSlsbAndMdbBlast32Seq() throws Exception {
        submitJobAndWaitForCompletion("TestMixSlsbAndMdbBlast");
        validateHits();
    }

    public void testAllSlbBlastUsingSequenceLauncherMdbNoWait32Seq() throws Exception {
        submitJobAndWaitForCompletion("TestAllSlbBlastUsingSequenceLauncherMdbNoWait");
        validateHits();
    }

    public void testPersistBlastInSeparateLauncherMDB32Seq() throws Exception {
        submitJobAndWaitForCompletion("TestPersistBlastInSeparateLauncherMDB");
        validateHits();
    }

    public static Test suite() {
        TestSuite suite = new TestSuite();
        suite.addTestSuite(BlastTestMultiPartitionQDataNode.class);
        return suite;
    }
}
