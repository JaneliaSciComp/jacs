
package org.janelia.it.jacs.compute.service.export.writers;

import org.janelia.it.jacs.model.common.SortArgument;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Jun 6, 2008
 * Time: 3:31:42 PM
 */
public abstract class ExportWriter {

    protected List<SortArgument> headerItems = new ArrayList<SortArgument>();
    protected String fullPathFilename;
    protected Writer writer;

    protected ExportWriter() {
    }

    protected ExportWriter(String fullPathFilename, List<SortArgument> headerItems) {
        this.headerItems = headerItems;
        this.fullPathFilename = fullPathFilename;
        try {
            this.writer = new FileWriter(fullPathFilename);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public abstract String getFormatType();

    /**
     * This method is intended to start off the file type with any characters necessary. ie <xml ... for XmlWriters.
     *
     * @throws java.io.IOException error when starting to write to the file
     */
    public abstract void start() throws IOException;


    /**
     * This method takes the current ExportItem and invokes the method for the byte[] representation of that item
     *
     * @param exportItems items which contain the content
     * @throws Exception error matching up the headers to the items to be exported
     */
    public void writeItems(List<List<String>> exportItems) throws Exception {
        for (List<String> exportItem : exportItems) {
            // Verify the number of items in the string matches the header size
            if (headerItems.size() != exportItem.size()) {
                throw new Exception("The export header has " + headerItems.size() + " items yet data passed only has " + exportItem.size());
            }
            this.writeItem(exportItem);
        }
    }

    public abstract void writeItem(List<String> itemStrings) throws IOException;

    /**
     * This method is intended to close the file type with any characters necessary. ie </xml> for XmlWriters.
     *
     * @throws java.io.IOException error trying to close up the file
     */
    public void end() throws IOException {
        endFormatting();
        if (null != writer) {
            writer.flush();
            writer.close();
        }
    }

    protected abstract void endFormatting() throws IOException;

}
