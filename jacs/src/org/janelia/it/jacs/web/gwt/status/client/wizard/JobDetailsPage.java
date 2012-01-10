
package org.janelia.it.jacs.web.gwt.status.client.wizard;

import com.google.gwt.core.client.GWT;
import com.google.gwt.maps.client.InfoWindowContent;
import com.google.gwt.maps.client.MapType;
import com.google.gwt.maps.client.event.MarkerClickHandler;
import com.google.gwt.maps.client.geom.LatLng;
import com.google.gwt.maps.client.overlay.Marker;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.ui.*;
import org.janelia.it.jacs.shared.tasks.BlastJobInfo;
import org.janelia.it.jacs.web.gwt.common.client.Constants;
import org.janelia.it.jacs.web.gwt.common.client.model.genomics.BlastHit;
import org.janelia.it.jacs.web.gwt.common.client.model.metadata.Site;
import org.janelia.it.jacs.web.gwt.common.client.panel.TitledBoxActionLinkUtils;
import org.janelia.it.jacs.web.gwt.common.client.service.DataService;
import org.janelia.it.jacs.web.gwt.common.client.service.DataServiceAsync;
import org.janelia.it.jacs.web.gwt.common.client.service.StatusService;
import org.janelia.it.jacs.web.gwt.common.client.service.StatusServiceAsync;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.common.client.ui.RoundedButton;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.BackActionLink;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.HelpActionLink;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.comparables.PopperUpperHTML;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;
import org.janelia.it.jacs.web.gwt.common.client.wizard.WizardController;
import org.janelia.it.jacs.web.gwt.common.shared.data.EntityListener;
import org.janelia.it.jacs.web.gwt.map.client.GoogleMap;
import org.janelia.it.jacs.web.gwt.map.client.panel.MapBox;
import org.janelia.it.jacs.web.gwt.status.client.JobResultsData;
import org.janelia.it.jacs.web.gwt.status.client.Status;
import org.janelia.it.jacs.web.gwt.status.client.panel.AlignmentListener;
import org.janelia.it.jacs.web.gwt.status.client.panel.BlastHitsPanel;
import org.janelia.it.jacs.web.gwt.status.client.panel.JobSummaryPanel;
import org.janelia.it.jacs.web.gwt.status.client.panel.SequenceAlignmentPanel;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * This is part of the job results wizard; this page shows one job's results.
 */
public class JobDetailsPage extends JobResultsWizardPage {
    private static Logger _logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.status.client.wizard.JobDetailsPage");
    public static final String HISTORY_TOKEN = "JobDetailsPage";
    private Panel _mainPanel;
    private JobSummaryPanel _summaryPanel;
    private Panel _numSitesPanel;
    private MapBox _mapPanel;
    private BlastHitsPanel _hitPanel;
    private SequenceAlignmentPanel _alignmentPanel;

    public static String JOB_SUMMARY_HELP_URL_PROP = "JobSummary.HelpURL";

    private static DataServiceAsync _dataservice = (DataServiceAsync) GWT.create(DataService.class);

    static {
        ((ServiceDefTarget) _dataservice).setServiceEntryPoint("data.srv");
    }

    private static StatusServiceAsync _statusservice = (StatusServiceAsync) GWT.create(StatusService.class);

    static {
        ((ServiceDefTarget) _statusservice).setServiceEntryPoint("status.srv");
    }

    public JobDetailsPage(JobResultsData data, WizardController controller) {
        super(data, controller);
        init();
    }

    public Widget getMainPanel() {
        return _mainPanel;
    }

    public String getPageToken() {
        if (getData().getJob() != null)
            return HISTORY_TOKEN + "?" + Status.JOB_ID_PARAM + "=" + getData().getJob().getJobId();
        else if (getData().getJobId() != null)
            return HISTORY_TOKEN + "?" + Status.JOB_ID_PARAM + "=" + getData().getJobId();
        else
            return HISTORY_TOKEN;
    }

    public String getPageTitle() {
        return Constants.JOBS_JOB_DETAILS_LABEL;
    }

