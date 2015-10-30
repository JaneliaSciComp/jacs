
package org.janelia.it.jacs.web.gwt.search.client.panel.project;

import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.janelia.it.jacs.model.tasks.search.SearchTask;
import org.janelia.it.jacs.web.gwt.common.client.ui.paging.AbstractPagingPanel;
import org.janelia.it.jacs.web.gwt.common.client.ui.paging.SimplePaginator;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.paging.PagedDataRetriever;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;
import org.janelia.it.jacs.web.gwt.search.client.model.ProjectResult;
import org.janelia.it.jacs.web.gwt.search.client.panel.CategorySearchDataBuilder;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Aug 29, 2007
 * Time: 10:54:25 AM
 */
public class ProjectSearchDataBuilder extends CategorySearchDataBuilder {
    // constants for column headings
    private static String NAME_HEADING = "Project Name";
    private static String DESCRIPTION_HEADING = "Description";
    private static String EMAIL_HEADING = "Email";
    private static String URL_HEADING = "Url";
    private static String RELEASED_HEADING = "Released";
    private static String INVESTIGATORS_HEADING = "Investigators";
    private static String ORGANIZATION_HEADING = "Organization";
    private static String FUNDING_HEADING = "Funding";
    private static String INSTITUTION_HEADING = "Institution";
    private static String RANK_HEADING = "Rank";

    private static final String DATA_PANEL_TITLE = "All Matching Projects";

    private class ProjectAbstractsPanel extends AbstractPagingPanel {
        ProjectAbstractsPanel(SimplePaginator paginator, String[] pageSizeOptions) {
            super(paginator, pageSizeOptions);
            setPagingControlPanelStyleName("SimplePaginatorLocator");
        }

        public void render(Object data) {
            List projectList = (List) data;
            dataPanel.clear();
            if (projectList == null || projectList.size() == 0) {
                String message = "No project found";
                dataPanel.clear();
                dataPanel.add(HtmlUtils.getHtml(message, "text"));
            }
            else {
                for (Object aProjectList : projectList) {
                    ProjectResult projectResult = (ProjectResult) aProjectList;
                    dataPanel.add(createProjectAbstractPanel(projectResult));
                }
            }
        }

        public void renderError(Throwable throwable) {
            String errorMessage = "Error retrieving data: " + throwable.getMessage();
            dataPanel.clear();
            dataPanel.add(HtmlUtils.getHtml(errorMessage, "error"));
        }

        private Panel createProjectAbstractPanel(ProjectResult projectResult) {
            VerticalPanel projectAbstractPanel = new VerticalPanel();
            projectAbstractPanel.setStyleName("ProjectAbstractPanel");
            // add project's name
            Widget projectNameLink = getAccessionLink(projectResult.getName(), projectResult.getAccession());
            projectNameLink.setStyleName("ProjectName");
            projectAbstractPanel.add(projectNameLink);

            // Add project's primary investigator & organization on the same line
            HorizontalPanel infoPanel1 = new HorizontalPanel();
            infoPanel1.add(HtmlUtils.getHtml("Principal Investigator: ", "prompt"));
            infoPanel1.add(HtmlUtils.getHtml("&nbsp;" + projectResult.getInvestigators(), "text"));
            if (projectResult.getOrganization() != null) {
                infoPanel1.add(HtmlUtils.getHtml("&nbsp;&nbsp;&nbsp;&nbsp;", "text"));
                infoPanel1.add(HtmlUtils.getHtml("Organization:", "prompt"));
                infoPanel1.add(HtmlUtils.getHtml("&nbsp;" + projectResult.getOrganization(), "text"));
            }
            projectAbstractPanel.add(infoPanel1);

            // Add funding source on new line
            HorizontalPanel infoPanel2 = new HorizontalPanel();
            infoPanel2.add(HtmlUtils.getHtml("Funded By:", "prompt"));
            infoPanel2.add(HtmlUtils.getHtml("&nbsp;" + projectResult.getFundingSource(), "text"));
            projectAbstractPanel.add(infoPanel2);

            // Add rank on new line
            HorizontalPanel infoPanel3 = new HorizontalPanel();
            infoPanel3.add(HtmlUtils.getHtml("Rank:", "prompt"));
            infoPanel3.add(HtmlUtils.getHtml("&nbsp;" + projectResult.getRank().toString(), "text"));
            projectAbstractPanel.add(infoPanel3);

            // Add headline
//            projectAbstractPanel.add(getHeadline(projectResult.getHeadline()));

            return projectAbstractPanel;
        }

    }

    private SimplePaginator projectsPaginator;

    public ProjectSearchDataBuilder(String searchId, String searchQuery) {
        super(searchId, searchQuery);
    }

    public void populateDataPanel() {
        if (!_haveData) {
            projectsPaginator.first();
        }
    }

    protected String getPanelSearchCategory() {
        return SearchTask.TOPIC_PROJECT;
    }

    protected String[][] getSortOptions() {
        return new String[][]{
                {"symbol", NAME_HEADING},
                {null, DESCRIPTION_HEADING},
                {"email", EMAIL_HEADING},
                {"website_url", URL_HEADING},
                {"released", RELEASED_HEADING},
                {"principal_investigators", INVESTIGATORS_HEADING},
                {"organization", ORGANIZATION_HEADING},
                {"funded_by", FUNDING_HEADING},
                {"institutional_affiliation", INSTITUTION_HEADING},
                {"rank", RANK_HEADING}
        };
    }

    protected List formatDataListAsTableRowList(List dataList) {
        // simply return the same data and don't do any formatting in this case
        return dataList;
    }

    protected PagedDataRetriever createDataRetriever() {
        return new CategoryResultDataRetriever();
    }

    protected Panel createDataPanel(int defaultNumVisibleRows, String[] pageLengthOptions) {
        projectsPaginator = new SimplePaginator(createDataRetriever(), defaultNumVisibleRows, getSortOptions());
        ProjectAbstractsPanel projectAbstractsPanel = new ProjectAbstractsPanel(projectsPaginator, pageLengthOptions);
        projectAbstractsPanel.setWidth("100%");
        return projectAbstractsPanel;
    }

    protected String createDataPanelTitle() {
        return DATA_PANEL_TITLE;
    }

}
