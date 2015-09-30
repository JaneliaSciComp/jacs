
package org.janelia.it.jacs.compute.service.export.writers;

import org.janelia.it.jacs.model.common.SortArgument;
import org.janelia.it.jacs.shared.export.ExportWriterConstants;

import java.io.IOException;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Jun 6, 2008
 * Time: 3:39:22 PM
 */
public class ExportXmlWriter extends ExportWriter {

    public ExportXmlWriter() {
    }

    public ExportXmlWriter(String fullPathFilename, List<SortArgument> headerItems) {
        super(fullPathFilename, headerItems);
    }

    public String getFormatType() {
        return ExportWriterConstants.EXPORT_TYPE_NCBI_BLAST_XML;
    }

    /**
     * This method is intended to start off the file type with any characters necessary. ie <xml ... for XmlWriters.
     */
    public void start() throws IOException {
        writer.write("<xml>\n");
    }

    public void writeItem(List<String> itemStrings) throws IOException {
        StringBuffer itemText = new StringBuffer();
//        itemText.append("<").append(elementName);
//        for (String s : attributes.keySet()) {
//            itemText.append(" ").append(s).append("=\"").append(attributes.get(s)).append("\"");
//        }
        itemText.append(" />\n");
        writer.write(itemText.toString());
    }

    /**
     * This method is intended to close the file type with any characters necessary. ie </xml> for XmlWriters.
     */
    protected void endFormatting() throws IOException {
        writer.write("</xml>\n");
    }
}