    protected void setupButtons() {
        super.setupButtons(); // start with defaults;

        RoundedButton backButton = getButtonManager().getBackButton();
        backButton.setVisible(true);
        backButton.setEnabled(true);
        backButton.setText("Back to Job Results");

        getButtonManager().getNextButton().setVisible(false);
    }

//    private class BackToJobResultsLink extends ActionLink {
//        private BackToJobResultsLink(String linkText,ClickListener clickListener) {
//            super(linkText,clickListener);
//        }
//
//        protected void onAttach() {
//            super.onAttach();
//            int currentPage=getController().getCurrentPageIndex();
//            String backPageToken=getController().getPageTokenAt(currentPage-1);
//            setTargetHistoryToken(backPageToken);
//        }
//

    //    }
    private void init() {
        _mainPanel = new VerticalPanel();

        // Create a job summary panel with a back link 
        BackActionLink backLink = new BackActionLink("back to jobs", new ClickListener() {
            public void onClick(Widget widget) {
                getController().back();
            }
        });

        _summaryPanel = new JobSummaryPanel("Job Summary");
        TitledBoxActionLinkUtils.addHelpActionLink(_summaryPanel, new HelpActionLink("help"), JOB_SUMMARY_HELP_URL_PROP);
        _summaryPanel.addActionLink(backLink);
        _summaryPanel.setWidth("100%");

        // Panel to show the hits
        _hitPanel = new BlastHitsPanel("Matching Sequences", new AlignmentSelectedListener(), new EntitySelectedListener());

        // Panel to show the alignment
        _alignmentPanel = new SequenceAlignmentPanel("Sequence Alignment", new EntitySelectedListener());

        // Panel for the map
        SimplePanel mapHints = new SimplePanel();
        mapHints.setStyleName("jobDetailsHitTableHint");
        mapHints.add(HtmlUtils.getHtml(
                "&bull; Each sample site is marked on the map.<br>" +
                        "&bull; Click a site marker for more information<br>" +
                        "&bull; Drag the map with your mouse, or double-click to change region and zoom", //TODO: enable double-click zooming
                //TODO: add (external) link to Google Maps help - http://www.google.com/intl/en_us/help/maps/tour/
                "hint"));

        _numSitesPanel = new VerticalPanel();
        _numSitesPanel.setStyleName("jobDetailsNumSitesPanel");
        _numSitesPanel.setVisible(false);

        _mapPanel = new MapBox("Sequence Geography", null);
        _mapPanel.add(_numSitesPanel);
        //TODO: find a better way to make a gray line
        _mapPanel.add(HtmlUtils.getHtml("----------------------------------------------------------------------------------------------------", "hint"));
        _mapPanel.add(mapHints);

/*
    +-vertpanel-row-------------------------
       +-Job Summary ----------------+
    +-row-----------------------------------
       +-horizpanel----------------------------
          +vertpanel---+ +vertpanel-+
          | +-Hits--+  | | +-Map--+ |
          | |       |  | | |      | |
          | +-------+  | | +------+ |
          | +-Align-+  | |----------+
          | |       |  | |
          | +-------+  | |
          + -----------+ |
       +-horizpanel----------------------------
    +---------------------------------------
 */

        HorizontalPanel everythingElsePanel = new HorizontalPanel();
        everythingElsePanel.setWidth("100%");
        VerticalPanel col1 = new VerticalPanel();
        VerticalPanel col2 = new VerticalPanel();

        everythingElsePanel.add(col1);
        everythingElsePanel.add(HtmlUtils.getHtml("&nbsp;", "spacer"));
        everythingElsePanel.add(col2);

        col1.add(_hitPanel);
        col1.add(HtmlUtils.getHtml("&nbsp;", "spacer"));
        col1.add(_alignmentPanel);

        col2.add(_mapPanel);

        _mainPanel.add(_summaryPanel);
        _mainPanel.add(HtmlUtils.getHtml("&nbsp;", "spacer"));
        _mainPanel.add(everythingElsePanel);
    }

    private class AlignmentSelectedListener implements AlignmentListener {
        public void onAlignmentSelected(BlastHit hit, String program) {
            _alignmentPanel.setAlignment(hit, program);
        }
    }

    private class EntitySelectedListener implements EntityListener {
        public void onEntitySelected(String entityAcc, Object entityData) {
            getData().setDetailAcc(entityAcc);
            getController().next();
        }
    }

