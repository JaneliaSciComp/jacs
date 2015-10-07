
package org.janelia.it.jacs.compute.service.export.writers;

import org.janelia.it.jacs.compute.service.export.util.CSVDataConversionHelper;
import org.janelia.it.jacs.model.common.SortArgument;
import org.janelia.it.jacs.shared.export.ExportWriterConstants;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Jun 6, 2008
 * Time: 3:37:32 PM
 */
public class ExportCsvWriter extends ExportWriter {

    public ExportCsvWriter() {
    }

    public ExportCsvWriter(String fullPathFilename, List<SortArgument> headerItems) {
        super(fullPathFilename, headerItems);
    }

    public String getFormatType() {
        return ExportWriterConstants.EXPORT_TYPE_CSV;
    }

    /**
     * This method is intended to start off the file type with any characters necessary. ie <xml ... for XmlWriters.
     */
    public void start() throws IOException {
        if (headerItems != null && headerItems.size() > 0) {
            ArrayList<String> tmpItems = new ArrayList<String>();
            for (SortArgument headerItem : headerItems) {
                tmpItems.add(headerItem.getSortArgumentName());
            }
            writeLine(tmpItems);
        }
    }

    public void writeItem(List<String> itemStrings) throws IOException {
        writeLine(itemStrings);
    }

    /**
     * This method loops through header and data rows and formats them.
     *
     * @param stringItems items to separate by commas
     */
    private void writeLine(List<String> stringItems) throws IOException {
        StringBuffer sbuf = new StringBuffer();
        for (Iterator<String> stringIterator = stringItems.iterator(); stringIterator.hasNext();) {
            String s = stringIterator.next();
            s = CSVDataConversionHelper.escapeSpecialExcelChars(s);
            sbuf.append(s);
            if (stringIterator.hasNext()) {
                sbuf.append(",");
            }
            else {
                sbuf.append("\n");
            }
        }
        writer.write(sbuf.toString());
    }

    /**
     * This method is intended to close the file type with any characters necessary. ie </xml> for XmlWriters.
     */
    protected void endFormatting() throws IOException {
        writer.write("");
    }
}
