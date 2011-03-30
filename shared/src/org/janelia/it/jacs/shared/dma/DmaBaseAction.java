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

/**
 * Base class either with dummy method implements for DmaAction.
 *
 * @author Tareq Nabeel
 */
public abstract class DmaBaseAction implements DmaAction {

    private long startTimeMillis;
    private DmaArgs dmaArgs;
    private Progress progress;
    private ProgressCapturer progressCapturer;
    private DmaFile dmaFile;

    public abstract String getName();

    public DmaBaseAction() {
        startTimeMillis = System.currentTimeMillis();
    }

    public long getStartTime() {
        return startTimeMillis;
    }

    public String getLabel() {
        return getName();
    }

    public Progress getProgress() {
        return progress;
    }

    public void setProgress(Progress progress) {
        this.progress = progress;
    }

    public long getProcessedSeqCount() {
        return 0;
    }

    public long getProcessedByteCount() {
        return 0;
    }

    public long getTargetSeqCount() {
        return 0;
    }

    public long getTargetByteCount() {
        return 0;
    }

    public long getSeqErrorCount() {
        return 0;
    }

    public ProgressCapturer getProgressCapturer() {
        return progressCapturer;
    }

    public void setProgressCapturer(ProgressCapturer progressCapturer) {
        this.progressCapturer = progressCapturer;
    }

    public void setDmaFile(DmaFile dmaFile) {
        this.dmaFile = dmaFile;
    }

    public DmaFile getDmaFile() {
        return dmaFile;
    }

    public void setDmaArgs(DmaArgs dmaArgs) {
        this.dmaArgs = dmaArgs;
    }

    public DmaArgs getDmaArgs() {
        return dmaArgs;
    }
}
