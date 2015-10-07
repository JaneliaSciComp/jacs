
package org.janelia.it.jacs.web.gwt.download.client.samples.wizard;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.janelia.it.jacs.web.gwt.common.client.model.metadata.Sample;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.paging.DataRetrievedListener;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;
import org.janelia.it.jacs.web.gwt.common.client.util.SpanTimerLogger;
import org.janelia.it.jacs.web.gwt.common.client.wizard.WizardController;
import org.janelia.it.jacs.web.gwt.download.client.DownloadMetaDataService;
import org.janelia.it.jacs.web.gwt.download.client.DownloadMetaDataServiceAsync;
import org.janelia.it.jacs.web.gwt.download.client.project.ProjectSymbolSelectedListener;
import org.janelia.it.jacs.web.gwt.download.client.project.ProjectsSelectedListener;
import org.janelia.it.jacs.web.gwt.download.client.samples.SampleMetadataTablePanel;
import org.janelia.it.jacs.web.gwt.download.client.samples.SampleSelectedListener;
import org.janelia.it.jacs.web.gwt.download.client.samples.SamplesMapPanel;
import org.janelia.it.jacs.web.gwt.download.client.samples.SamplesPage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Michael Press
 */
public class SamplesMapWizardPage extends SamplesWizardPage {
    private static Logger _logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.dowload.client.samples.Samples");

    private VerticalPanel _mainPanel;
    public boolean _dataLoaded = false;

    private static DownloadMetaDataServiceAsync downloadService = (DownloadMetaDataServiceAsync) GWT.create(DownloadMetaDataService.class);
    private List<DataRetrievedListener> _dataListeners;
    private SampleMetadataTablePanel _metadataPanel;

    public SamplesMapWizardPage(SampleInfo data, WizardController controller) {
        super(data, controller);
        init();
    }

    static {
        ((ServiceDefTarget) downloadService).setServiceEntryPoint("download.oas");
    }

    public void init() {
        SamplesMapPanel mapPanel = new SamplesMapPanel(new SampleSelectedOnMapListener(), getData(), new ProjectsSelectedListenerImpl());
        _metadataPanel = new SampleMetadataTablePanel(new ProjectSelectedListener(), getData());

        _dataListeners = new ArrayList<DataRetrievedListener>();
        _dataListeners.add(mapPanel);
        _dataListeners.add(_metadataPanel);

        // Build the page
        _mainPanel = new VerticalPanel();
        _mainPanel.add(mapPanel);
        _mainPanel.add(HtmlUtils.getHtml("&nbsp;", "spacer"));
        _mainPanel.add(_metadataPanel);
    }

    private class ProjectsSelectedListenerImpl implements ProjectsSelectedListener {
        public void onSelect(List<String> projectNames) {
            _metadataPanel.setSelectedProjects(projectNames);
        }
    }

    public Widget getMainPanel() {
        return _mainPanel;
    }

    protected void preProcess(Integer priorPageNumber) {
        if (_dataLoaded)
            return;

        // Schedule data retrieval
        DeferredCommand.addCommand(new Command() {
            public void execute() {
                getSamples();
            }
        });
    }

    private void getSamples() {
        final SpanTimerLogger samplesTimer = new SpanTimerLogger("Retrieving samples");
        samplesTimer.start();
        downloadService.getProjectSamplesByProject(new AsyncCallback() {
            public void onFailure(Throwable throwable) {
                _logger.error("GetProjectSamplesCallback failed: " + throwable.getMessage());
                for (DataRetrievedListener listener : _dataListeners)
                    listener.onFailure(throwable);
            }

            public void onSuccess(Object result) {
                samplesTimer.end();
                _logger.debug("GetProjectSamplesCallback returned onSuccess()");

                _dataLoaded = true;
                Map<String, List<Sample>> samples = (Map<String, List<Sample>>) result;

                // Notify data listeners
                final SpanTimerLogger processTimer = new SpanTimerLogger("Processing samples");
                processTimer.start();
                if (samples == null || samples.size() == 0) {
                    for (DataRetrievedListener listener : _dataListeners)
                        listener.onNoData();
                }
                else {
                    for (DataRetrievedListener listener : _dataListeners)
                        listener.onSuccess(samples);
                    processTimer.end();
                }
            }
        });
    }

    public String getPageToken() // used for history
    {
        return "";
    }

    public String getPageTitle() {
        return null;
    }

    private class SampleSelectedOnMapListener implements SampleSelectedListener {
        public void onSelect(Sample sample) {
            getData().setCurrentSample(sample);
            getController().gotoPage(SamplesPage.SAMPLE_DETAIL_PAGE);
        }

        public void onUnSelect(Sample project) {
        }
    }

    private class ProjectSelectedListener implements ProjectSymbolSelectedListener {
        public void onSelect(String projectSymbol) {
            _metadataPanel.clearHover();
            getData().setCurrentProject(projectSymbol);
            getController().gotoPage(SamplesPage.PROJECT_DETAIL_PAGE);
        }

        public void onUnSelect(String projectSymbol) {
        }
    }
}