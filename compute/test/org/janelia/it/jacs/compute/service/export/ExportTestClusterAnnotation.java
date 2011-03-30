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

import org.janelia.it.jacs.model.tasks.export.ClusterProteinAnnotationExportTask;
import org.janelia.it.jacs.shared.export.ExportWriterConstants;

import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Jul 10, 2008
 * Time: 4:26:52 PM
 *
 */
public class ExportTestClusterAnnotation extends ExportTestBase {

    public ExportTestClusterAnnotation() {
        super();
    }

    public ExportTestClusterAnnotation(String name) {
        super(name);
    }

    protected void setUp() throws Exception {
        super.setUp();
    }

    public void testClusterAnnotationFastaExport() throws Exception {
        ArrayList<String> accessionList=new ArrayList<String>();
        accessionList.add("CAM_CL_647");
        String annotationID="CAM_CNAM_57077";
        ClusterProteinAnnotationExportTask task = new ClusterProteinAnnotationExportTask(
                annotationID,
                false /* is NR only */,
                ExportWriterConstants.EXPORT_TYPE_FASTA,
                accessionList,
                null // SortArgument list not necessary
        );
        task.setOwner(TEST_USER_NAME);
        submitJobAndWaitForCompletion("FileExport", task);
    }

}
