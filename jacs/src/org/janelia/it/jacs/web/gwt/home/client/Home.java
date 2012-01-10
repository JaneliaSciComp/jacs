
package org.janelia.it.jacs.web.gwt.home.client;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import org.janelia.it.jacs.shared.tasks.JobInfo;
import org.janelia.it.jacs.web.gwt.common.client.BaseEntryPoint;
import org.janelia.it.jacs.web.gwt.common.client.Constants;
import org.janelia.it.jacs.web.gwt.common.client.jobs.JobSelectionListener;
import org.janelia.it.jacs.web.gwt.common.client.panel.TitledBox;
import org.janelia.it.jacs.web.gwt.common.client.panel.TitledBoxActionLinkUtils;
import org.janelia.it.jacs.web.gwt.common.client.panel.user.jobs.BlastJobResultsPanel;
import org.janelia.it.jacs.web.gwt.common.client.popup.jobs.JobCompletedPopup;
import org.janelia.it.jacs.web.gwt.common.client.popup.launcher.PopupCenteredLauncher;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.HelpActionLink;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.Link;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;
import org.janelia.it.jacs.web.gwt.common.client.util.UrlBuilder;
import org.janelia.it.jacs.web.gwt.home.client.panel.PipelineListPanel;
import org.janelia.it.jacs.web.gwt.home.client.panel.ProjectsPanel;
import org.janelia.it.jacs.web.gwt.home.client.panel.ToolsPanel;
import org.janelia.it.jacs.web.gwt.home.client.panel.UserSettingsPanel;

public class Home extends BaseEntryPoint {
    public static String RESEARCH_HOME_HELP_LINK_PROP = "ResearchHome.HelpURL";
    private BlastJobResultsPanel _blastJobResultsPanel;

    public void onModuleLoad() {
        // Create and fade in the page contents
        clear();
        setBreadcrumb(new Breadcrumb(Constants.RESEARCH_HOME_LABEL), Constants.ROOT_PANEL_NAME);

        VerticalPanel mainHomePanel = new VerticalPanel();
        HorizontalPanel subPanel = new HorizontalPanel();
        subPanel.add(getProjectsPanel());
        subPanel.add(HtmlUtils.getHtml("&nbsp;", "spacer"));
        subPanel.add(getEntryPointsPanel()); // Research Entry Points Panel
        subPanel.add(HtmlUtils.getHtml("&nbsp;", "spacer"));
        subPanel.add(getPipelineListPanel());
        subPanel.add(HtmlUtils.getHtml("&nbsp;", "spacer"));
        mainHomePanel.add(subPanel);
        mainHomePanel.add(HtmlUtils.getHtml("&nbsp;", "spacer"));
        mainHomePanel.add(getUserSettingsPanel());
        mainHomePanel.add(HtmlUtils.getHtml("&nbsp;", "spacer"));
        mainHomePanel.add(getRecentActivityPanel()); // Row 2 - My Data, Tutorial and Help panels

        RootPanel.get(Constants.ROOT_PANEL_NAME).add(mainHomePanel);
        show();
    }

    private Panel getEntryPointsPanel() {
        HorizontalPanel panel = new HorizontalPanel();
        // removing "Featured Project" panel from Compute Server home page
//        panel.add(new FeaturedProjectPanel());
//        panel.add(HtmlUtils.getHtml("&nbsp;", "spacer"));

        ToolsPanel toolsPanel = new ToolsPanel();
        TitledBoxActionLinkUtils.addHelpActionLink(toolsPanel, new HelpActionLink("help"), RESEARCH_HOME_HELP_LINK_PROP);
        panel.add(toolsPanel);

        return panel;
    }

    private Panel getPipelineListPanel() {
        HorizontalPanel panel = new HorizontalPanel();
        PipelineListPanel pipelineListPanel = new PipelineListPanel();
        TitledBoxActionLinkUtils.addHelpActionLink(pipelineListPanel, new HelpActionLink("help"), RESEARCH_HOME_HELP_LINK_PROP);
        panel.add(pipelineListPanel);

        return panel;
    }

    private Panel getUserSettingsPanel() {
        HorizontalPanel panel = new HorizontalPanel();
        UserSettingsPanel userSettingsPanel = new UserSettingsPanel();
        TitledBoxActionLinkUtils.addHelpActionLink(userSettingsPanel, new HelpActionLink("help"), RESEARCH_HOME_HELP_LINK_PROP);
        panel.add(userSettingsPanel);

        return panel;
    }

    private Widget getRecentActivityPanel() {
        HorizontalPanel recentActivityPanel = new HorizontalPanel();
        //recentActivityPanel.add(new RecentSearchesPanel());
        recentActivityPanel.add(HtmlUtils.getHtml("&nbsp;", "spacer"));
        TitledBox blastBox = new TitledBox("Recent BLAST Results");
        //blastBox.removeActionLinks();
        blastBox.setWidth("300px"); // min width when contents hidden
        _blastJobResultsPanel = new BlastJobResultsPanel(new JobResultsSelectedListener(), new ReRunJobListener(), new String[]{"5", "10", "20"}, 10);
        _blastJobResultsPanel.setJobCompletedListener(new JobCompletedListener());
        _blastJobResultsPanel.showActionColumn(false);
        _blastJobResultsPanel.showDeletionColumn(false);
        _blastJobResultsPanel.showProgramColumn(false);
        blastBox.add(_blastJobResultsPanel);
        Link viewAllResultsLink = new Link("View All BLAST Results", new ClickListener() {
            public void onClick(Widget sender) {
                Window.open(Constants.SERVLET_CONTEXT + "/gwt/Status/Status.htm", "_self", "");
            }
        });
        blastBox.add(viewAllResultsLink);
        recentActivityPanel.add(blastBox);

        // load recent jobs table when browser's done with initial content load
        DeferredCommand.addCommand(new Command() {
            public void execute() {
                _blastJobResultsPanel.first();
            }
        });

        return recentActivityPanel;
    }

    public Widget getProjectsPanel() {
        HorizontalPanel panel = new HorizontalPanel();
        ProjectsPanel projectsPanel = new ProjectsPanel();
        TitledBoxActionLinkUtils.addHelpActionLink(projectsPanel, new HelpActionLink("help"), RESEARCH_HOME_HELP_LINK_PROP);
        panel.add(projectsPanel);
        return panel;
    }

    private class JobResultsSelectedListener implements JobSelectionListener {
        public void onSelect(JobInfo job) {
            Window.open(UrlBuilder.getStatusUrl() + "#JobDetailsPage" + "?jobId=" + job.getJobId(), "_self", "");
        }

        public void onUnSelect() {
        }
    }

    private class JobCompletedListener implements JobSelectionListener {
        public void onSelect(JobInfo job) {
            new PopupCenteredLauncher(new JobCompletedPopup(job)).showPopup(null);
        }

        public void onUnSelect() {
        }
    }

    private class ReRunJobListener implements JobSelectionListener {
        public void onSelect(JobInfo job) {
        }

        public void onUnSelect() {
        }
    }


}
