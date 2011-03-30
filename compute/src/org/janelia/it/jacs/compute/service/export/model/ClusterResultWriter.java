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

package org.janelia.it.jacs.compute.service.export.model;

import org.janelia.it.jacs.compute.access.search.ClusterResult;
import org.janelia.it.jacs.compute.service.export.writers.ExportWriter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Aug 1, 2008
 * Time: 10:20:13 AM
 */
public class ClusterResultWriter {
    ExportWriter exportWriter;
    List<ClusterResult> clusterResults;

    public ClusterResultWriter(ExportWriter exportWriter, List<ClusterResult> clusterResults) {
        this.exportWriter = exportWriter;
        this.clusterResults = clusterResults;
    }

    public void write() throws IOException {
        List<String> headerList = new ArrayList<String>();
        headerList.addAll(ClusterResultFormatter.getHeaderList());
        exportWriter.writeItem(headerList);
        if (clusterResults == null || clusterResults.size() == 0)
            return; // nothing to do
        for (ClusterResult cr : clusterResults) {
            List<String> colList = ClusterResultFormatter.formatColumns(cr);
            exportWriter.writeItem(colList);
        }
    }

}
