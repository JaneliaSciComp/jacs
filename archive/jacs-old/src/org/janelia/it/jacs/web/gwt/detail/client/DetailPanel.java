
package org.janelia.it.jacs.web.gwt.detail.client;

import com.google.gwt.user.client.History;
import com.google.gwt.user.client.HistoryListener;
import com.google.gwt.user.client.ui.VerticalPanel;
import org.janelia.it.jacs.model.genomics.AccessionIdentifierUtil;
import org.janelia.it.jacs.web.gwt.common.client.BaseEntryPoint;
import org.janelia.it.jacs.web.gwt.common.client.SystemWebTracker;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.ActionLink;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;
import org.janelia.it.jacs.web.gwt.common.client.util.URLUtils;

/**
 * This class serves as a wrapper around the different Detail sub panels and should be used
 * by client (e.g. Status DetailsWizardPage) to build detail panel / sub panel
 *
 * @author Tareq Nabeel
 */
public class DetailPanel extends VerticalPanel {
    public static final String ACC_PARAM = "acc";
    private static Logger logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.detail.client.DetailPanel");

    private String currentEntityAccession = null;
    private DetailSubPanelBuilderFactory subPanelBuilderFactory;
    private ActionLink previousBackLink;
    private ActionLink currentBackLink;
    private DetailPanelHistoryHandler _panelHistoryHandler;
    private String currentHistoryToken;
    private BaseEntryPoint _parentEntryPoint;

    private class DetailPanelHistoryHandler implements HistoryListener {

        DetailPanelHistoryHandler() {
        }

        public void onHistoryChanged(String historyToken) {
            if (historyToken != null && !historyToken.equals(currentHistoryToken)) {
                String accessionNo = URLUtils.getParameterValue(historyToken, ACC_PARAM);
                if (accessionNo != null && !accessionNo.equals(currentEntityAccession)) {
                    if ((previousBackLink == null || !historyToken.equals(previousBackLink.getTargetHistoryToken())) &&
                            (currentBackLink == null || !historyToken.equals(currentBackLink.getTargetHistoryToken()))) {
                        // this is typically the forward button case so we use the currentbacklink
                        // however if the user clicks on forward more than once the back link
                        // is no longer right
                        rebuildPanel(accessionNo, null, currentBackLink);
                    }
                    else {
                        rebuildPanel(accessionNo, null, previousBackLink);
                    }
                }
            }
            currentHistoryToken = historyToken;
        }

    }

    public DetailPanel(BaseEntryPoint parentEntryPoint) {
        super();
        _parentEntryPoint = parentEntryPoint;
        subPanelBuilderFactory = new DetailSubPanelBuilderFactory();
        _panelHistoryHandler = new DetailPanelHistoryHandler();
    }


    public void setCurrentBackLink(ActionLink currentBackLink) {
        this.currentBackLink = currentBackLink;
    }

    public void setPreviousBackLink(ActionLink previousBackLink) {
        this.previousBackLink = previousBackLink;
    }

    /**
     * Builds the Detail panel given a Camera Accession
     *
     * @param acc        Camera Accession
     * @param currentURL
     */
    public void rebuildPanel(String acc, String currentURL) {
        rebuildPanel(acc, currentURL, null);
    }

    /**
     * Builds the Detail panel given a Camera Accession and ActionLink
     *
     * @param acc      Camera Accession
     * @param backLink ActionLink to add to Detail Panel e.g. for taking user back to previous page
     * @parem pageToken current page history token
     */
    public void rebuildPanel(String acc, String pageToken, ActionLink backLink) {
        logger.debug("DetailPanel rebuildPanel...");
        if (acc != null) {
            _parentEntryPoint.setBookmarkId(acc);
        }
        try {
            currentEntityAccession = acc;
            // track the detail panel display
            SystemWebTracker.trackActivity("ViewSequenceDetails", new String[]{acc});
            logger.debug("DetailPanel rebuildPanel called with acc=" + acc);
            super.clear();
            logger.debug("DetailPanel getDetailPanelBuilder...");
            DetailPanelBuilder detailPanelBuilder = subPanelBuilderFactory.getDetailPanelBuilder(this, acc, backLink);
            if (detailPanelBuilder == null) {
                String message = "DetailPanel detailPanelBuilder is null";
                logger.error(message);
                throw new RuntimeException(message);
            }
            else {
                logger.debug("DetailPanel detailPanelBuilder is not null");
            }
            logger.debug("DetailPanel getSubPanel...");
            DetailSubPanel subPanel = detailPanelBuilder.getSubPanel();
            logger.debug("DetailPanel add subPanel...");
            add(subPanel);
            logger.debug("DetailPanel build...");
            detailPanelBuilder.build();
            // update the history
            logger.debug("DetailPanel updateHistory...");
            if (pageToken != null) {
                currentHistoryToken = pageToken;
                History.newItem(pageToken);
            }
        }
        catch (InvalidAccessionException e) {
            logger.error(e);
            add(HtmlUtils.getHtml(getInvalidAccessionHTMLMessage(e), "detailPanelError"));
        }
        catch (RuntimeException e) {
            logger.error("DetailPanel rebuildPanel caught exception ", e);
            throw e;
        }
    }

    public static String getInvalidAccessionHTMLMessage(Exception e) {
        return e.getMessage() + ".<br/><br/>Supported accessions begin with " + AccessionIdentifierUtil.getSupportedAccessions();
    }

    protected void onAttach() {
        super.onAttach();
        // when the detail panel becomes visible attach the history handler
        History.addHistoryListener(_panelHistoryHandler);
    }

    protected void onDetach() {
        super.onDetach();
        // when the detail panel is no longer visible detach the history handler as well
        History.removeHistoryListener(_panelHistoryHandler);
    }

}
