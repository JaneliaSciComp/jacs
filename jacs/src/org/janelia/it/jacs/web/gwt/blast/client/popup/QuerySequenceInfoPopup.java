
package org.janelia.it.jacs.web.gwt.blast.client.popup;

import com.google.gwt.user.client.ui.FlexTable;
import org.janelia.it.jacs.model.common.UserDataNodeVO;
import org.janelia.it.jacs.web.gwt.common.client.Constants;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.comparables.FormattedDateTime;
import org.janelia.it.jacs.web.gwt.common.client.util.BlastData;
import org.janelia.it.jacs.web.gwt.common.client.util.NumberUtils;
import org.janelia.it.jacs.web.gwt.common.client.util.StringUtils;

/**
 * @author Michael Press
 */
public class QuerySequenceInfoPopup extends org.janelia.it.jacs.web.gwt.common.client.popup.BaseInfoPopup {
    private BlastData _blastData;

    public QuerySequenceInfoPopup(BlastData blastData) {
        super(/*title*/ null, /*realize now*/ false);
        _blastData = blastData;
    }

    protected void populateContent() {
        if (_blastData == null)
            return;

        int row = 0;
        FlexTable grid = new FlexTable();
        grid.setStyleName("BlastInfoPopup");
        grid.setCellPadding(0);
        grid.setCellSpacing(0);

        addRow(row, grid, "Query Name", _blastData.getMostRecentlySpecifiedQuerySequenceName());

        // Previous sequence
        if (_blastData.getQuerySequenceDataNodeMap() != null && _blastData.getQuerySequenceDataNodeMap().size() > 0) {
            UserDataNodeVO queryNode = _blastData.getQuerySequenceDataNodeMap().values().iterator().next();

            //addRow(++row, grid, "Description", queryNode.getDescription());
            if (queryNode.getDateCreated() != null)
                addRow(++row, grid, "Created", new FormattedDateTime(queryNode.getDateCreated().getTime()).toString());
            addRow(++row, grid, "File Size", NumberUtils.formatInteger(queryNode.getLength()) + " B");
            //addRow(++row, grid, "Sequences", queryNode.getLength());
        }

        // Pasted or uploaded sequence
        else if (StringUtils.hasValue(_blastData.getUserReferenceFASTA())) {
            // Uploaded sequence
            if (_blastData.getUserReferenceFASTA().equals(Constants.UPLOADED_FILE_NODE_KEY)) {
                //TODO: have uploader return the file size
                addRow(++row, grid, "User Action", "Uploaded File"); //TODO: can get filename?
            }
            // pasted sequence(s)
            else {
                addRow(++row, grid, "User Action", "Typed Sequence");
                addRow(++row, grid, "Sequence", getDisplaySequence());
                addRow(++row, grid, "Size", NumberUtils.formatInteger(_blastData.getUserReferenceFASTA().length()));
            }
        }

        addRow(++row, grid, "Type", _blastData.getMostRecentlySelectedQuerySequenceType());

        //TODO: display sequence only if upload? Show only defline?
        //addRow(++row, grid, "Sequence", _blastData.getUserReferenceFASTA());

        add(grid);
    }

    private String getDisplaySequence() {
        if (_blastData.getUserReferenceFASTA().length() < 100)
            return _blastData.getUserReferenceFASTA();
        else
            return _blastData.getUserReferenceFASTA() + "...";
    }
}