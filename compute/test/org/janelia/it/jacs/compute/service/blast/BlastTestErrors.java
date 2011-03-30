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
import org.janelia.it.jacs.compute.api.EJBFactory;

/**
 * Created by IntelliJ IDEA.
 * User: tnabeel
 * Date: May 22, 2007
 * Time: 2:58:43 PM
 *
 */
public class BlastTestErrors extends BlastTestBase {

    public BlastTestErrors() {
        super();
    }

    public BlastTestErrors(String name) {
        super(name);
    }

    protected void setUp() throws Exception {
        super.setUp();
        setBlastDatasetName(GOS_CHESAPEAKE_BAY);
    }

    public void testBlastAllWarnings() throws Exception {
        setExpectedHits(4L);
        setExpectedMessageCount(7);
        setBlastInputFastaText(">62005829\nGTGATGGTGGTGGCGGTGGTGATGGTGATGGTGATGGTGATGGT\n>62004030\nTGATGATGATGATGATGATGATGATGATGATGATGAGGATGAGGATGATGATGATGATGA\n>62006130\nTGATGATGATTGATGATGATGATGATGATGATGCTGATGATGATGATGATGATGATGA\n>62009466\nCTGCAGTCAGCTTCAAAATCGCATCATTTCTTAGGAGTAAACGAAAAT\n>62009467\nAAAAAATGTTCAATCATGTTCAAAAATACTGAAATTTCAAAAAACATATTTATCGAAATAAAAAAAAAAAAAAAA\n>62001882\nCAGACAGACAGACAGACAGACCGACAGACAGACAGACAGACAGACAGACAGACAGACAGACAGACGAGAC\n>62001369\nGTGTGTCATTGTGTGTCATTGTGTGTCATTGTGTGTCATTGTGTGTGTATCATAATGTGTG\n>62004557\nATATATTGATATATTGATATATTGATATATTGATATATTGATATATTGATATATTGATAT");
        submitJobAndWaitForCompletion("TestBlast");
        validateHits();
        validateMessageCount();
    }

    public void testBlastWarningsAndSuccess() throws Exception {
        setExpectedHits(22L);
        setExpectedMessageCount(6);
        setBlastInputFastaText(">mydefine\nTTGGGGATCGTGCTGGGTGTCATTGTTGCTTTCGTGTCAGCGGTGGTTGCTGTTGGTCGCTTCCTGATTG\n>62004030\nTGATGATGATGATGATGATGATGATGATGATGATGAGGATGAGGATGATGATGATGATGA\n>62006130\nTGATGATGATTGATGATGATGATGATGATGATGCTGATGATGATGATGATGATGATGA\n>62009466\nCTGCAGTCAGCTTCAAAATCGCATCATTTCTTAGGAGTAAACGAAAAT\n>62009467\nAAAAAATGTTCAATCATGTTCAAAAATACTGAAATTTCAAAAAACATATTTATCGAAATAAAAAAAAAAAAAAAA\n>62001882\nCAGACAGACAGACAGACAGACCGACAGACAGACAGACAGACAGACAGACAGACAGACAGACAGACGAGAC\n>62001369\nGTGTGTCATTGTGTGTCATTGTGTGTCATTGTGTGTCATTGTGTGTGTATCATAATGTGTG\n>62004557\nATATATTGATATATTGATATATTGATATATTGATATATTGATATATTGATATATTGATAT");
        submitJobAndWaitForCompletion("BlastWithGridMerge");
        validateHits();
        validateMessageCount();
    }

    public void testBlastnNoHits() throws Exception {
        setExpectedHits(0L);
        setExpectedMessageCount(0);
        setBlastInputFastaText(">mydefine\n4444444444");
        submitJobAndWaitForCompletion("BlastWithGridMerge");
        validateHits();
        validateMessageCount();
    }

    public void testBlastError() throws Exception {
        try {
            submitJobAndWaitForCompletion("TestBlastError");
        } catch (Exception e) {
            verifyErrorCompletion();
            assertNull(EJBFactory.getRemoteComputeBean().getBlastHitCountByTaskId(getTaskId()));
            return;
        }
        fail("Should have caught exception");
    }

    public static Test suite() {
        TestSuite suite = new TestSuite(BlastTestErrors.class);
        return suite;
    }

}
