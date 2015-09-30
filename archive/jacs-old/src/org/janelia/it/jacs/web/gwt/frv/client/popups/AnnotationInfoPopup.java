
package org.janelia.it.jacs.web.gwt.frv.client.popups;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.janelia.it.jacs.web.gwt.common.client.popup.BasePopupPanel;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.common.client.ui.ButtonSet;
import org.janelia.it.jacs.web.gwt.common.client.ui.RoundedButton;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.ExternalLink;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;
import org.janelia.it.jacs.web.gwt.frv.client.RecruitmentService;
import org.janelia.it.jacs.web.gwt.frv.client.RecruitmentServiceAsync;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Oct 4, 2007
 * Time: 4:39:10 PM
 */
public class AnnotationInfoPopup extends BasePopupPanel {

    private static Logger _logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.frv.client.popups.AnnotationInfoPopup");

    private String recruitableNodeId;
    private long ntLocation;
    private String giNumber;

    private VerticalPanel panel = new VerticalPanel();
    private Label loading = new Label();
    private String annotationFilter;

    protected static RecruitmentServiceAsync _recruitmentService = (RecruitmentServiceAsync) GWT.create(RecruitmentService.class);

    static {
        ((ServiceDefTarget) _recruitmentService).setServiceEntryPoint("recruitment.srv");
    }


    public AnnotationInfoPopup(String giNumberOfSourceData, String recruitableNodeId, long ntLocation, String annotationFilter) {
        super("Annotation Details", /*realizeNow*/ false, /*autohide*/ true, /*modal*/ true);
        this.giNumber = giNumberOfSourceData;
        this.recruitableNodeId = recruitableNodeId;
        this.ntLocation = ntLocation;
        this.annotationFilter = annotationFilter;
        loading.setText("Loading annotation information for location " + ntLocation + "...");
    }

    protected void populateContent() {
        this.add(panel);
//        panel.setSize("400px", "100px");
        panel.setVerticalAlignment(VerticalPanel.ALIGN_TOP);
        panel.add(loading);
        panel.add(new ExternalLink("NCBI source data for parent: gi|" + giNumber, "http://www.ncbi.nlm.nih.gov/entrez/viewer.fcgi?db=nuccore&qty=1&c_start=1&list_uids=" + giNumber + "&uids=&dopt=gb&dispmax=5&sendto=t&fmt_mask=0&from=begin&to=end&extrafeatpresent=1&ef_CDD=8&ef_MGC=16&ef_HPRD=32&ef_STS=64&ef_tRNA=128&ef_microRNA=256&ef_Exon=512"));
        // Try to figure out which feature track was hit
        _recruitmentService.getAnnotationInfoForSelection(recruitableNodeId, ntLocation, annotationFilter, new AsyncCallback() {
            public void onFailure(Throwable throwable) {
                _logger.error("Failed to retrieve RecruitmentViewer.getAnnotationInfoForSelection");
            }

            public void onSuccess(Object o) {
                //Window.alert(o.toString());
                _logger.info("Got annotation entries for user");
//                    panel.add(new ExternalLink("NCBI annotation source data...", "http://www.ncbi.nlm.nih.gov/entrez/viewer.fcgi?db=nucleotide&qty=1&c_start=1&list_uids=78778385&uids=&dopt=gbwithparts&dispmax=5&sendto=t&fmt_mask=0&from=565835&to=566743&extrafeatpresent=1&ef_CDD=8&ef_MGC=16&ef_HPRD=32&ef_STS=64&ef_tRNA=128&ef_microRNA=256&ef_Exon=512"));
                loading.setText("Annotation information for location " + ntLocation);
                String annotationInfo = "<br/>Not available.";
                if (null != o) {
                    annotationInfo = o.toString().replaceAll("\n", "<br/>").replaceAll("\t", "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;").replaceAll(" ", "&nbsp;");
                }
                panel.add(HtmlUtils.getHtml(annotationInfo, "smallText"));
            }
        });
    }

    protected ButtonSet createButtons() {
        RoundedButton closeButton = new RoundedButton("Close", new ClickListener() {
            public void onClick(Widget widget) {
                hidePopup();
            }
        });

        return new ButtonSet(new RoundedButton[]{closeButton});
    }

    /**
     * Hook for inner classes to hide the popup
     */
    protected void hidePopup() {
        this.hide();
    }

}
