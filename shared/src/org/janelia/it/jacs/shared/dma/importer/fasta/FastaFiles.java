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

package org.janelia.it.jacs.shared.dma.importer.fasta;

import org.janelia.it.jacs.shared.dma.DmaArgs;
import org.janelia.it.jacs.shared.dma.DmaFile;
import org.janelia.it.jacs.shared.dma.DmaFiles;
import org.janelia.it.jacs.shared.utils.FileUtil;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * This class represents a collection of fasta files that are to be consumed by FastaImporter
 *
 * @author Tareq Nabeel
 */
public class FastaFiles extends DmaFiles {

    private static final String os = System.getProperties().getProperty("os.name");
    private static final boolean isWindows = os != null && os.toLowerCase().contains("windows");

    public FastaFiles(List<File> files, String[] extensions, DmaArgs dmaArgs) {
        super(files, extensions, dmaArgs);
    }

    protected long retrieveSequenceCount(DmaFile dmafile) {
        long seqCount = 0;
        if (!isWindows) {
            if (dmaLogger.isDebugEnabled(getClass())) {
                dmaLogger.logDebug("Retrieving count for " + dmafile.getName(), getClass());
            }
            try {
                seqCount = FileUtil.getCountUsingUnixCall("grep \"^>\" " + dmafile.getAbsolutePath() + " | wc -l");
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
            catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        return seqCount;
    }

    protected void initDmaFile(DmaFile dmaFile) {
    }


}
