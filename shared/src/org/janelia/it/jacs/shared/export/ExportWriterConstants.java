
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
