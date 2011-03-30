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

package org.janelia.it.jacs.compute.service.export.writers;

import org.janelia.it.jacs.model.common.SortArgument;
import org.janelia.it.jacs.shared.export.ExportWriterConstants;

import java.io.IOException;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Jul 16, 2008
 * Time: 1:30:41 PM
 */
public class ExportWriterFactory {

    public static ExportWriter getWriterForExportType(String fullPathFilename, String exportType, List<SortArgument> dataHeaders)
            throws IOException {
        if (ExportWriterConstants.EXPORT_TYPE_CSV.equalsIgnoreCase(exportType)) {
//            _logger.debug("Returning ExportCVSWriter for type "+exportType);
            return new ExportCsvWriter(fullPathFilename, dataHeaders);
        }
        else if (ExportWriterConstants.EXPORT_TYPE_HTML.equalsIgnoreCase(exportType)) {
//            _logger.debug("Returning ExportHtmlWriter for type "+exportType);
            return new ExportHtmlWriter(fullPathFilename, dataHeaders);
        }
        else if (ExportWriterConstants.EXPORT_TYPE_NCBI_BLAST_XML.equalsIgnoreCase(exportType)) {
//            _logger.debug("Returning ExportXmlWriter for type "+exportType);
            return new ExportXmlWriter(fullPathFilename, dataHeaders);
        }
        else if (ExportWriterConstants.EXPORT_TYPE_FASTA.equalsIgnoreCase(exportType)) {
//            _logger.debug("Returning ExportFastaWriter for type "+exportType);
            return new ExportFastaWriter(fullPathFilename, dataHeaders);
        }
        // EXPORT_TYPE_CURRENT assumes that we are exporting a pre-existing file, ie FastaFileNode
        else if (ExportWriterConstants.EXPORT_TYPE_CURRENT.equalsIgnoreCase(exportType)) {
            return null;
        }
        else if (ExportWriterConstants.EXPORT_TYPE_EXCEL.equalsIgnoreCase(exportType)) {
            return new ExportExcelWriter(fullPathFilename, dataHeaders);
        }
        else {
//            _logger.error("Found no ExportWriter for type "+exportType);
            return null;
        }
    }

}
