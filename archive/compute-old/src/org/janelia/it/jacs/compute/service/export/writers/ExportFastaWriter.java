
package org.janelia.it.jacs.compute.service.export.writers;

import org.janelia.it.jacs.model.common.SortArgument;
import org.janelia.it.jacs.shared.export.ExportWriterConstants;
import org.janelia.it.jacs.shared.node.FastaUtil;

import java.io.IOException;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Jun 6, 2008
 * Time: 3:37:32 PM
 */
public class ExportFastaWriter extends ExportWriter {
    public static final int FASTA_SEQ_WIDTH = 80;

    public ExportFastaWriter() {
    }

    public ExportFastaWriter(String fullPathFilename, List<SortArgument> headerItems) {
        super(fullPathFilename, headerItems);
    }

    public String getFormatType() {
        return ExportWriterConstants.EXPORT_TYPE_FASTA;
    }

    /**
     * This method is intended to start off the file type with any characters necessary. ie <xml ... for XmlWriters.
     */
    public void start() throws IOException {
        // FASTA has no header line
        writer.write("");
    }

    public void writeItem(List<String> itemStrings) throws IOException {
        String defline = itemStrings.get(0);
        String sequence = itemStrings.get(1);
        writer.write(FastaUtil.formatFasta(defline, sequence, FASTA_SEQ_WIDTH));
    }

    /**
     * This method is intended to close the file type with any characters necessary. ie </xml> for XmlWriters.
     */
    protected void endFormatting() throws IOException {
        // FASTA has no closure line
        writer.write("");
    }
}