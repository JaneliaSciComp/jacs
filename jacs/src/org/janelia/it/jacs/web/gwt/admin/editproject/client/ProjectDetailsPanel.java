
package org.janelia.it.jacs.web.gwt.admin.editproject.client;

import com.google.gwt.user.client.ui.*;
import org.janelia.it.jacs.web.gwt.download.client.model.ProjectImpl;

/**
 * @author Guy Cao
 */
public class ProjectDetailsPanel extends HorizontalPanel {

    // GUI components //
    private TextBox nameBox = null;
    private TextBox symbolBox = null;
    private TextBox piBox;
    private TextBox affiliationBox;
    private TextBox projectOrganizationBox;
    private TextBox fundingOrganizationBox;
    private TextBox websiteBox;
    private TextBox emailBox;
    private CheckBox publicBox;

    public ProjectDetailsPanel(ProjectTabPanel parent) {

        super();

        createDetailsPanel();

        parent.add(this, "Project Details");

    }


    private void createDetailsPanel() {

        createInputFields();

        createRequiredPanel();
    }


    private void createRequiredPanel() {

        RequiredPanel requiredPanel = new RequiredPanel();

        this.setHorizontalAlignment(HorizontalPanel.ALIGN_RIGHT);
        this.add(requiredPanel);

        this.setStyleName("EPFullPanel");

    }


    private void createInputFields() {

        FlexTable detailsInputFields = new FlexTable();

        // NAME BOX //
        nameBox = new TextBox();
        //nameBox.setWidth(textFieldWidth);
        nameBox.setStyleName("EPTextBox");
        EPTableUtilities.addWidgetWidgetPair(detailsInputFields, 0, 0,
                new HTMLPanel("<span class='prompt'>Project Name:</span><span class='requiredInformation'>*</span>"),
                nameBox);


        // SYMBOL BOX //
        symbolBox = new TextBox();
        //symbolBox.setWidth("185");
        symbolBox.setStyleName("EPsymbolTextBox");

        DockPanel symbolPanel = new DockPanel();
        symbolPanel.setVerticalAlignment(HorizontalPanel.ALIGN_BOTTOM);
        Label symLabel = new Label("CAM_PROJ_");
        //symLabel.setStyleName("EPSymbolLabel");
        symLabel.setStyleName("text");
        symbolPanel.add(symLabel, DockPanel.WEST);
        symbolPanel.add(symbolBox, DockPanel.EAST);
        EPTableUtilities.addWidgetWidgetPair(detailsInputFields, 1, 0,
                new HTMLPanel("<span class='prompt'>Project Symbol:</span><span class='requiredInformation'>*</span>"),
                symbolPanel);

        // PRINCIPAL INVESTIGATOR BOX //
        piBox = new TextBox();
        //piBox.setWidth(textFieldWidth);
        piBox.setStyleName("EPTextBox");

        HorizontalPanel piPanel = new HorizontalPanel();
        piPanel.setVerticalAlignment(HorizontalPanel.ALIGN_MIDDLE);
        piPanel.add(piBox);
        piPanel.add(new HTMLPanel("<span class='commaList'>&nbsp;&nbsp;comma-separated list</span>"));
        EPTableUtilities.addPromptWidgetPair(detailsInputFields, 2, 0, "Principal Investigators(s)", piPanel);

        // AFFILIATION BOX //
        affiliationBox = new TextBox();
        //affiliationBox.setWidth(textFieldWidth);
        affiliationBox.setStyleName("EPTextBox");
        EPTableUtilities.addPromptWidgetPair(detailsInputFields, 3, 0, "Institutional Affiliation", affiliationBox);

        // PROJECT ORGANIZATION BOX //
        projectOrganizationBox = new TextBox();
        //projectOrganizationBox.setWidth(textFieldWidth);
        projectOrganizationBox.setStyleName("EPTextBox");
        HorizontalPanel poPanel = new HorizontalPanel();
        poPanel.setVerticalAlignment(HorizontalPanel.ALIGN_MIDDLE);
        poPanel.add(projectOrganizationBox);
        poPanel.add(new HTMLPanel("<span class='commaList'>&nbsp;&nbsp;comma-separated list</span>"));
        EPTableUtilities.addPromptWidgetPair(detailsInputFields, 4, 0, "Project Organization(s)", poPanel);

        // FUNDING ORGANIZATION BOX //
        fundingOrganizationBox = new TextBox();
        //fundingOrganizationBox.setWidth(textFieldWidth);
        fundingOrganizationBox.setStyleName("EPTextBox");
        HorizontalPanel foPanel = new HorizontalPanel();
        foPanel.setVerticalAlignment(HorizontalPanel.ALIGN_MIDDLE);
        foPanel.add(fundingOrganizationBox);
        foPanel.add(new HTMLPanel("<span class='commaList'>&nbsp;&nbsp;comma-separated list</span>"));
        EPTableUtilities.addPromptWidgetPair(detailsInputFields, 5, 0, "Funding Organization(s)", foPanel);

        // WEBSITE BOX //
        websiteBox = new TextBox();
        websiteBox.setStyleName("EPWebTextBox");
        HorizontalPanel websitePanel = new HorizontalPanel();
        websitePanel.setVerticalAlignment(HorizontalPanel.ALIGN_BOTTOM);
        Label webLabel = new Label("http://");
        webLabel.setStyleName("text");
        websitePanel.add(webLabel);
        websitePanel.add(websiteBox);
        EPTableUtilities.addPromptWidgetPair(detailsInputFields, 6, 0, "Website", websitePanel);

        // EMAIL BOX //
        emailBox = new TextBox();
        emailBox.setStyleName("EPTextBox");
        EPTableUtilities.addPromptWidgetPair(detailsInputFields, 7, 0, "Contact Email", emailBox);

        // MAKE PUBLIC BOX //
        publicBox = new CheckBox();
        publicBox.setStyleName("EPCheckBox");
        Label releaseProject = new Label("Make project public?");
        releaseProject.setStyleName("prompt");
        EPTableUtilities.addWidgetWidgetPair(detailsInputFields, 8, 0, releaseProject, publicBox);

        // disable all input fields until edit project radio buttons triggers it
        disableAllInput();


        this.add(detailsInputFields);
    }


