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

import org.janelia.it.jacs.shared.utils.DateUtil;

import java.text.NumberFormat;

/**
 * This class encapsulates the status of a line item in the Dma status report e.g. the progress for panda_pep_04.niaa
 * fasta import.
 *
 * @author Tareq Nabeel
 */
public class Progress {

    private String itemName;

    private long totalSequencesProcessed;
    private long sequencesProcessedSinceLastComputation;
    private long sequencesInError;
    private long targetSequences;
    private float percentSeqComplete;
    private int avgSequencePerSec;
    private int actualSequencePerSec;
    private double avgBytesPerSec;
    private double actualBytesPerSec;
    private long totalBytesProcessed;
    private long totalTargetBytes;
    private float percentMBComplete;

    private String strTotalElapsedTime;
    private String strTotalSequencesProcessed;
    private String strSequencesProcessedSinceLastComputation;
    private String strSequencesInError;
    private String strTargetSequences;
    private String strPercentSeqComplete;
    private String strAvgSequencePerSec;
    private String strActualSequencePerSec;
    private String strTotalBytesProcessed;
    private String strTotalTargetBytes;
    private String strMBPercentComplete;
    private String strAvgBytesPerSec;
    private String strActualBytesPerSec;

    /**
     * @param itemName                 the name to be displayed in the report for this item
     * @param totalBytesProcessed
     * @param totalTargetBytes
     * @param totalSequencesProcessed
     * @param targetSequences
     * @param sequencesInError
     * @param sequencesProcessedSinceLastComputation
     *
     * @param totalElapsedTime
     * @param elapsedTimeSinceLastComputation
     *
     * @param previousPercentComplete
     * @param previousAvgSeqSec
     * @param previousTotalElapsedTime
     * @param bytesProcessedSinceLastComputation
     *
     * @param previousAvgBytesSec
     */
    public Progress(String itemName, long totalBytesProcessed, long totalTargetBytes, long totalSequencesProcessed, long targetSequences, long sequencesInError, long sequencesProcessedSinceLastComputation, long totalElapsedTime, long elapsedTimeSinceLastComputation, float previousPercentComplete, int previousAvgSeqSec, String previousTotalElapsedTime, long bytesProcessedSinceLastComputation, double previousAvgBytesSec) {
        this.itemName = itemName;
        NumberFormat numFormat = NumberFormat.getNumberInstance();
        NumberFormat wholeNumFormat = NumberFormat.getNumberInstance();

        this.totalBytesProcessed = totalBytesProcessed;
        float mbProcessed = ((float) totalBytesProcessed / 1000000);
        numFormat.setMaximumFractionDigits(2);
        numFormat.setMinimumFractionDigits(2);
        strTotalBytesProcessed = numFormat.format(mbProcessed);

        this.totalTargetBytes = totalTargetBytes;
        if (totalTargetBytes > 0) {
            float mbTarget = ((float) totalTargetBytes / 1000000);
            strTotalTargetBytes = numFormat.format(mbTarget);
            this.percentMBComplete = mbProcessed / mbTarget * 100;
            strMBPercentComplete = numFormat.format(percentMBComplete) + "%";
        }
        else {
            this.percentMBComplete = -1;
            strTotalTargetBytes = "NA";
            strMBPercentComplete = "NA";
        }

        this.totalSequencesProcessed = totalSequencesProcessed;
        this.strTotalSequencesProcessed = wholeNumFormat.format(totalSequencesProcessed);

        this.sequencesProcessedSinceLastComputation = sequencesProcessedSinceLastComputation;
        this.strSequencesProcessedSinceLastComputation = wholeNumFormat.format(sequencesProcessedSinceLastComputation);

        this.sequencesInError = sequencesInError;
        this.strSequencesInError = wholeNumFormat.format(sequencesInError);

        this.targetSequences = targetSequences;
        if (targetSequences > 0) {
            strTargetSequences = wholeNumFormat.format(targetSequences);
            this.percentSeqComplete = ((float) totalSequencesProcessed / targetSequences * 100);
            strPercentSeqComplete = numFormat.format(percentSeqComplete) + "%";
        }
        else {
            this.percentSeqComplete = -1;
            strTargetSequences = "NA";
            strPercentSeqComplete = "NA";
        }

        if (previousPercentComplete != 100) {
            this.avgSequencePerSec = (int) (((double) totalSequencesProcessed) / totalElapsedTime * 1000);
            strAvgSequencePerSec = wholeNumFormat.format(avgSequencePerSec);
            this.avgBytesPerSec = ((double) totalBytesProcessed) / totalElapsedTime * 1000;
            this.strAvgBytesPerSec = wholeNumFormat.format(avgBytesPerSec);
            strTotalElapsedTime = DateUtil.getElapsedTime(totalElapsedTime, false);
        }
        else {
            this.avgSequencePerSec = previousAvgSeqSec;
            strAvgSequencePerSec = wholeNumFormat.format(previousAvgSeqSec);
            avgBytesPerSec = previousAvgBytesSec;
            strAvgBytesPerSec = wholeNumFormat.format(previousAvgBytesSec);
            strTotalElapsedTime = previousTotalElapsedTime;
        }

        this.actualSequencePerSec = (int) (((double) sequencesProcessedSinceLastComputation) / elapsedTimeSinceLastComputation * 1000);
        this.strActualSequencePerSec = wholeNumFormat.format(actualSequencePerSec);
        this.actualBytesPerSec = ((double) bytesProcessedSinceLastComputation) / elapsedTimeSinceLastComputation * 1000;
        strActualBytesPerSec = wholeNumFormat.format(actualBytesPerSec);
    }


