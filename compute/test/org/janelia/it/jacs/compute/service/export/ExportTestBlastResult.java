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

package org.janelia.it.jacs.compute.service.export;

import org.janelia.it.jacs.model.tasks.export.BlastResultExportTask;
import org.janelia.it.jacs.shared.export.ExportWriterConstants;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Jun 26, 2008
 * Time: 10:14:16 AM
 *
 */
public class ExportTestBlastResult extends ExportTestBase {
    public static final String TESTUSER2_VS_DEEPMED_READS_JOB_ID="1228121711434531274";
    public static final String TESTUSER2_LRG_BLAST_JOB_ID="1203051837138141559";

    public ExportTestBlastResult() {
        super();
    }

    public ExportTestBlastResult(String name) {
        super(name);
    }

    protected void setUp() throws Exception {
        super.setUp();
    }

    public void testQueryFastaExport() throws Exception {
        BlastResultExportTask task=new BlastResultExportTask(
                TESTUSER2_VS_DEEPMED_READS_JOB_ID,
                BlastResultExportTask.SEQUENCES_QUERY,
                ExportWriterConstants.EXPORT_TYPE_FASTA,
                null, // null indicates we want all sequences for this blast job
                null // SortArgument list not necessary for QueryFasta export
                );
        task.setOwner(TEST_USER_NAME);
        submitJobAndWaitForCompletion("FileExport",task);
    }

    public void testSubjectFastaExport() throws Exception {
        BlastResultExportTask task=new BlastResultExportTask(
                TESTUSER2_VS_DEEPMED_READS_JOB_ID,
                BlastResultExportTask.SEQUENCES_SUBJECT,
                ExportWriterConstants.EXPORT_TYPE_FASTA,
                null, // null indicates we want all sequences for this blast job
                null // SortArgument list not necessary for QueryFasta export
                );
        task.setOwner(TEST_USER_NAME);
        submitJobAndWaitForCompletion("FileExport",task);
    }

    public void testCsvExport() throws Exception {
        BlastResultExportTask task = new BlastResultExportTask(
                TESTUSER2_VS_DEEPMED_READS_JOB_ID,
                BlastResultExportTask.SEQUENCES_ALL,
                ExportWriterConstants.EXPORT_TYPE_CSV,
                null, // null indicates we want all sequences for this blast job
                null // SortArgument list not necessary for QueryFasta export
        );
        task.setOwner(TEST_USER_NAME);
        submitJobAndWaitForCompletion("FileExport", task);
    }

    public void testNcbiXmlExport() throws Exception {
        BlastResultExportTask task=new BlastResultExportTask(
                    TESTUSER2_VS_DEEPMED_READS_JOB_ID,
                BlastResultExportTask.SEQUENCES_ALL,
                ExportWriterConstants.EXPORT_TYPE_NCBI_BLAST_XML,
                null, // we want all sequences
                null  // SortArgument list not necessary for QueryFasta export
                );
        task.setOwner(TEST_USER_NAME);
        submitJobAndWaitForCompletion("FileExport",task);
    }

    public void testNcbiXmlLargeExport() throws Exception {
        BlastResultExportTask task = new BlastResultExportTask(
                TESTUSER2_LRG_BLAST_JOB_ID,
                BlastResultExportTask.SEQUENCES_ALL,
                ExportWriterConstants.EXPORT_TYPE_NCBI_BLAST_XML,
                null, // we want all sequences
                null  // SortArgument list not necessary for QueryFasta export
        );
        task.setOwner(TEST_USER_NAME);
        submitJobAndWaitForCompletion("FileExport", task);
    }

}
