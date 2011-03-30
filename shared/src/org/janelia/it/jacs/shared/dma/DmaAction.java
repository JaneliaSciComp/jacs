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

package org.janelia.it.jacs.shared.dma;

import org.janelia.it.jacs.shared.dma.reporter.Progress;
import org.janelia.it.jacs.shared.dma.reporter.ProgressCapturer;

import java.io.IOException;

/**
 * This class represents all the different actions (e.g. import fasta data, generate datasets, etc.)
 * that can be performed in the DMA module.  DmaThreads and ProgressCapturer works with instances
 * of this class.  We could probably refactor some of these methods into other interfaces to avoid
 * empty method implementations in some DmaAction classes
 *
 * @author Tareq Nabeel
 */
public interface DmaAction {

    /**
     * Only method that must be implemented
     */
    public void execute() throws IOException;

    /**
     * Must be unique within a DmaThreads instance as it's used
     * to remove the action from live threads
     *
     * @return
     */
    public String getName();

    /**
     * Used for reporting to calcuate total processing time and averages
     *
     * @return
     */
    public long getStartTime();

    /**
     * Used for reporting the action as a line item on the progress report
     *
     * @return
     */
    public String getLabel();

    /**
     * Used for reporting
     *
     * @return
     */
    public Progress getProgress();

    /**
     * Used for reporting
     *
     * @return
     */
    public long getProcessedSeqCount();

    /**
     * Used for reporting
     *
     * @return
     */
    public long getProcessedByteCount();

    /**
     * Used for reporting
     *
     * @return
     */
    public long getTargetSeqCount();

    /**
     * Used for reporting
     *
     * @return
     */
    public long getTargetByteCount();

    /**
     * Used for reporting
     *
     * @return
     */
    public long getSeqErrorCount();

    /**
     * Used for reporting
     *
     * @return
     */
    public void setProgressCapturer(ProgressCapturer progressCapturer);

    /**
     * Would be needed by implementing DmaAction class if it uses the dmaFile for
     * processing
     *
     * @param dmaFile
     */
    public void setDmaFile(DmaFile dmaFile);

    public DmaFile getDmaFile();

    /**
     * Can be used by implementing DmaAction class if it chooses to make it's functionality
     * configurable at runtime
     *
     * @param dmaArgs
     */
    public void setDmaArgs(DmaArgs dmaArgs);

    public DmaArgs getDmaArgs();

}