    public long getTotalSequencesProcessed() {
        return totalSequencesProcessed;
    }

    public long getSequencesProcessedSinceLastComputation() {
        return sequencesProcessedSinceLastComputation;
    }

    public long getSequencesInError() {
        return sequencesInError;
    }

    public long getTargetSequences() {
        return targetSequences;
    }

    public float getPercentSeqComplete() {
        return percentSeqComplete;
    }

    public int getAvgSequencePerSec() {
        return avgSequencePerSec;
    }

    public int getActualSequencePerSec() {
        return actualSequencePerSec;
    }

    public String getStrTotalElapsedTime() {
        return strTotalElapsedTime;
    }

    public String getStrTotalSequencesProcessed() {
        return strTotalSequencesProcessed;
    }

    public String getStrSequencesProcessedSinceLastComputation() {
        return strSequencesProcessedSinceLastComputation;
    }

    public String getStrSequencesInError() {
        return strSequencesInError;
    }

    public String getStrTargetSequences() {
        return strTargetSequences;
    }

    public String getStrPercentSeqComplete() {
        return strPercentSeqComplete;
    }

    public String getStrAvgSequencePerSec() {
        return strAvgSequencePerSec;
    }

    public String getStrActualSequencePerSec() {
        return strActualSequencePerSec;
    }

    public String getItemName() {
        return itemName;
    }

    public long getTotalBytesProcessed() {
        return totalBytesProcessed;
    }

    public long getTotalTargetBytes() {
        return totalTargetBytes;
    }

    public String getStrTotalBytesProcessed() {
        return strTotalBytesProcessed;
    }

    public String getStrTotalTargetBytes() {
        return strTotalTargetBytes;
    }

    public float getPercentMBComplete() {
        return percentMBComplete;
    }

    public String getStrMBPercentComplete() {
        return strMBPercentComplete;
    }

    public double getAvgBytesPerSec() {
        return avgBytesPerSec;
    }

    public double getActualBytesPerSec() {
        return actualBytesPerSec;
    }

    public String getStrAvgBytesPerSec() {
        return strAvgBytesPerSec;
    }

    public String getStrActualBytesPerSec() {
        return strActualBytesPerSec;
    }
}
