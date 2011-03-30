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

package org.janelia.it.jacs.shared.blast;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.model.tasks.blast.*;

/**
 * User: aresnick
 * Date: May 18, 2009
 * Time: 3:23:48 PM
 * <p/>
 * <p/>
 * Description:
 */
public class BlastWriterNCBITabWithHeaderTest extends BlastWriterTest {

    static{
        logger = Logger.getLogger(BlastWriterNCBITabWithHeaderTest.class);
    }

    public BlastWriterNCBITabWithHeaderTest() {
        super();

    }

    public void testBlastnNCBITabWithHeaderOutput() throws Exception {
        blastWriterTestImpl(new BlastNTask(),"blastn.xml");
    }

    public void testBlastpNCBITabWithHeaderOutput() throws Exception {
        blastWriterTestImpl(new BlastPTask(),"blastp.xml");
    }

    public void testBlastxNCBITabWithHeaderOutput() throws Exception {
        blastWriterTestImpl(new BlastXTask(),"blastx.xml");
    }

    public void testMegablastNCBITabWithHeaderOutput() throws Exception {
        blastWriterTestImpl(new MegablastTask(),"megablast.xml");
    }

    public void testTblastnNCBITabWithHeaderOutput() throws Exception {
        blastWriterTestImpl(new TBlastNTask(),"tblastn.xml");
    }

    public void testTblastxNCBITabWithHeaderOutput() throws Exception {
        blastWriterTestImpl(new TBlastXTask(),"tblastx.xml");
    }

    protected BlastWriter getBlastWriter() {
        return new BlastNCBITabWithHeaderWriter();
    }

    protected String getOutputFormatKey() {
        return BlastTask.FORMAT_TAB_WITH_HEADER;
    }
}