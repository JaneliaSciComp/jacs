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

package org.janelia.it.jacs.compute.service.export.processor;

import org.janelia.it.jacs.compute.access.FeatureDAO;
import org.janelia.it.jacs.compute.access.MetadataDAO;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.service.export.model.BseCsvWriter;
import org.janelia.it.jacs.compute.service.export.model.BseFastaWriter;
import org.janelia.it.jacs.compute.service.export.writers.ExportCsvWriter;
import org.janelia.it.jacs.compute.service.export.writers.ExportFastaWriter;
import org.janelia.it.jacs.model.common.SortArgument;
import org.janelia.it.jacs.model.genomics.Read;
import org.janelia.it.jacs.model.tasks.export.ExportTask;
import org.janelia.it.jacs.model.user_data.export.ExportFileNode;
import org.janelia.it.jacs.shared.export.ExportWriterConstants;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Jul 11, 2008
 * Time: 4:28:22 PM
 */
public class SampleReadsExportProcessor extends ExportProcessor {

    protected FeatureDAO featureDAO = new FeatureDAO(_logger);
    protected MetadataDAO metadataDAO = new MetadataDAO(_logger);

    public SampleReadsExportProcessor(ExportTask exportTask, ExportFileNode exportFileNode) throws IOException, MissingDataException {
        super(exportTask, exportFileNode);
    }

    public void execute() throws Exception {
        List<String> accList = exportTask.getAccessionList();
        List<Read> readList = new ArrayList<Read>();
        SortArgument[] sortArgs = getSortArgumentsAsArray();
//        System.out.println("There are "+accList.size()+" items.");
        int CHUNK = 5000;
        for (String sampleAcc : accList) {
            int sampleTotal = featureDAO.getNumReadsFromSample(sampleAcc);
            for (int i = 0; i < sampleTotal; i += CHUNK) {
                int nextChunk = CHUNK;
                if ((i + nextChunk) > sampleTotal) {
                    nextChunk = sampleTotal - i;

                }
//                System.out.println("Writing from "+i+" to "+(i+nextChunk));
                readList.addAll(featureDAO.getPagedReadsFromSample(sampleAcc, null /*readAccSet*/, i /*start*/,
                        nextChunk /*numrows*/, sortArgs));
                if (ExportWriterConstants.EXPORT_TYPE_CSV.equals(exportTask.getExportFormatType()) ||
                        ExportWriterConstants.EXPORT_TYPE_EXCEL.equals(exportTask.getExportFormatType())) {
                    ExportCsvWriter exportCsvWriter = (ExportCsvWriter) exportWriter;
                    BseCsvWriter proteinCsvWriter = new BseCsvWriter(exportCsvWriter, readList);
                    proteinCsvWriter.write();
                }
                else if (ExportWriterConstants.EXPORT_TYPE_FASTA.equals(exportTask.getExportFormatType())) {
                    ExportFastaWriter exportFastaWriter = (ExportFastaWriter) exportWriter;
                    BseFastaWriter proteinFastaWriter = new BseFastaWriter(exportFastaWriter, readList);
                    proteinFastaWriter.write();
                }
                else {
                    throw new Exception("Not configured to handle exportFormatType=" + exportTask.getExportFormatType());
                }
            }
        }
//        System.out.println("Finished writing items.");
    }

    public String getProcessorType() {
        return null;
    }

    protected List<SortArgument> getDataHeaders() {
        return null;
    }

}
