
package org.janelia.it.jacs.web.gwt.download.client.project;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.ui.*;
import org.janelia.it.jacs.web.gwt.common.client.panel.TitledBox;
import org.janelia.it.jacs.web.gwt.common.client.panel.TitledBoxFactory;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.common.client.ui.LoadingLabel;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.ExternalLink;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.Link;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.MailToLink;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;
import org.janelia.it.jacs.web.gwt.common.client.util.UrlBuilder;
import org.janelia.it.jacs.web.gwt.download.client.DownloadMetaDataService;
import org.janelia.it.jacs.web.gwt.download.client.DownloadMetaDataServiceAsync;
import org.janelia.it.jacs.web.gwt.download.client.model.Project;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Oct 9, 2007
 * Time: 4:42:32 PM
 */
public class ProjectInfoPanel extends Composite {
    private static Logger _logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.download.client.ProjectDetailPanel");

    private Project _project;
    private VerticalPanel _mainPanel;
    private LoadingLabel _loadingLabel;
    private TitledBoxFactory.BoxType _boxType;

    private static DownloadMetaDataServiceAsync downloadService = (DownloadMetaDataServiceAsync) GWT.create(DownloadMetaDataService.class);

    static {
        ((ServiceDefTarget) downloadService).setServiceEntryPoint("download.oas");
    }

    /**
     * For deferred project assignment via setProject(Project)
     */
    public ProjectInfoPanel(TitledBoxFactory.BoxType moreInfoBoxType) {
        _boxType = moreInfoBoxType;
        init();
    }

    public ProjectInfoPanel(String projectSymbol, TitledBoxFactory.BoxType moreInfoBoxType) {
        _boxType = moreInfoBoxType;
        init();
        retrieveProjectAndBuildPanel(projectSymbol);
    }

    public ProjectInfoPanel(Project project, TitledBoxFactory.BoxType moreInfoBoxType) {
        _project = project;
        _boxType = moreInfoBoxType;
        init();
        buildPanel();
    }

    private void init() {
        _mainPanel = new VerticalPanel();
        initWidget(_mainPanel);

        setWidth("100%");

        _loadingLabel = new LoadingLabel();
        _loadingLabel.setVisible(true);
    }

    public void setProject(String projectSymbol) {
        retrieveProjectAndBuildPanel(projectSymbol);
    }

    private void retrieveProjectAndBuildPanel(final String projectSymbol) {
        downloadService.getProjectBySymbol(projectSymbol, new AsyncCallback() {
            public void onFailure(Throwable throwable) {
                //TODO: inform user
                _logger.error("ProjectDetailPanel.retrieveProject(" + projectSymbol + ") failed:" + throwable.getMessage(), throwable);
            }

            public void onSuccess(Object object) {
                _logger.info("ProjectDetailPanel.retrieveProject(" + projectSymbol + ") returned successfully");
                _project = (Project) object;
                buildPanel();
            }
        });
    }

    protected void buildPanel() {
        _loadingLabel.setVisible(false);
        _mainPanel.clear();

        HTMLPanel panel = new HTMLPanel(new StringBuffer()
                .append("<span id='projectTitle'></span>")
                .append("<span class='MoreInfoLinksBox' id='projectLinks'></span>")
                .append("<span id='projectInfo'></span>")
                .append("<span id='projectDescription'></span>")
                .toString());
        panel.add(createProjectTitle(_project.getProjectName()), "projectTitle");
        panel.add(createProjectInfo(_project), "projectInfo");
        panel.add(createProjectLinks(_project), "projectLinks");
        panel.add(HtmlUtils.getHtml(_project.getDescription(), "text"), "projectDescription");

        _mainPanel.add(panel);
    }

    protected Widget createProjectTitle(String title) {
        HTML html = new HTML(title);
        html.setStyleName("BrowseProjectTitle");
        return html;
    }

    private Panel createProjectInfo(Project project) {
        int row = 0;
        FlexTable grid = new FlexTable();

        if (project.getPrincipalInvestigators() != null)
            addGridRow("Principal Investigator", project.getPrincipalInvestigators(), grid, row++);
        if (project.getFundedBy() != null)
            addGridRow("Funded By", project.getFundedBy(), grid, row++);
        if (project.getOrganization() != null)
            addGridRow("Organization", project.getOrganization(), grid, row++);
        if (project.getInstitutionalAffiliation() != null)
            addGridRow("Affiliation", project.getInstitutionalAffiliation(), grid, row);

        return (grid);
    }

    private void addGridRow(String prompt, String value, FlexTable grid, int row) {
        addGridRow(prompt, value, null, grid, row);
    }

    private void addGridRow(String prompt, String value, Widget widget, FlexTable grid, int row) {
        int col = 0;
        if (prompt != null) {
            grid.setWidget(row, col, HtmlUtils.getHtml(prompt + ":", "prompt"));
            grid.getCellFormatter().setStyleName(row, col, "gridCell");
            grid.getCellFormatter().addStyleName(row, col, "gridCellFullWidth");  // prevent the prompt from breaking on whitespace
            col++;
        }
        if (widget != null)
            grid.setWidget(row, col, widget);
        else {
            value = (value == null) ? "&nbsp;" : value;
            HTMLPanel setPanel = new HTMLPanel(value);
            setPanel.setStyleName("text");
            grid.setWidget(row, col, setPanel);
        }
        grid.getCellFormatter().setStyleName(row, col, "gridCell");
    }

    private Panel createProjectLinks(Project project) {
        TitledBox infoBox = TitledBoxFactory.createTitledBox("More Information", _boxType, /*show action links*/ false);
        Grid grid = new Grid(4, 1);

        int row = 0;
        grid.setWidget(row++, 0, createLinkCell(new Link("Samples", UrlBuilder.getSamplesUrl() + "?projectSymbol=" + project.getProjectSymbol())));
        grid.setWidget(row++, 0, createLinkCell(new Link("Publications & Data", UrlBuilder.getPubsUrl() + "?projectSymbol=" + project.getProjectSymbol())));
        if (project.getWebsite() != null)
            grid.setWidget(row++, 0, createLinkCell(new ExternalLink("Website", project.getWebsite())));
        if (project.getEmail() != null)
            grid.setWidget(row, 0, createLinkCell(new MailToLink("Contact", project.getEmail())));
        infoBox.add(grid);

        return (infoBox);
    }

    /**
     * Have to fill up the blank space with something other than the external link or else the double
     * underline will expand past the text
     */
    private Widget createLinkCell(Widget externalLink) {
        DockPanel panel = new DockPanel();
        String id = HTMLPanel.createUniqueId();
        HTMLPanel linkPanel = new HTMLPanel("<span class='greaterGreater'>&gt;&gt;&nbsp;</span><span id='" + id + "'></span>");
        linkPanel.add(externalLink, id);

        DOM.setStyleAttribute(linkPanel.getElement(), "display", "inline");
        DOM.setStyleAttribute(externalLink.getElement(), "display", "inline");
        panel.add(linkPanel, DockPanel.WEST);
        panel.add(new HTML("&nbsp;"), DockPanel.CENTER); // fill the rest of the row with blank space

        return panel;
    }
}
