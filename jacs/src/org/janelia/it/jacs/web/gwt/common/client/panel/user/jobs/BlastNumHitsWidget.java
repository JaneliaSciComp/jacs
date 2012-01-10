
package org.janelia.it.jacs.web.gwt.common.client.panel.user.jobs;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Widget;
import org.janelia.it.jacs.shared.tasks.BlastJobInfo;
import org.janelia.it.jacs.web.gwt.common.client.ui.imagebundles.ImageBundleFactory;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.Link;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.comparables.IntegerString;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.comparables.PopperUpperHTML;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;
import org.janelia.it.jacs.web.gwt.common.client.util.SystemProps;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Jul 24, 2008
 * Time: 10:47:55 AM
 */
public class BlastNumHitsWidget {
    //    public static final int NHITS_HIGHLIMIT = 50000;
    public static final int NHITS_HIGHLIMIT = SystemProps.getInt("BlastServer.HitThresholdForFileNode", 50000);

    public static Comparable getNumHitsField(BlastJobInfo job) {
        Long numHits = job.getNumHits();
        if (numHits == null) {
            return new IntegerString(-1, "--");// no results sorts numerically as -1
        }
        else {
            return new IntegerString(numHits, job.getNumHitsFormatted());
        }
    }

    public static Widget createNumHitsWidget(BlastJobInfo job, ClickListener clickListener) {
        Long numHits = job.getNumHits();
        String content = getNumHitsField(job).toString();
        if (numHits != null) {
            if (numHits == 0 || !BlastJobResultsPanel.SHOW_BLAST_RESULTS_PAGE) {
                return HtmlUtils.getHtml(content, "text");
            }
            else if (numHits < NHITS_HIGHLIMIT) {
                return new Link(content, clickListener);
                /**new ClickListener(){
                 public void onClick(Widget w) {
                 }}**/
            }
            else {
                // numHits >= NHITS_HIGHLIMIT
                final String largeBlastPopupNote =
                        "This BLAST job exceeds the maximum of " +
                                NHITS_HIGHLIMIT +
                                " alignments.  The results are available for export only";
                Grid grid = new Grid(1, 2);
                grid.setWidget(0, 0, ImageBundleFactory.getControlImageBundle().getInfoImage().createImage());
                HTML numHitsField = HtmlUtils.getHtml(content, "text");
                grid.setWidget(0, 1, numHitsField);
                HTML largeBlastInfo = HtmlUtils.getHtml(largeBlastPopupNote, "largeBlastJobPopperUpperNoteText");
                return new PopperUpperHTML(grid.toString(),
                        "largeBlastJobNumHitsText",
                        "largeBlastJobNumHitsHoverText",
                        largeBlastInfo);
            }
        }
        else {
            return HtmlUtils.getHtml(content, "text");
        }
    }

}
