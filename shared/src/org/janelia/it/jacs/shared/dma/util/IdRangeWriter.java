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

package org.janelia.it.jacs.shared.dma.util;

import org.janelia.it.jacs.shared.dma.entity.EntityIdRange;
import org.janelia.it.jacs.shared.dma.entity.MutableBoolean;
import org.janelia.it.jacs.shared.dma.entity.MutableLong;
import org.janelia.it.jacs.shared.dma.reporter.DmaLogger;
import org.janelia.it.jacs.shared.utils.FileUtil;

import java.io.*;
import java.util.List;

/**
 * This class is responsible writing out a range of entity ids to the file system
 *
 * @author Tareq Nabeel
 */
public class IdRangeWriter {

    public static void writeEntities(List<EntityIdRange> entityIdChunkRanges, MutableLong processedRecords, MutableLong entityId, String outputDirPath, String filePrefix, long totalRecordsPerFile, long batchSize) {
        try {

            MutableLong fileIndex = new MutableLong(0);
            MutableLong fileRecords = new MutableLong(0);
            BufferedWriter writer = createNewWriter(null, fileIndex, fileRecords, outputDirPath, filePrefix);

            MutableLong entityCountInBatch = new MutableLong(0);
            MutableBoolean endedRange = new MutableBoolean(true);
            EntityIdRange entityIdChunkRange = null;
            for (EntityIdRange entityIdChunkRange1 : entityIdChunkRanges) {
                entityIdChunkRange = entityIdChunkRange1;
                processedRecords.add(entityIdChunkRange.getCount());
                entityCountInBatch.add(entityIdChunkRange.getCount());
                fileRecords.add(entityIdChunkRange.getCount());
                entityId.setValue(entityIdChunkRange.getStart());
                if (endedRange.getValue()) {
                    writer.write(String.valueOf(entityId.getValue()));
                    writer.write(" and ");
                }
                writer = writeBatchToFile(writer, entityIdChunkRange, endedRange, entityCountInBatch, fileRecords, fileIndex, outputDirPath, filePrefix, batchSize, totalRecordsPerFile);
            }
            if (entityCountInBatch.getValue() > 0 && !endedRange.getValue() && entityIdChunkRange != null) {
                writer.write(String.valueOf(entityIdChunkRange.getEnd()));
                writer.write("(" + entityCountInBatch.getValue() + ")");
                writer.newLine();
            }
            writeFileCount(fileRecords, outputDirPath, filePrefix, fileIndex);
            writer.close();
            if (fileRecords.getValue() == 0) {
                // writeBatchToFile created an empty file for next iteration of entityIdChunkRanges
                FileUtil.deleteFile(outputDirPath, getIdFileName(filePrefix, fileIndex));
            }
        }
        catch (Exception e) {
            throw new RuntimeException("IdRangeWriter createFiles failed at processedRecord " + processedRecords, e);
        }
    }

    private static BufferedWriter writeBatchToFile(BufferedWriter writer, EntityIdRange entityIdChunkRange, MutableBoolean endedRange, MutableLong entityCountInBatch, MutableLong fileRecords, MutableLong fileIndex, String outputDirPath, String filePrefix, long batchSize, long totalRecordsPerFile) throws IOException {
        if (entityCountInBatch.getValue() > batchSize) {
            writer.write(String.valueOf(entityIdChunkRange.getEnd()));
            writer.write("(" + entityCountInBatch.getValue() + ")");
            writer.newLine();
            entityCountInBatch.setValue(0);
            endedRange.setValue(true);
            if (fileRecords.getValue() > totalRecordsPerFile) {
//                writer.write(" fileRecords=" + fileRecords.getValue());
                writeFileCount(fileRecords, outputDirPath, filePrefix, fileIndex);
                writer.close();
                writer = createNewWriter(writer, fileIndex, fileRecords, outputDirPath, filePrefix);
            }
        }
        else {
            endedRange.setValue(false);
        }
        return writer;
    }

    private static BufferedWriter createNewWriter(Writer writer, MutableLong fileIndex, MutableLong fileRecords, String outputDirPath, String filePrefix) throws IOException {
        if (writer != null) {
            writer.close();
        }
        fileIndex.increment();
        fileRecords.setValue(0);
        File idsFile = FileUtil.ensureFileExists(outputDirPath, getIdFileName(filePrefix, fileIndex), true);
        BufferedWriter newWriter = new BufferedWriter(new FileWriter(idsFile));
        DmaLogger.getInstance().logInfo("idsFile=" + idsFile, IdRangeWriter.class);
        return newWriter;
    }

    private static void writeFileCount(MutableLong fileRecords, String outputDirPath, String filePrefix, MutableLong fileIndex) throws IOException {
        PrintWriter pw = new PrintWriter(FileUtil.ensureFileExists(outputDirPath, getCountFileName(filePrefix, fileIndex), true));
        try {
            pw.write(String.valueOf(fileRecords.getValue()));
        }
        finally {
            pw.close();
        }
    }

    private static String getIdFileName(String filePrefix, MutableLong fileIndex) {
        return filePrefix + fileIndex.getValue() + "." + filePrefix;
    }

    private static String getCountFileName(String filePrefix, MutableLong fileIndex) {
        return filePrefix + fileIndex.getValue() + "." + "count";
    }
}
