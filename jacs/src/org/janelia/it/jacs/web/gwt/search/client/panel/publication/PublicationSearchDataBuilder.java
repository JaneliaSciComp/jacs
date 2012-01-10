
package org.janelia.it.jacs.web.gwt.search.client.panel.publication;

import com.google.gwt.user.client.ui.*;
import org.janelia.it.jacs.model.tasks.search.SearchTask;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.common.client.ui.paging.AbstractPagingPanel;
import org.janelia.it.jacs.web.gwt.common.client.ui.paging.SimplePaginator;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.paging.PagedDataRetriever;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;
import org.janelia.it.jacs.web.gwt.search.client.model.PublicationResult;
import org.janelia.it.jacs.web.gwt.search.client.panel.CategorySearchDataBuilder;

import java.util.Iterator;
import java.util.List;


/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Aug 29, 2007
 * Time: 10:54:25 AM
 */
public class PublicationSearchDataBuilder extends CategorySearchDataBuilder {
    private static Logger logger =
            Logger.getLogger("org.janelia.it.jacs.web.gwt.search.client.panel.publication.PublicationSearchDataBuilder");

    // constants for column headings
    private static String TITLE_HEADING = "Title";
    private static String AUTHORS_HEADING = "Authors";
    private static String JOURNAL_HEADING = "Journal";
    private static String DATE_HEADING = "Date";
    private static String PROJECTS_HEADING = "Projects";
    private static final String DATA_PANEL_TITLE = "All Matching Publications";

    private class PublicationAbstractsPanel extends AbstractPagingPanel {
        PublicationAbstractsPanel(SimplePaginator paginator, String[] pageSizeOptions) {
            super(paginator, pageSizeOptions);
            setPagingControlPanelStyleName("SimplePaginatorLocator");
        }

        public void render(Object data) {
            List publicationList = (List) data;
            dataPanel.clear();
            if (publicationList == null || publicationList.size() == 0) {
                String message = "No project found";
                dataPanel.clear();
                dataPanel.add(HtmlUtils.getHtml(message, "text"));
            }
            else {
                for (Iterator itr = publicationList.iterator(); itr.hasNext();) {
                    PublicationResult publicationResult = (PublicationResult) itr.next();
                    dataPanel.add(createPublicationAbstractPanel(publicationResult));
                }
            }
        }

        public void renderError(Throwable throwable) {
            String errorMessage = "Error retrieving data: " + throwable.getMessage();
            dataPanel.clear();
            dataPanel.add(HtmlUtils.getHtml(errorMessage, "error"));
        }

        private Panel createPublicationAbstractPanel(PublicationResult pub) {
            VerticalPanel publicationAbstractPanel = new VerticalPanel();
            publicationAbstractPanel.setStyleName("PublicationAbstractPanel");

            // add publication's title
            Widget publicationTitleLink = getAccessionLink(pub.getTitle(), pub.getAccession());
            publicationTitleLink.setStyleName("PublicationTitle");
            publicationAbstractPanel.add(publicationTitleLink);

            // add authors & document match
            if (pub.getAuthors() != null)
                publicationAbstractPanel.add(getAuthors(pub.getAuthors()));
            if (pub.getJournalEntry() != null)
                publicationAbstractPanel.add(getJournal(pub.getJournalEntry()));
            if (pub.getProjects() != null)
                publicationAbstractPanel.add(getProject(pub.getProjects()));
            publicationAbstractPanel.add(getRank(pub.getRank()));
//            publicationAbstractPanel.add(getHeadline(pub.getHeadline()));

            return publicationAbstractPanel;
        }

        private Widget getAuthors(String allAuthors) {
            String prompt = "Author:";
            if (allAuthors != null && allAuthors.indexOf(",") > 0)
                prompt = "Authors:";

            String authors = allAuthors;
            if (allAuthors != null && allAuthors.length() > 120)
                authors = allAuthors.substring(0, 120) + "&nbsp;...";

            return getPromptValuePair(prompt, authors);
        }

        private Widget getRank(Float rank) {
            return getPromptValuePair("Rank:", rank.toString());
        }

        private Widget getJournal(String journal) {
            return getPromptValuePair("Journal:", journal);
        }

        private Widget getProject(String project) {
            //TODO: make this a link to the project page
            return getPromptValuePair("Project:", project);
        }

        // TODO: make this usable by Project data panel
        private Widget getPromptValuePair(String prompt, String value) {
            HorizontalPanel panel = new HorizontalPanel();
            panel.add(HtmlUtils.getHtml(prompt, "prompt"));
            panel.add(HtmlUtils.getHtml("&nbsp;" + value, "text"));

            return panel;
        }

        private Widget getJournalHTML(String journalEntry) {
            return HtmlUtils.getHtml(journalEntry, "text");
        }

        private Widget getProjectHTML(String project) {
            return HtmlUtils.getHtml(project, "text");
        }

        private Widget getHeadline(String pub) {
            // Strip off the useless document accession
            HTML headline = HtmlUtils.getHtml(stripAccessionFromHeadline(pub), "text");
            headline.addStyleName("PublicationDocument");

            return headline;
        }

        private String stripAccessionFromHeadline(String pub) {
            if (pub != null && pub.indexOf("... [") > 0)
                pub = pub.substring(0, pub.indexOf("... [")) + "...";

            return pub;
        }
    }

    private SimplePaginator publicationsPaginator;

    public PublicationSearchDataBuilder(String searchId, String searchQuery) {
        super(searchId, searchQuery);
    }

    public void populateDataPanel() {
        if (!_haveData) {
            publicationsPaginator.first();
        }
    }

    protected String getPanelSearchCategory() {
        return SearchTask.TOPIC_PUBLICATION;
    }

    protected String[][] getSortOptions() {
        return new String[][]{
                {"title", TITLE_HEADING},
                {null, AUTHORS_HEADING},
                {"journal_entry", JOURNAL_HEADING},
                {null, DATE_HEADING},
                {null, PROJECTS_HEADING}
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
        publicationsPaginator = new SimplePaginator(createDataRetriever(), defaultNumVisibleRows, getSortOptions());
        PublicationAbstractsPanel publicationAbstractsPanel =
                new PublicationAbstractsPanel(publicationsPaginator, pageLengthOptions);
        publicationAbstractsPanel.setWidth("100%");
        return publicationAbstractsPanel;
    }

    protected String createDataPanelTitle() {
        return DATA_PANEL_TITLE;
    }

}
