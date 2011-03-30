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

import org.janelia.it.jacs.shared.dma.reporter.DmaLogger;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * This class represents a collection of files or directories used by DmaThreads and instances of
 * DmaAction
 *
 * @author Tareq Nabeel
 */
public abstract class DmaFiles {

    // Some features (e.g. fasta file sequence count computation for reporting) would not work
    // on Windows... so we need to keep track of OS
    private static final String os = System.getProperties().getProperty("os.name");

    // This list will not change once it's initialized
    private List<DmaFile> dmaFileList = new ArrayList<DmaFile>();

    // This list will shrink as DmaThreads consumes files for processing
    private List<DmaFile> dmaFileListToBeProcessed = new ArrayList<DmaFile>();

    protected long totalFilesSize;

    // Extensions to filter by when setting up an instance of DmaFiles using
    // a director or list of directories
    private String[] extensions;

    // The target sequence count
    protected long targetSeqCount;

    // Comes into play when broadcasting targetSeqCount
    private int filesSequencesRetreivedFor;

    protected abstract long retrieveSequenceCount(DmaFile dmafile);

    protected abstract void initDmaFile(DmaFile dmaFile);

    protected DmaLogger dmaLogger = DmaLogger.getInstance();

    protected DmaArgs dmaArgs;

    public DmaFiles() {
    }

    /**
     * @param file       the directory to look for files with extensions in <code>extensions</code>
     *                   or the file itself
     * @param extensions what to filter by
     * @param dmaArgs    how the Dma run was configured
     */
    public DmaFiles(File file, String[] extensions, DmaArgs dmaArgs) {
        this.extensions = extensions;
        this.dmaArgs = dmaArgs;
        addFile(file);
        sortFiles();
        retrieveSequenceCounts();
    }

    /**
     * @param fileList   List of directories to look for files with extensions in <code>extensions</code>
     *                   or list of files
     * @param extensions what to filter by
     * @param dmaArgs    how the Dma run was configured
     */
    public DmaFiles(List<File> fileList, String[] extensions, DmaArgs dmaArgs) {
        this.extensions = extensions;
        this.dmaArgs = dmaArgs;
        for (File file : fileList) {
            addFile(file);
        }
        sortFiles();
        retrieveSequenceCounts();
    }

    /**
     * Recursively adds files that matches provided extensions in the directory
     * <code>file</code>
     *
     * @param file the directory to recurse through
     */
    private void addFile(File file) {
        if (file.isDirectory()) {
            File[] fastaFiles = file.listFiles();
            for (File fastaFile : fastaFiles) {
                if (fastaFile.isDirectory()) {
                    addFile(fastaFile);
                }
                else {
                    filterDmaFile(fastaFile);
                }
            }
        }
        else {
            filterDmaFile(file);
        }
    }

