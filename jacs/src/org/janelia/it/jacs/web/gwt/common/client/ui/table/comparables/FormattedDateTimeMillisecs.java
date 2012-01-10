
package org.janelia.it.jacs.web.gwt.common.client.ui.table.comparables;

import org.gwtwidgets.client.util.SimpleDateFormat;

/**
 * @author Michael Press
 */
public class FormattedDateTimeMillisecs extends FormattedDate {
    /**
     * No-arg constructor required for GWT
     */
    public FormattedDateTimeMillisecs() {
        super();
    }

    public FormattedDateTimeMillisecs(long time) {
        super(time);
    }

    public String getFormat() {
        //TODO: MS not supported !!!
        return new SimpleDateFormat("MM/dd/yy hh:mm:ss.SSS").format(this);
    }
}