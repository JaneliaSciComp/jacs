
package org.janelia.it.jacs.web.gwt.common.client.ui.table.comparables;

/**
 * Adds time (HH:MM and optional :SS) to FormattedDate.
 *
 * @author Michael Press
 */
public class FormattedDateTime extends FormattedDate {
    private boolean _includeSecs = false;

    /**
     * No-arg constructor required for GWT
     */
    public FormattedDateTime() {
        super();
    }

    public FormattedDateTime(long date) {
        this(date, false);
    }

    public FormattedDateTime(long date, boolean includeSecs) {
        super(date);
        _includeSecs = includeSecs;
    }

    protected String getFormat() {
        return super.getFormat() + ((_includeSecs) ? " hh:mm a" : "");
    }
}