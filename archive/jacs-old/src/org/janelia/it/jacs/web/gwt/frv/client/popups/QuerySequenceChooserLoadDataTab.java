
package org.janelia.it.jacs.web.gwt.frv.client.popups;

import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.janelia.it.jacs.model.genomics.SequenceType;
import org.janelia.it.jacs.web.gwt.common.client.jobs.JobSelectionListener;
import org.janelia.it.jacs.web.gwt.common.client.panel.user.UploadUserSequencePanel;
import org.janelia.it.jacs.web.gwt.common.client.ui.SelectionListener;
import org.janelia.it.jacs.web.gwt.common.client.util.BlastData;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Mar 18, 2008
 * Time: 11:20:29 AM
 */
public class QuerySequenceChooserLoadDataTab implements QuerySequenceChooserTab {

    private static final String TAB_LABEL = "Blast and Recruit My Sequence";
    private UploadUserSequencePanel uploadPanel;

    public QuerySequenceChooserLoadDataTab(SelectionListener selectionListener) {
        uploadPanel = new UploadUserSequencePanel(selectionListener, SequenceType.NUCLEOTIDE, 10);
    }

    public Widget getPanel() {
        VerticalPanel contentPanel = new VerticalPanel();

        contentPanel.add(HtmlUtils.getHtml("&nbsp;", "smallSpacer"));
        contentPanel.add(uploadPanel);
        contentPanel.add(HtmlUtils.getHtml("&nbsp;", "smallSpacer"));

        return contentPanel;
    }

    public String getTabLabel() {
        return TAB_LABEL;
    }

    public void setRecruitableJobSelectionListener(JobSelectionListener listener) {
        // Not valid in this context
        //Window.alert("Calling setRecruitableJobSelectionListener");
    }

    public void realize() {
        // Nothing to do
    }

    public BlastData getBlastData() {
        return uploadPanel.getBlastData();
    }

    public boolean validateAndPersistSequenceSelection() {
        return uploadPanel.validateAndPersistSequenceSelection();
    }
}
