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

package org.janelia.it.jacs.shared.export;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.io.Serializable;
import java.util.HashMap;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Jun 6, 2008
 * Time: 3:32:01 PM
 */
public class ExportWriterConstants implements IsSerializable, Serializable {
//    public static Logger _logger = Logger.getLogger(ExportWriterFactory.class);

    // EXPORT_TYPE_CURRENT assumes that we are exporting a pre-existing file, ie FastaFileNode
    public transient static final String EXPORT_TYPE_CURRENT = "current";
    public transient static final String EXPORT_TYPE_CSV = "csv";
    public transient static final String EXPORT_TYPE_NCBI_BLAST_XML = "blastXml";
    public transient static final String EXPORT_TYPE_HTML = "html";
    public transient static final String EXPORT_TYPE_FASTA = "fasta";
    public transient static final String EXPORT_TYPE_TEXT = "txt";
    public transient static final String EXPORT_TYPE_EXCEL = "xls";

    public static final String COMPRESSION_GZ = ".gz";
    public static final String COMPRESSION_ZIP = ".zip";
    public static final String COMPRESSION_NONE = "None";
    private static HashMap<String, String> exportTypeToMimeMap = new HashMap<String, String>();

    static {
        exportTypeToMimeMap.put(EXPORT_TYPE_CURRENT, "");
        exportTypeToMimeMap.put(EXPORT_TYPE_CSV, "csv");
        exportTypeToMimeMap.put(EXPORT_TYPE_FASTA, "fasta");
        exportTypeToMimeMap.put(EXPORT_TYPE_HTML, "html");
        exportTypeToMimeMap.put(EXPORT_TYPE_NCBI_BLAST_XML, "xml");
        exportTypeToMimeMap.put(EXPORT_TYPE_TEXT, "txt");
        exportTypeToMimeMap.put(EXPORT_TYPE_EXCEL, "xls");
    }

    public ExportWriterConstants() {
    }

    public static String getMimeTypeForExportType(String targetExportType) {
        if (null == targetExportType) return "";
        return exportTypeToMimeMap.get(targetExportType);
    }
}