    /**
     * Skip or add the file to the internal lists depending on whether or not it matched
     * the provided extensions
     *
     * @param file file or directory to filter
     */
    private void filterDmaFile(File file) {
        try {
            String fileName = file.getName();
            if (matchesExtension(fileName)) {
                addDmaFile(file);
            }
            else {
                if (dmaLogger.isDebugEnabled(getClass())) {
                    dmaLogger.logDebug("Skipping " + fileName, this.getClass());
                }
            }
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Adds the file or directory to the internal lists
     *
     * @param file
     */
    public void addDmaFile(File file) {
        DmaFile dmaFile = createDmaFile(file);
        addDmaFileSize(file);
        addDmaFile(dmaFile);
    }

    public void addDmaFile(DmaFile dmaFile) {
        dmaFileList.add(dmaFile);
        dmaFileListToBeProcessed.add(dmaFile);
        initDmaFile(dmaFile);
        if (dmaLogger.isDebugEnabled(getClass())) {
            dmaLogger.logDebug("Added " + dmaFile.getName() + " for processing", this.getClass());
        }
    }

    protected DmaFile createDmaFile(File file) {
        return new DmaFile(file);
    }

    /**
     * Sort the files added to DmaFiles based on asc or desc order size
     * of the files
     */
    public void sortFiles() {
        if (dmaArgs.isAscFileOrder()) {
            Collections.sort(dmaFileList, createAscSizeComparator());
            Collections.sort(dmaFileListToBeProcessed, createAscSizeComparator());
        }
        else {
            Collections.sort(dmaFileList, createDscSizeComparator());
            Collections.sort(dmaFileListToBeProcessed, createDscSizeComparator());
        }
    }

    protected Comparator<DmaFile> createAscSizeComparator() {
        return new DFAscSizeComparator();
    }

    protected Comparator<DmaFile> createDscSizeComparator() {
        return new DFDescSizeComparator();
    }

    /**
     * Return the target sequence count if we've completed sequence computation
     * for all the files.
     *
     * @return
     */
    public long getTargetSeqCount() {
        if (filesSequencesRetreivedFor == originalSize()) {
            return targetSeqCount;
        }
        else {
            return 0;
        }
    }

    /**
     * Retrieve the sequence counts asynchronously as the computation could take long
     */
    public void retrieveSequenceCounts() {
        for (DmaFile dmafile : getDmaFileList()) {
            Thread filePreparerThread = new Thread(new SeqCountRetriever(dmafile));
            filePreparerThread.start();
        }
        try {
            Thread.sleep(5000);
        }
        catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Fire off seqeunce retrieval asynchronously
     */
    private class SeqCountRetriever implements Runnable {
        private DmaFile dmafile;

        public SeqCountRetriever(DmaFile dmafile) {
            this.dmafile = dmafile;
        }

        public void run() {
            try {
                if (dmaLogger.isDebugEnabled(this.getClass())) {
                    dmaLogger.logDebug("Retrieving sequence count for " + dmafile.getName(), getClass());
                }
                long seqCount = retrieveSequenceCount(dmafile);
                dmafile.setTargetSeqCount(seqCount);
                targetSeqCount += seqCount;
                filesSequencesRetreivedFor++;
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void setTargetSeqCount(long targetSeqCount) {
        this.targetSeqCount = targetSeqCount;
    }

    public List<DmaFile> getDmaFileList() {
        return dmaFileList;
    }

    public List<DmaFile> getDmaFileListToBeProcessed() {
        return dmaFileListToBeProcessed;
    }

    public int originalSize() {
        return dmaFileList.size();
    }

    public int currentSize() {
        return dmaFileListToBeProcessed.size();
    }

    public DmaFile removeLast() {
        return dmaFileListToBeProcessed.remove(dmaFileListToBeProcessed.size() - 1);
    }

    protected void addDmaFileSize(File file) {
        totalFilesSize += file.length();
    }

    private boolean matchesExtension(String fileName) {
        if (extensions == null || extensions.length == 0) {
            return true;
        }
        for (String extension : extensions) {
            if (fileName.endsWith(extension)) {
                return true;
            }
        }
        return false;
    }

    public synchronized long getTotalFilesSize() {
        return totalFilesSize;
    }

    public synchronized void setTotalFilesSize(long totalFilesSize) {
        this.totalFilesSize = totalFilesSize;
    }

    public DmaArgs getDmaArgs() {
        return dmaArgs;
    }

    public void setDmaArgs(DmaArgs dmaArgs) {
        this.dmaArgs = dmaArgs;
    }

    private class DFAscSizeComparator implements Comparator<DmaFile> {
        public int compare(DmaFile o1, DmaFile o2) {
            long thisVal = o2.getJavaFileSize();
            long anotherVal = o1.getJavaFileSize();
            return (thisVal < anotherVal ? -1 : (thisVal == anotherVal ? 0 : 1));
        }
    }

    private class DFDescSizeComparator implements Comparator<DmaFile> {
        public int compare(DmaFile o1, DmaFile o2) {
            long thisVal = o1.getJavaFileSize();
            long anotherVal = o2.getJavaFileSize();
            return (thisVal < anotherVal ? -1 : (thisVal == anotherVal ? 0 : 1));
        }
    }
}
