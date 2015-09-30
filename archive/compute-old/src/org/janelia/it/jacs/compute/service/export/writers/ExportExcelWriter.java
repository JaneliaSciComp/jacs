
package org.janelia.it.jacs.compute.service.export.writers;

import jxl.Workbook;
import jxl.write.*;
import jxl.write.Number;
import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.service.export.util.CSVDataConversionHelper;
import org.janelia.it.jacs.model.common.SortArgument;
import org.janelia.it.jacs.shared.export.ExportWriterConstants;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Jul 16, 2008
 * Time: 2:03:34 PM
 */
public class ExportExcelWriter extends ExportWriter {
    Logger _logger = Logger.getLogger(ExportExcelWriter.class);
    public static int MAX_ROWS = 64000;
    WritableWorkbook workbook;
    WritableSheet sheet;
    Pattern integerPattern = Pattern.compile("\\-?\\d+");
    Pattern floatPattern = Pattern.compile("[\\dEe\\-\\.]+");
    WritableCellFormat integerFormat = new WritableCellFormat(NumberFormats.INTEGER);
    WritableCellFormat floatFormat = new WritableCellFormat(NumberFormats.FLOAT);
    // Data starts at the value below. index 0 = the header row
    int rowPosition = 1;

    public ExportExcelWriter(String fullPathFilename, List<SortArgument> headerItems) throws IOException {
        this.headerItems = headerItems;
        this.fullPathFilename = fullPathFilename;
        workbook = Workbook.createWorkbook(new File(fullPathFilename));
        sheet = workbook.createSheet("Data", 0);
    }

    public String getFormatType() {
        return ExportWriterConstants.EXPORT_TYPE_EXCEL;
    }

    /**
     * This method is intended to start off the file type with any characters necessary. ie <xml ... for XmlWriters.
     */
    public void start() throws IOException {
        if (headerItems != null && headerItems.size() > 0) {
            int columnPosition = 0;
            for (SortArgument sa : headerItems) {
                Label label = new Label(columnPosition, 0, sa.getSortArgumentName());
                try {
                    sheet.addCell(label);
                    columnPosition++;
                }
                catch (Exception e) {
                    throw new IOException(e.getMessage());
                }
            }
        }
    }

    public void writeItem(List<String> itemStrings) throws IOException {
        if (rowPosition >= MAX_ROWS)
            throw new IOException("Exceed max rows=" + MAX_ROWS);
        int columnPosition = 0;
        for (String item : itemStrings) {
            try {
                item = CSVDataConversionHelper.escapeSpecialExcelChars(item);
                boolean addedCell = false;
                Matcher integerMatcher = integerPattern.matcher(item);
                Matcher floatMatcher = floatPattern.matcher(item);
                if (integerMatcher.matches()) {
                    try {
                        Long longTest = new Long(item);
                        sheet.addCell(new Number(columnPosition, rowPosition, longTest, integerFormat));
                        addedCell = true;
                    }
                    catch (Exception e) {
                        // Do nothing
                    }
                }
                else if (floatMatcher.matches()) {
                    try {
                        Float floatTest = new Float(item);
                        sheet.addCell(new Number(columnPosition, rowPosition, floatTest, floatFormat));
                        addedCell = true;
                    }
                    catch (Exception e) {
                        // Do nothing
                    }
                }
                if (!addedCell) {
                    Label label = new Label(columnPosition, rowPosition, item);
                    sheet.addCell(label);
                }
            }
            catch (Exception e) {
                // Do nothing
            }
            columnPosition++;
        }
        rowPosition++;
    }

    /**
     * This method is intended to close the file type with any characters necessary. ie </xml> for XmlWriters.
     */
    protected void endFormatting() throws IOException {
        workbook.write();
        try {
            workbook.close();
        }
        catch (Exception e) {
            throw new IOException(e.getMessage());
        }
    }
}
