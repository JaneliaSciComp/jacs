
package org.janelia.it.jacs.compute.service.export.writers;

import org.janelia.it.jacs.model.common.SortArgument;
import org.janelia.it.jacs.shared.export.ExportWriterConstants;

import java.io.IOException;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Jun 6, 2008
 * Time: 3:38:40 PM
 */
public class ExportHtmlWriter extends ExportWriter {

    public ExportHtmlWriter() {
    }

    public ExportHtmlWriter(String fullPathFilename, List<SortArgument> headerItems) {
        super(fullPathFilename, headerItems);
    }

    public String getFormatType() {
        return ExportWriterConstants.EXPORT_TYPE_HTML;
    }

    /**
     * This method is intended to start off the file type with any characters necessary. ie <xml ... for XmlWriters.
     */
    public void start() throws IOException {
        writer.write("");
    }

    public void writeItem(List<String> itemStrings) throws IOException {
        writer.write("");
    }

    /**
     * This method is intended to close the file type with any characters necessary. ie </xml> for XmlWriters.
     */
    protected void endFormatting() throws IOException {
        writer.write("");
    }
}
