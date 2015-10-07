
package org.janelia.it.jacs.web.gwt.download.client;

import com.google.gwt.user.client.ui.*;
import org.janelia.it.jacs.web.gwt.common.client.BaseEntryPoint;
import org.janelia.it.jacs.web.gwt.common.client.Constants;
import org.janelia.it.jacs.web.gwt.common.client.model.download.DownloadableDataNode;
import org.janelia.it.jacs.web.gwt.common.client.panel.TitledPanel;
import org.janelia.it.jacs.web.gwt.common.client.service.ResultReceiver;
import org.janelia.it.jacs.web.gwt.download.client.formatter.DataFileFormatter;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Lfoster
 * Date: Sep 14, 2006
 * Time: 1:57:22 PM
 * <p/>
 * Landing point for new files.  Let users see the list and grab what they want.
 */
public class DownloadNewFilesPage extends BaseEntryPoint {

    private List newFiles;
    private Grid newFilesGrid;

    public void onModuleLoad() {
        // Create and fade in the page contents
        clear();
        setBreadcrumb(new Breadcrumb(Constants.DATA_NEW_FILES_LABEL), Constants.ROOT_PANEL_NAME);
        populateNewFiles();
    }

    /**
     * Externally callable setter, to see that list is there.  Can be called by a framework.
     *
     * @param newFiles what is new.
     */
    public void setNewFilesList(List newFiles) {
        this.newFiles = newFiles;
        getContentPanel();
    }

    private void populateNewFiles() {
        // Will Add the model list from within, if it is not already setup.
        if (newFiles == null || newFiles.size() == 0) {

            ResultReceiver rcv = new ResultReceiver() {
                public void setResult(Object result) {
                    if (result instanceof List) {
                        setNewFilesList((List) result);
                    }
                }
            };
            PublicationServiceHelper.populateNewFiles(rcv);

        }

    }

    /**
     * Creates the main panel and adds it to the display.
     */
    private void getContentPanel() {
        // At this point, the list of new files will have been made ready.
        TitledPanel newFilesPanel = new TitledPanel("New Files", false, true);
        newFilesGrid = new Grid(newFiles.size() + 1, 4);
        newFilesGrid.setStyleName("DownloadNewFileGrid");

        DataFileFormatter formatter = new DataFileFormatter();

        // First row is for headers.
        HTML filesHeaderHTML = new HTML("File");
        filesHeaderHTML.setStyleName("DownloadNewFileFilesHeader");

        HTML dateHeaderHTML = new HTML("Date Added");
        dateHeaderHTML.setStyleName("DownloadNewFileDateHeader");

        HTML commentHeaderHTML = new HTML("Comment");
        commentHeaderHTML.setStyleName("DownloadNewFileCommentHeader");

        HTML sizeHeaderHTML = new HTML("File Size");
        sizeHeaderHTML.setStyleName("DownloadNewFileSizeHeader");

        newFilesGrid.setWidget(0, 0, filesHeaderHTML);
        newFilesGrid.setWidget(0, 1, dateHeaderHTML);
        newFilesGrid.setWidget(0, 2, commentHeaderHTML);
        newFilesGrid.setWidget(0, 3, sizeHeaderHTML);

        // Subsequent rows are for table data.
        for (int i = 1; i < newFiles.size() + 1; i++) {
            // Alternate the row-specific display settings.
            HTMLTable.RowFormatter rowFormatter = newFilesGrid.getRowFormatter();
            rowFormatter.addStyleName(i, i % 2 == 0 ? "DownloadNewFilesEvenRow" : "DownloadNewFilesOddRow");

            int nextFileNumber = i - 1;
            DownloadableDataNode nextFile = (DownloadableDataNode) newFiles.get(nextFileNumber);    // Skipping one row for the heading.
            ClickListener rowClickListener = new RowClickListener(i, nextFileNumber);

            // First column: the brief file name description.
            String text = nextFile.getText();
            if (text == null)
                text = "Unknown";

            HTML textHTML = new HTML(text);
            textHTML.setStyleName("DownloadNewFilesFileColumn");
            textHTML.addClickListener(rowClickListener);
            newFilesGrid.setWidget(i, 0, textHTML);

            // Second column: the date file was added.
            String dateAdded = nextFile.getAttribute("Date Added");
            if (dateAdded == null)
                dateAdded = "Unknown";
            HTML dateAddedHTML = new HTML(dateAdded);
            dateAddedHTML.setStyleName("DownloadNewFilesDateColumn");
            dateAddedHTML.addClickListener(rowClickListener);
            newFilesGrid.setWidget(i, 1, dateAddedHTML);

            // Third column: any comment.
            String comment = nextFile.getAttribute("Status");
            if (comment == null)
                comment = "";
            HTML commentHTML = new HTML(comment);
            commentHTML.setStyleName("DownloadNewFilesCommentColumn");
            commentHTML.addClickListener(rowClickListener);
            newFilesGrid.setWidget(i, 2, commentHTML);

            // Fourth column: file size.
            String size = formatter.abbreviateSize(nextFile.getSize());
            HTML sizeHTML = new HTML(size);
            sizeHTML.setStyleName("DownloadNewFilesSizeColumn");
            sizeHTML.addClickListener(rowClickListener);
            newFilesGrid.setWidget(i, 3, sizeHTML);
        }

        VerticalPanel insetsPanel = new VerticalPanel();
        insetsPanel.add(newFilesGrid);
        newFilesPanel.add(insetsPanel);
        insetsPanel.setStyleName("DownloadNewFilesInsetsPanel");
        RootPanel.get(Constants.ROOT_PANEL_NAME).add(newFilesPanel);

        show();
    }

    /**
     * Listen for a click on something in a row of the table, and when it happens,
     * popup something for the file represented by that row.
     */
    class RowClickListener implements ClickListener {
        private int rowNumber;
        private int fileNumber;

        public RowClickListener(int rowNumber, int fileNumber) {
            this.rowNumber = rowNumber;
            this.fileNumber = fileNumber;
        }

        public void onClick(Widget sender) {
            HTMLTable.RowFormatter rowFormatter = newFilesGrid.getRowFormatter();
            for (int i = 1; i < newFiles.size() + 1; i++) {
                rowFormatter.removeStyleName(i, "DownloadNewFilesHighlightedRow");
            }
            rowFormatter.addStyleName(rowNumber, "DownloadNewFilesHighlightedRow");
            DownloadableDataNode file = (DownloadableDataNode) newFiles.get(fileNumber);
            VerticalPanel panel = new FileAttributesPanel(file);
            DataFileFormatter formatter = new DataFileFormatter();

            // Figure out the position of the widget on screen.
            Widget refWidget = newFilesGrid.getWidget(rowNumber, fileNumber);
            int x = refWidget.getAbsoluteLeft();
            int y = refWidget.getAbsoluteTop();
            formatter.showFileMetaData(panel, x, y);
        }
    }

}
