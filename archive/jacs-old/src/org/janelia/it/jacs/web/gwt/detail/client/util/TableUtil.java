
package org.janelia.it.jacs.web.gwt.detail.client.util;

import com.google.gwt.user.client.ui.HTMLTable;
import com.google.gwt.user.client.ui.Widget;
import org.janelia.it.jacs.web.gwt.common.client.ui.RowIndex;
import org.janelia.it.jacs.web.gwt.common.client.util.TableUtils;

import java.util.Date;

/**
 * Contains utility methods needed for table row additions in Detail pages
 *
 * @author Tareq Nabeel
 */
public class TableUtil {

    /**
     * This method is used to add a data item of type String to a table
     *
     * @param table     HTMLTable instance to add item to
     * @param rowIndex  RowIndex instance to use and increment
     * @param prompt    the label for the item
     * @param itemValue the HTML string
     */
    public static void addHTMLRow(HTMLTable table, RowIndex rowIndex, String prompt, String itemValue) {
        if (itemValue == null || itemValue.trim().equals("")) {
            return;
        }
        TableUtils.addHTMLRow(table, rowIndex, prompt, itemValue);
        setDetailLabelCellStyle(table, rowIndex.getCurrentRow());
        rowIndex.increment();
    }

    /**
     * This method is used to add a data item of type String to a table
     *
     * @param table     HTMLTable instance to add item to
     * @param rowIndex  RowIndex instance to use and increment
     * @param prompt    the label for the item
     * @param itemValue the item value
     */
    public static void addTextRow(HTMLTable table, RowIndex rowIndex, String prompt, String itemValue) {
        if (itemValue == null || itemValue.trim().equals("")) {
            return;
        }
        TableUtils.addTextRow(table, rowIndex, prompt, itemValue);
        setDetailLabelCellStyle(table, rowIndex.getCurrentRow());
        rowIndex.increment();
    }

    /**
     * This method is used to set the style of label cells in a table
     *
     * @param table    HTMLTable instance to set the style of label cell
     * @param rowIndex RowIndex instance to use
     */
    public static void setDetailLabelCellStyle(HTMLTable table, int rowIndex) {
        TableUtils.setLabelCellStyle(table, rowIndex);
        TableUtils.addCellStyle(table, rowIndex, 0, "detailLabelCell");
    }

    /**
     * Adds a widget to a table
     *
     * @param table    the table to add the item to
     * @param rowIndex the row index at which to add item to
     * @param prompt   the label of the data item
     * @param widget   the widget to add
     */
    public static void addWidgetRow(HTMLTable table, RowIndex rowIndex, String prompt, Widget widget) {
        if (widget == null) {
            return;
        }
        TableUtils.addWidgetRow(table, rowIndex, prompt, widget);
        TableUtils.setLabelCellStyle(table, rowIndex.getCurrentRow());
        rowIndex.increment();
    }


    /**
     * This method is used to add a data item of type Date to the table
     *
     * @param table     the table to add the item to
     * @param rowIndex  the row index at which to add item to
     * @param prompt    the label of the data item
     * @param itemValue the value of the item
     */
    public static void addDateRow(HTMLTable table, RowIndex rowIndex, String prompt, Date itemValue) {
        if (itemValue == null) {
            return;
        }
        // Data Time
        TableUtils.addDateRow(table, rowIndex, prompt, itemValue);
        TableUtils.setLabelCellStyle(table, rowIndex.getCurrentRow());
        rowIndex.increment();
    }
}
