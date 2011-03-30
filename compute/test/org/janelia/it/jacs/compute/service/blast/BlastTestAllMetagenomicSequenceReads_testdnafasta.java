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
 * Date: Apr 3, 2007
 * Time: 8:21:00 AM
 *
 */
public class BlastTestAllMetagenomicSequenceReads_testdnafasta extends BlastTestBase {

    private static final Long EXPECTED_HITS = 25L;

    public BlastTestAllMetagenomicSequenceReads_testdnafasta() {
        super();
    }

    public BlastTestAllMetagenomicSequenceReads_testdnafasta(String name) {
        super(name);
    }

    protected void setUp() throws Exception {
        super.setUp();
        setExpectedHits(EXPECTED_HITS);
        setBlastInputFileName("test.dna.fasta");
        setBlastDatasetName("All Metagenomic Sequence Reads (N)");
    }

    //good
    public void testGuiBlast() throws Exception {
        submitJobAndWaitForCompletion("BlastWithGridMerge");
        validateHits();
    }

    //good
    public void testAllPojoBlast() throws Exception {
        submitJobAndWaitForCompletion("TestAllPojoBlast");
    }

    //good
    public void testAllSlsbBlast() throws Exception {
        submitJobAndWaitForCompletion("TestAllSlsbBlast");
    }

    //good
    public void testAllPojoBlastLaunchedByBlastLauncherMdb() throws Exception {
        submitJobAndWaitForCompletion("TestAllPojoBlastLaunchedByBlastLauncherMdb");
    }

    public void testAllPojoBlastLaunchedByProcessLauncherMdb() throws Exception {
        submitJobAndWaitForCompletion("TestAllPojoBlastLaunchedByProcessLauncherMdb");
    }

    //good
    public void testAllPojoBlastLaunchedBySequenceLauncherMdb() throws Exception {
        submitJobAndWaitForCompletion("TestAllPojoBlastLaunchedBySequenceLauncherMdb");
    }

    //good
    public void testAllMdbBlastControlledByProcessLauncherSlsb() throws Exception {
        submitJobAndWaitForCompletion("TestAllMdbBlastControlledByProcessLauncherSlsb");
    }

    public void testAllMdbBlastControlledByProcessLauncherPojo() throws Exception {
        submitJobAndWaitForCompletion("TestAllMdbBlastControlledByProcessLauncherPojo");
    }

    public void testAllMdbBlastControlledByProcessLauncherMdb() throws Exception {
        submitJobAndWaitForCompletion("TestAllMdbBlastControlledByProcessLauncherMdb");
    }

    //good
    public void testMixSlsbAndMdbBlast() throws Exception {
        submitJobAndWaitForCompletion("TestMixSlsbAndMdbBlast");
    }

    //good
    public void testMixSlsbAndMdbAndMixSequenceAndOperation() throws Exception {
        submitJobAndWaitForCompletion("TestMixSlsbAndMdbAndMixSequenceAndOperation");
    }

    //good
    public void testMdbControlledBlast() throws Exception {
        submitJobAndWaitForCompletion("TestMdbControlledBlast");
    }

    public void testAllSlbBlastUsingSequenceLauncherMdbNoWait() throws Exception {
        submitJobAndWaitForCompletion("TestAllSlbBlastUsingSequenceLauncherMdbNoWait");
    }

    public static Test suite() {
        TestSuite suite = new TestSuite(BlastTestAllMetagenomicSequenceReads_testdnafasta.class);
        return suite;
    }


}

