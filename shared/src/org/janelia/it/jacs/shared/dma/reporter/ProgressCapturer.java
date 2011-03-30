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

package org.janelia.it.jacs.shared.dma.reporter;

import org.janelia.it.jacs.shared.dma.DmaAction;
import org.janelia.it.jacs.shared.dma.entity.MutableLong;

/**
 * This class is responsible for capturing the progress of DmaAction instances
 *
 * @author Tareq Nabeel
 */
public class ProgressCapturer {

    private DmaAction dmaAction;
    private long startTimeMillisSinceLastCapture;
    private long sequencesProcessedSinceLastCapture;
    private long bytesProcessedSinceLastCapture;
    private float previousPercentComplete;
    private int previousAvSeqPerSec;
    private String previousTotalElapsedTime;
    private double previousAvBytesPerSec;

    public ProgressCapturer(DmaAction dmaAction) {
        this.dmaAction = dmaAction;
    }

    public Progress capture(MutableLong totalBytesProcessed, MutableLong totalRecordsProcessed, MutableLong seqErrorCount) {
        long processedByteCount;
        long processedSequenceCount;
        long totalSeqErrorCount;
        if (totalBytesProcessed == null) {
            processedByteCount = dmaAction.getProcessedByteCount();
        }
        else {
            processedByteCount = totalBytesProcessed.getValue();
        }
        if (totalRecordsProcessed == null) {
            processedSequenceCount = dmaAction.getProcessedSeqCount();
        }
        else {
            processedSequenceCount = totalRecordsProcessed.getValue();
        }
        if (seqErrorCount == null) {
            totalSeqErrorCount = dmaAction.getSeqErrorCount();
        }
        else {
            totalSeqErrorCount = seqErrorCount.getValue();
        }
        long currentTimeMillis = System.currentTimeMillis();
        long totalElapsedTimeMillis = 0;
        if (dmaAction.getStartTime() != 0) {
            totalElapsedTimeMillis = currentTimeMillis - dmaAction.getStartTime();
        }
        long elapsedTimeMillisSinceLastComputation = currentTimeMillis - startTimeMillisSinceLastCapture;
        sequencesProcessedSinceLastCapture = processedSequenceCount - sequencesProcessedSinceLastCapture;
        bytesProcessedSinceLastCapture = processedByteCount - bytesProcessedSinceLastCapture;

        Progress progress = new Progress(dmaAction.getLabel(), processedByteCount, dmaAction.getTargetByteCount(), processedSequenceCount, dmaAction.getTargetSeqCount(), totalSeqErrorCount, this.sequencesProcessedSinceLastCapture, totalElapsedTimeMillis, elapsedTimeMillisSinceLastComputation, this.previousPercentComplete, this.previousAvSeqPerSec, previousTotalElapsedTime, bytesProcessedSinceLastCapture, previousAvBytesPerSec);

        this.previousPercentComplete = progress.getPercentSeqComplete();
        this.previousAvSeqPerSec = progress.getAvgSequencePerSec();
        this.previousAvBytesPerSec = progress.getAvgBytesPerSec();
        this.previousTotalElapsedTime = progress.getStrTotalElapsedTime();

        startTimeMillisSinceLastCapture = currentTimeMillis;
        sequencesProcessedSinceLastCapture = processedSequenceCount;
        bytesProcessedSinceLastCapture = processedByteCount;

        return progress;
    }

    public Progress capture() {
        return capture(null, null, null);
    }

}