    /**
     * This is invoked by the WizardController to notify us that the page should load.  We'll decide what to do
     * based on which page we're coming from.
     */
    protected void preProcess(Integer lastPage) {
        // If we're starting the wizard on this page, retrieve the job from the database
        if (lastPage == null) {
            _logger.debug("JobDetailsPage - starting entry point, so retrieving job.");
            reset();
            getJob();
        }
        // If we're coming from the first page, then a job was selected, so clear any old data and load the new job
        else if (lastPage < getController().findPageByName(getPageToken())) {
            _logger.debug("JobDetailsPage - coming from earlier page so retrieving job data.");
            reset();
            setupPanels();
        }
        // If we're coming back from ReadDetails, leave the current data as is
        else if (_logger.isDebugEnabled())
            _logger.debug("JobDetailsPage - coming back from later page, so retaining current information");
    }

    private void reset() {
        _logger.trace("JobDetailsPage.reset()");
        _mapPanel.reset();
        _alignmentPanel.reset();
        _hitPanel.reset();
        _numSitesPanel.clear();
    }

    /* Clear stuff from memory when going back to the first page */
    protected void postProcess(Integer nextPageNumber) {
        // Use a timer so the reset isn't visible to the user
        if (nextPageNumber != null && nextPageNumber < getController().findPageByName(getPageToken()))
            new ResetTimer().schedule(1000);
    }

    private class ResetTimer extends Timer {
        public void run() {
            _logger.debug("JobDetailsPage - going to earlier page so clearing all data.");
            _mapPanel.reset();
            _alignmentPanel.reset();
            _hitPanel.reset();
            _numSitesPanel.clear();
        }
    }

    private void setupPanels() {
        _logger.trace("JobDetailsPage.setupPanels()");
        _summaryPanel.setJob((BlastJobInfo) getData().getJob());
        _hitPanel.setJob((BlastJobInfo) getData().getJob());
        new MapTimer().schedule(1000); // 1 sec delay for hit table to update onscreen
    }

    /**
     * Called when user uses back button in browser to load the entry point on theis details page;  since the whole
     * list of jobs hasn't been loaded on the 1st page, we'll get the job identified by ID in the URL
     */
    private void getJob() {
        _logger.trace("JobDetailsPage.getJob()");
        //if (getData() == null || getData().getJobId() == null)
        //    setup
        _statusservice.getTaskResultForUser(getData().getJobId(), new AsyncCallback() {
            public void onFailure(Throwable throwable) {
                setErrorMessageInAllPanels("Failure retrieving job " + getData().getJobId());
            }

            public void onSuccess(Object object) {
                getData().setJob((org.janelia.it.jacs.shared.tasks.BlastJobInfo) object);
                setupPanels();
            }
        });
    }

    private void setErrorMessageInAllPanels(String message) {
        _logger.error(message);
        _mapPanel.clear();
        _mapPanel.add(HtmlUtils.getHtml(message, "error"));
        _numSitesPanel.setVisible(false);
        _summaryPanel.clear();
        _summaryPanel.add(HtmlUtils.getHtml(message, "error"));
        _hitPanel.clear();
        _hitPanel.add(HtmlUtils.getHtml(message, "error"));
    }

    // TODO: move this into a MapBox subclass to manage
    public class MapTimer extends Timer {
        public void run() {
            _dataservice.getSitesForBlastResult(getData().getJob().getJobId(), new MapDataRetrieved());
        }
    }

    private class MapDataRetrieved implements AsyncCallback {
        public static final int MAP_SIZE = 400;

        public void onFailure(Throwable throwable) {
            _logger.error("getSitesForBlastResult().onFailure(): ", throwable);
            setMapError();
        }

        public void onSuccess(Object object) {
            _logger.debug("getSitesForBlastResult().onSuccess()");
            if (object == null) {
                _logger.debug("got null map sites, setting no data message");
                setMapNoDataMessage();
            }
            else if (((Map) object).size() == 0) {
                _logger.debug("got empty map sites, setting no data message");
                setMapNoDataMessage();
            }
            else {
                if (_logger.isDebugEnabled()) _logger.debug("retrieved " + ((Map) object).size() + " sites for map");
                Map sites = (Map) object;

                // Create site markers and retrieve the map
                _logger.debug("creating map");
                Set<Marker> markers = getSiteMarkers(sites);
                GoogleMap map = new GoogleMap((markers.iterator().next()).getLatLng(), /*zoom*/3, markers, MAP_SIZE, MAP_SIZE);
                map.setMapType(MapType.getHybridMap());
                _mapPanel.setMap(map);

                // Configure the number of sites text and popup
                updateSiteNumLabel(sites);
                _logger.debug("map complete");
            }
        }