    protected void disableAllInput() {
        nameBox.setEnabled(false);
        symbolBox.setEnabled(false);
        piBox.setEnabled(false);
        affiliationBox.setEnabled(false);
        projectOrganizationBox.setEnabled(false);
        fundingOrganizationBox.setEnabled(false);
        websiteBox.setEnabled(false);
        emailBox.setEnabled(false);
        publicBox.setEnabled(false);
    }


    protected void enableAllInput() {
        nameBox.setEnabled(true);
        symbolBox.setEnabled(true);
        piBox.setEnabled(true);
        affiliationBox.setEnabled(true);
        projectOrganizationBox.setEnabled(true);
        fundingOrganizationBox.setEnabled(true);
        websiteBox.setEnabled(true);
        emailBox.setEnabled(true);
        publicBox.setEnabled(true);
    }


    protected void clearInputTextBoxes() {
        nameBox.setText("");
        symbolBox.setText("");
        piBox.setText("");
        affiliationBox.setText("");
        projectOrganizationBox.setText("");
        fundingOrganizationBox.setText("");
        websiteBox.setText("");
        emailBox.setText("");
        publicBox.setValue(false);
    }


    public String getName() {
        return nameBox.getText();
    }

    public String getSymbol() {
        return symbolBox.getText();
    }

    public String getPI() {
        return piBox.getText();
    }

    public String getAffiliation() {
        return affiliationBox.getText();
    }

    public String getProjectOrganization() {
        return projectOrganizationBox.getText();
    }

    public String getFundingOrganization() {
        return fundingOrganizationBox.getText();
    }

    public String getWebsite() {
        return websiteBox.getText();
    }

    public String getEmail() {
        return emailBox.getText();
    }

    public boolean getMakePublic() {
        return publicBox.getValue();
    }


    ///////////////////////////////////////////////////


    public void updateProjectView(ProjectImpl project) {

        // clear everything
        clearInputTextBoxes();

        // all projects have these attributes
        nameBox.setText(project.getProjectName());
        symbolBox.setText(project.getProjectSymbol().substring(9));

        if (project.getPrincipalInvestigators() != null) {
            piBox.setText(project.getPrincipalInvestigators());
        }
        if (project.getInstitutionalAffiliation() != null) {
            affiliationBox.setText(project.getInstitutionalAffiliation());
        }
        if (project.getOrganization() != null) {
            projectOrganizationBox.setText(project.getOrganization());
        }
        if (project.getFundedBy() != null) {
            fundingOrganizationBox.setText(project.getFundedBy());
        }
        if (project.getWebsite() != null) {
            websiteBox.setText(project.getWebsite().substring(7));
        }
        if (project.getEmail() != null) {
            emailBox.setText(project.getEmail());
        }

        // "make public" -to- "release" -> waiting to be resolved.
        //if (project.get)


    }


    // sample
    public void setSampleNewProject() {

        System.out.println("setting sample new project");

        nameBox.setText("Whale Scat Project");
        symbolBox.setText("WhaleScat");
        piBox.setText("Forest Rowher");
        affiliationBox.setText("Massachusetts Institutes of Technology");
        projectOrganizationBox.setText("SDSU Center for Universal Microbial Sequencing");
        fundingOrganizationBox.setText("Moore Fundation");
        websiteBox.setText("www.yahoo.com/"); // add http://
    }


}
