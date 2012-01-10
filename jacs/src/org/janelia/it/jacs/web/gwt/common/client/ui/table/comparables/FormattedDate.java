
package org.janelia.it.jacs.web.gwt.common.client.ui.table.comparables;

import org.gwtwidgets.client.util.SimpleDateFormat;

import java.util.Date;

/**
 * Extends Date to output the date in a format we like.  Date is already a Comparable, so the date will
 * sort properly regardless of the output format (the sorting is done on the underlying long).
 *
 * @author Michael Press
 */
public class FormattedDate extends Date {
    /**
     * No-arg constructor required for GWT
     */
    public FormattedDate() {
        this(new Date().getTime());
    }

    public FormattedDate(long date) {
        super(date);
    }

    public String toString() {
        return (new SimpleDateFormat(getFormat()).format(this)).toLowerCase();
    }

    protected String getFormat() {
        return "MM/dd/yy";
    }
}
