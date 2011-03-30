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

import org.apache.log4j.FileAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.dma.Tag;
import org.janelia.it.jacs.shared.dma.DmaArgs;
import org.janelia.it.jacs.shared.dma.DmaFile;
import org.janelia.it.jacs.shared.dma.entity.SequenceInfo;
import org.janelia.it.jacs.shared.dma.entity.SequenceInfos;
import org.janelia.it.jacs.shared.node.FastaUtil;
import org.janelia.it.jacs.shared.utils.FileUtil;

import java.io.*;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

/**
 * This class is responsible for logging Dma messages, status, new tags, etc.  It wraps log4j
 * for logging messages and statues.
 *
 * @author Tareq Nabeel
 */
public class DmaLogger {
    private static DmaLogger me;
    private PrintWriter newTagWriter;

    private File logDir;
    private File dmaLogFile;
    private File internalIdsDir;
    private File externalIdsDir;
    private long totalEntityInserts;
    private long totalSequenceInserts;
    private long totalAssemblyInserts;
    private long totalTagInserts;

    private DmaLogger(DmaArgs dmaArgs) {
        try {
            logDir = createLogDir();
            addDmaLogFileAppender();

            if (dmaArgs.doFastaImport()) {
                newTagWriter = new PrintWriter(FileUtil.ensureFileExists(logDir.getAbsolutePath() + File.separator + "newTags.txt"));
            }

            // These are a must for processing.  They're being logged under logDir (timestamp directory) to make it
            // easier to track and flush away old runs.
            if (dmaArgs.doInternalEntityImport()) {
                internalIdsDir = FileUtil.ensureDirExists(getSequenceDir().getAbsolutePath(), SystemConfigurationProperties.getString("dma.internalIdsDirPath"), true);
            }
            if (dmaArgs.doExternalEntityImport()) {
                externalIdsDir = FileUtil.ensureDirExists(getSequenceDir().getAbsolutePath(), SystemConfigurationProperties.getString("dma.externalIdsDirPath"), true);
            }

        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void addDmaLogFileAppender() throws IOException {
        Logger rootLogger = org.apache.log4j.Logger.getRootLogger();
        FileAppender fileAppender = new FileAppender();
        fileAppender.setName("dmaLog");
        dmaLogFile = FileUtil.ensureFileExists(logDir.getAbsolutePath() + File.separator + "dma.log");
        fileAppender.setFile(dmaLogFile.getAbsolutePath());
        fileAppender.setWriter(new FileWriter(dmaLogFile));
        fileAppender.setImmediateFlush(true);
        PatternLayout patternLayout = new PatternLayout("[%d{ISO8601}][%-5p][%c{1}]-%m%n");
        fileAppender.setLayout(patternLayout);
        fileAppender.setAppend(false);
        rootLogger.addAppender(fileAppender);
    }


    public synchronized void addEntityInserts(long entityInserts) {
        totalEntityInserts += entityInserts;
    }

    public synchronized void addSequenceInserts(long sequenceInserts) {
        totalSequenceInserts += sequenceInserts;
    }

    public synchronized void addAssemblyInserts(long assemblyInserts) {
        totalAssemblyInserts += assemblyInserts;
    }

    public synchronized void addTagInserts(long tagInserts) {
        totalTagInserts += tagInserts;
    }

    public void cleanup() {
        if (newTagWriter != null) {
            newTagWriter.close();
        }
    }

    public static DmaLogger getInstance() {
        return me;
    }

    public static DmaLogger getInstance(DmaArgs dmaArgs) {
        if (me == null) {
            me = new DmaLogger(dmaArgs);
        }
        return me;
    }

    public void logNewTag(Tag tag) {
        logInfo("New tag added: " + tag, getClass());
        newTagWriter.write(tag.getName());
        newTagWriter.println();
        newTagWriter.flush();
    }

    /**
     * Logs the progress report to the log
     *
     * @param progressReportBuff the progress report
     * @param startTime          time logging originally started
     */
    public void logImportStatus(StringBuilder progressReportBuff, long startTime) {
        progressReportBuff.append("\nLog time: ").append(new Date()).append("\n");
        progressReportBuff.append("Import started at: ").append(new Date(startTime)).append("\n");
//        statusBuff.append("Free memory: ").append(Runtime.getRuntime().freeMemory() / 1000000).append(" MB").append("\n");
        if (totalEntityInserts > 0) {
            progressReportBuff.append("Total external entity inserts: ").append(totalEntityInserts).append("\n");
        }
        if (totalSequenceInserts > 0) {
            progressReportBuff.append("Total external sequence inserts: ").append(totalSequenceInserts).append("\n");
        }
        if (totalAssemblyInserts > 0) {
            progressReportBuff.append("Total assembly inserts: ").append(totalAssemblyInserts).append("\n");
        }
        if (totalTagInserts > 0) {
            progressReportBuff.append("Total tag inserts: ").append(totalTagInserts).append("\n");
        }
        progressReportBuff.append("Progress logged at: ").append(dmaLogFile.getAbsolutePath()).append("\n");
        progressReportBuff.append("-------------------------------------------------------------------------------------------------------------------------------------------");
        logInfo(progressReportBuff.toString(), getClass());

    }

    public boolean isInfoEnabled(Class clazz) {
        return Logger.getLogger(clazz).isInfoEnabled();
    }

    public boolean isDebugEnabled(Class clazz) {
        return Logger.getLogger(clazz).isDebugEnabled();
    }

    private Logger setupLogger(Class clazz) {
        return Logger.getLogger(clazz);
    }

    public void logInfo(String msg, Class clazz) {
        setupLogger(clazz).info(msg);
    }

    public void logDebug(String msg, Class clazz) {
        setupLogger(clazz).debug(msg);
    }

    public void logWarn(String msg, Class clazz) {
        setupLogger(clazz).warn(msg);
    }

    public void logError(String msg, Class clazz) {
        setupLogger(clazz).error(msg);
    }

    public void logError(String msg, Class clazz, Throwable e) {
        setupLogger(clazz).error(msg, e);
    }

    public File getSequenceDir() {
        return logDir;
    }

    public File getInternalIdsDir() {
        return internalIdsDir;
    }

    public File getExternalIdsDir() {
        return externalIdsDir;
    }

    /**
     * This method writes out the sequences that failed to import from a fasta file to
     * files named the same as the original fasta files but under the log directory
     *
     * @param originalFile
     * @param sequenceInfos
     */
    public void logSequencesInError(DmaFile originalFile, SequenceInfos sequenceInfos) {
        Writer sequenceWriter = null;
        try {
            sequenceWriter = getSequencesInErrorWriter(originalFile);
            Map<String, SequenceInfo> parsedSequenceInfos = sequenceInfos.getParsedSequenceInfos();
            for (SequenceInfo sequenceInfo : parsedSequenceInfos.values()) {
                FastaUtil.writeFormattedFasta(sequenceWriter, sequenceInfo.getDefline(), sequenceInfo.getSequence(), 80);
            }
        }
        catch (Exception e) {
//            statusWriter.println("Unable to write out error sequenceInfos in error for " + originalFastaFile.getName());
            e.printStackTrace();
        }
        finally {
            try {
                if (null!=sequenceWriter) {
                    sequenceWriter.close();
                }
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void logSequenceInError(DmaFile originalFile, SequenceInfo sequenceInfo) {
        logSequenceInError(originalFile, sequenceInfo.getDefline(), sequenceInfo.getSequence());
    }

    /**
     * This method writes out the sequence entry that failed to import from a fasta file to
     * a file named the same as the original fasta file but under the log directory
     *
     * @param originalFile
     * @param defline
     * @param sequence
     */
    public void logSequenceInError(DmaFile originalFile, String defline, String sequence) {
        Writer sequenceWriter = null;
        try {
            sequenceWriter = getSequencesInErrorWriter(originalFile);
            FastaUtil.writeFormattedFasta(sequenceWriter, defline, sequence, 80);
        }
        catch (Exception e) {
//            statusWriter.println("Unable to write out error sequenceInfos in error for " + originalFastaFile.getName());
            e.printStackTrace();
        }
        finally {
            try {
                if (null!=sequenceWriter) {
                    sequenceWriter.close();
                }
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private Writer getSequencesInErrorWriter(DmaFile originalFile) throws IOException {
        String sequenceErrorFilePath = this.logDir.getAbsolutePath() + File.separator + originalFile.getName();
        return new BufferedWriter(new FileWriter(sequenceErrorFilePath, true));
    }

    private File createLogDir() throws IOException {
        Calendar now = Calendar.getInstance();
        String childDir = String.valueOf(now.get(Calendar.YEAR)) + String.valueOf(now.get(Calendar.MONTH) + 1) + String.valueOf(now.get(Calendar.DAY_OF_MONTH)) + "_" + now.get(Calendar.HOUR_OF_DAY) + now.get(Calendar.MINUTE) + now.get(Calendar.SECOND);
        File parentDir = FileUtil.ensureDirExists(SystemConfigurationProperties.getString("dma.logDir"));
        return FileUtil.ensureDirExists(parentDir.getCanonicalPath(), childDir, true);
    }

}