        private Set<Marker> getSiteMarkers(Map sites) {
            Set<Marker> markers = new HashSet<Marker>();
            for (Object o : sites.keySet()) {
                Site site = (Site) o;
                if (site.getLatitudeDouble() != null && site.getLongitudeDouble() != null) {
                    final Marker marker = new Marker(LatLng.newInstance(site.getLatitudeDouble(), site.getLongitudeDouble()));
                    final String html = getSiteMarkerHtml(site, (Integer) sites.get(site));
                    marker.addMarkerClickHandler(new MarkerClickHandler() {
                        public void onClick(MarkerClickEvent clickEvent) {
                            clickEvent.getSender().showMapBlowup(new InfoWindowContent(html));
                        }
                    });

                    markers.add(marker);
                }
            }
            return markers;
        }

        private void updateSiteNumLabel(Map markers) {
            // PopperUpper to show the sites
            Widget link = new PopperUpperHTML(String.valueOf(markers.size()) + " sample site" + ((markers.size() > 1) ? "s" : ""),
                    getSampleSitePopup(markers));
            DOM.setStyleAttribute(link.getElement(), "display", "inline");

            // Text after the popperupper
            HTML text = HtmlUtils.getHtml(((markers.size() > 1) ? "are" : "is") + " represented in this data set", "text");
            DOM.setStyleAttribute(text.getElement(), "display", "inline");

            // Create a panel with ">> N sites are represented in this data set" and add the link and text
            HTMLPanel numSites = new HTMLPanel(
                    "<span class='greaterGreater'>&gt;&gt;&nbsp;</span>" +
                            "<span id='numSitesLink'></span>&nbsp;<span id='numSitesCaption'></span>");
            numSites.setStyleName("text"); // for the space
            DOM.setStyleAttribute(numSites.getElement(), "display", "inline");

            numSites.add(link, "numSitesLink");
            numSites.add(text, "numSitesCaption");

            _numSitesPanel.add(numSites);
            _numSitesPanel.setVisible(true);
        }

        private HTML getSampleSitePopup(Map sites) {
            StringBuffer buf = new StringBuffer();
            for (Object o : sites.keySet()) {
                Site site = (Site) o;
                buf.append("<span class='infoPrompt'>&bull;&nbsp; ").append(site.getSiteId()).append("&nbsp;</span>");
                buf.append("<span class='infoText'>").append(site.getSampleLocation()).append("</span>");
                buf.append("<br>");
            }
            return HtmlUtils.getHtml(buf.toString(), "infoText");
        }

        private String getSiteMarkerHtml(Site site, Integer numHits) {
            return
                    "<span class='infoPrompt'>" + site.getSiteId() + "</span><br>" +
                            "<span class='infoText'>" + site.getSampleLocation() + "</span><br><br>" +
                            "<span class='infoText'>" + numHits + " matching sequence" + ((numHits > 1) ? "s" : "") + "</span>";
            //"<br>"
            //"<span class='greaterGreater'>&gt;&gt;</span>&nbsp;<span class='smallTextLink'>View matching sequences</span><br>" +
            //"<span class='greaterGreater'>&gt;&gt;</span>&nbsp;<span class='smallTextLink'>View sample data</span><br>" +
            //"&nbsp;"
            //);
        }

        private void setMapError() {
            _logger.error("Failure retrieving blast result node for job " + getData().getJob().getJobId());
            _mapPanel.setMessage("Error retrieving geographical information.", "error");
            _numSitesPanel.setVisible(false);
        }

        private void setMapNoDataMessage() {
            _logger.debug("No sites to map");
            _mapPanel.setMessage("No geographical information is available for these sequences.", "text");
            _numSitesPanel.setVisible(false);
        }

    }


}
