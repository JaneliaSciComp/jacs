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

/**
 * Created by IntelliJ IDEA.
 * User: tnabeel
 * Date: Apr 19, 2007
 * Time: 2:58:05 PM
 *
 */
public class BlastTestMultiFastaXMLPersist extends BlastTestBase {

    private static final Long EXPECTED_HITS = 580L;

    public BlastTestMultiFastaXMLPersist() {
        super();
    }

    public BlastTestMultiFastaXMLPersist(String name) {
        super(name);
    }

    protected void setUp() throws Exception {
        super.setUp();
        setExpectedHits(EXPECTED_HITS);
        setExpectedBlastResultsZipFileName("gosChesapeake32SequenceResults.xml");
        setBlastInputFileName("clean-multi-fasta-32.fasta");
        setBlastDatasetName(GOS_CHESAPEAKE_BAY);
    }

    public void testBlast() throws Exception {
        submitJobAndWaitForCompletion("TestBlastXmlOut");
        validateHitsAndBlastResultContent();
    }

    public void testAllPojoBlast() throws Exception {
        submitJobAndWaitForCompletion("TestAllPojoBlastXmlOut");
        validateHitsAndBlastResultContent();
    }

    public void testAllPojoBlastLaunchedByBlastLauncherMdb() throws Exception {
        submitJobAndWaitForCompletion("TestAllPojoBlastLaunchedByBlastLauncherMdbXmlOut");
        validateHitsAndBlastResultContent();
    }

    public void testAllPojoBlastLaunchedByProcessLauncherMdb() throws Exception {
        submitJobAndWaitForCompletion("TestAllPojoBlastLaunchedByProcessLauncherMdbXmlOut");
        validateHitsAndBlastResultContent();
    }

    public void testAllPojoBlastLaunchedBySequenceLauncherMdb() throws Exception {
        submitJobAndWaitForCompletion("TestAllPojoBlastLaunchedBySequenceLauncherMdbXmlOut");
        validateHitsAndBlastResultContent();
    }

    public void testAllMdbBlastControlledByProcessLauncherSlsb() throws Exception {
        submitJobAndWaitForCompletion("TestAllMdbBlastControlledByProcessLauncherSlsbXmlOut");
        validateHitsAndBlastResultContent();
    }

    public void testAllMdbBlastControlledByProcessLauncherPojo() throws Exception {
        submitJobAndWaitForCompletion("TestAllMdbBlastControlledByProcessLauncherPojoXmlOut");
        validateHitsAndBlastResultContent();
    }

    public void testAllMdbBlastControlledByProcessLauncherMdb() throws Exception {
        submitJobAndWaitForCompletion("TestAllMdbBlastControlledByProcessLauncherMdbXmlOut");
        validateHitsAndBlastResultContent();
    }

    /**
     * Tests MultiFastaSplitterService
     *
     * @throws Exception
     */
    public void testBlastQueryCountCutoffPlusOneMore() throws Exception {
        setExpectedHits(82207L);
        setExpectedBlastResultsZipFileName("gosChesapeake12646SequenceResults.xml");
        setBlastInputFileName("multi-fasta-12646.fasta");
        submitJobAndWaitForCompletion("TestBlastXmlOut");
        validateHitsAndBlastResultContent();
    }

    /**
     * Tests MultiFastaSplitterService
     *
     * @throws Exception
     */
    public void testBlastQueryCountCutoff() throws Exception {
        setExpectedHits(82182L);
        setExpectedBlastResultsZipFileName("gosChesapeake12645SequenceResults.xml");
        setBlastInputFileName("multi-fasta-12645.fasta");
        submitJobAndWaitForCompletion("TestBlastXmlOut");
        validateHitsAndBlastResultContent();
    }

    /**
     * Tests MultiFastaSplitterService with multiple partitions
     *
     * @throws Exception
     */
    public void testBlastQueryCountCutoffMinusOneAndMultiPartition() throws Exception {
        setExpectedHits(76582L);
        setExpectedBlastResultsZipFileName("gosChesapeake12644SequenceResults.xml");
        setBlastDatasetName(MARINE_VIROMES);
        setBlastInputFileName("multi-fasta-12644.fasta");
        submitJobAndWaitForCompletion("TestBlastXmlOut");
        validateHitsAndBlastResultContent();
    }


}
