
package org.janelia.it.jacs.web.gwt.admin.editpublication.client;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.*;
import org.janelia.it.jacs.web.gwt.common.client.popup.launcher.PopupBelowLauncher;
import org.janelia.it.jacs.web.gwt.common.client.ui.HoverImageSetter;
import org.janelia.it.jacs.web.gwt.common.client.ui.SelectionListener;
import org.janelia.it.jacs.web.gwt.common.client.ui.imagebundles.ControlImageBundle;
import org.janelia.it.jacs.web.gwt.common.client.ui.imagebundles.ImageBundleFactory;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.ActionLink;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.SortableTable;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.columns.ImageColumn;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.columns.TextColumn;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.comparables.FulltextPopperUpperHTML;

public class AuthorTable extends SortableTable {

    private static final int NO_ROW_SELECTED = Integer.MIN_VALUE;

    private static final int AUTHOR_DELETE_COLUMN = 0;
    private static final int AUTHOR_NUM_COLUMN = 1;
    private static final int AUTHOR_NAME_COLUMN = 2;

    private static final int NAME_MAX_SIZE = 60;

    protected AuthorTableSelectionListener authorSelectionListener = null;


    public AuthorTable() {
        super();
        this.setStyleName("AuthorTable");

        // no more sorting for now
        addColumn(new ImageColumn(""));
        addColumn(new TextColumn("#", true));
        addColumn(new TextColumn("Author Name", false));

        this.setHighlightSelect(true);

        authorSelectionListener = new AuthorTableSelectionListener();
        this.addSelectionListener(authorSelectionListener);
    }


    protected void upButtonPressed() {
        if (authorSelectionListener != null && // listener can't be null
                authorSelectionListener.getRowSelected() != NO_ROW_SELECTED && // some row has to be selected
                authorSelectionListener.getRowSelected() != 1) // can't move up first row
        {

            String currAuthor = (String) this.getValue(authorSelectionListener.getRowSelected(), AUTHOR_NAME_COLUMN).getValue();
            String switchWithAuthor = (String) this.getValue(authorSelectionListener.getRowSelected() - 1, AUTHOR_NAME_COLUMN).getValue();

            // switch the name column only
            this.setValue(authorSelectionListener.getRowSelected(), AUTHOR_NAME_COLUMN, switchWithAuthor,
                    getNameWidget(switchWithAuthor, authorSelectionListener.getRowSelected()));

            this.setValue(authorSelectionListener.getRowSelected() - 1, AUTHOR_NAME_COLUMN, currAuthor,
                    getNameWidget(currAuthor, authorSelectionListener.getRowSelected() - 1));

            this.refresh();
            this.selectRow(authorSelectionListener.getRowSelected() - 1);
        }
        else if (authorSelectionListener.getRowSelected() == 1) {
            System.out.println("can't move up first author anymore");
        }
    }

    protected void downButtonPressed() {
        if (authorSelectionListener != null && // listener can't be null
                authorSelectionListener.getRowSelected() != NO_ROW_SELECTED && // some row has to be selected
                authorSelectionListener.getRowSelected() != this.getRowCount() - 1) // can't move down last row
        {

            String currAuthor = (String) this.getValue(authorSelectionListener.getRowSelected(), AUTHOR_NAME_COLUMN).getValue();
            String switchWithAuthor = (String) this.getValue(authorSelectionListener.getRowSelected() + 1, AUTHOR_NAME_COLUMN).getValue();

            // switch the name column only
            this.setValue(authorSelectionListener.getRowSelected(), AUTHOR_NAME_COLUMN, switchWithAuthor,
                    getNameWidget(switchWithAuthor, authorSelectionListener.getRowSelected()));

            this.setValue(authorSelectionListener.getRowSelected() + 1, AUTHOR_NAME_COLUMN, currAuthor,
                    getNameWidget(currAuthor, authorSelectionListener.getRowSelected() + 1));

            this.refresh();
            this.selectRow(authorSelectionListener.getRowSelected() + 1);
        }
        else if (authorSelectionListener.getRowSelected() == this.getRowCount() - 1) {
            System.out.println("can't move down last author anymore");
        }
    }

    protected class AuthorTableSelectionListener implements SelectionListener {

        int rowSelected = NO_ROW_SELECTED;

        public void onSelect(String rowString) {
            rowSelected = Integer.parseInt(rowString);
        }

        public void onUnSelect(String value) {
            rowSelected = NO_ROW_SELECTED;
        }

        public int getRowSelected() {
            return rowSelected;
        }

    }


    protected void setSampleAuthors() {

        //List tableRows = new ArrayList();

        addNewAuthor("Rekha Seshadri");

        addNewAuthor("Saul Kravitz");

        addNewAuthor("Prateek Kumar");

        addNewAuthor("Guy Cao");

        addNewAuthor("William Jefferson Clinton");

        addNewAuthor("George W. Bush");

        //this.clear();
        //this.setData(tableRows);
        //this.refresh();
    }

    protected void addNewAuthor(String authorString) {


        //System.out.println(" trying to add new author() ");

        int newRowIndex = this.getRowCount();

        this.insertRow(newRowIndex);

        this.setValue(newRowIndex, AUTHOR_DELETE_COLUMN, null,
                createRemoveAuthorWidget(this, newRowIndex));

        this.setValue(newRowIndex, AUTHOR_NUM_COLUMN, new Integer(newRowIndex),
                new Label(Integer.toString(newRowIndex)));

        this.setValue(newRowIndex, AUTHOR_NAME_COLUMN, authorString,
                getNameWidget(authorString, newRowIndex));

        this.refresh();
    }


    private Widget getNameWidget(String author, int rowIndex) { //used to be TableRow row

        //String id1="node"+node.getDatabaseObjectId()+"Name";
        //String id2="node"+node.getDatabaseObjectId()+"EditActionLink";

        String id1 = "nodeName";
        String id2 = "nodeEditActionLink";

        HTMLPanel panel = new HTMLPanel(
                "<span id='" + id1 + "'></span>" +
                        "<span id='" + id2 + "'></span>"
        );

        Widget name = new FulltextPopperUpperHTML(author, NAME_MAX_SIZE);
        DOM.setStyleAttribute(name.getElement(), "display", "inline");

        Widget link = new ActionLink("edit", new EditAuthorNameHandler(rowIndex, author, this));
        DOM.setStyleAttribute(link.getElement(), "display", "inline");

        panel.add(name, id1);
        panel.add(link, id1);
        DOM.setStyleAttribute(panel.getElement(), "display", "inline");

        return panel;
    }


    /* copycat from: BlastWizardUserSequencePage->class:EditUserNodeNameHandler */
    private class EditAuthorNameHandler implements ClickListener, EditAuthorNameListener {

        int rowIndex;
        String authorName;
        AuthorTable parentTable;

        public EditAuthorNameHandler(int rowIndex, String authorName, AuthorTable parentTable) {
            this.rowIndex = rowIndex;
            this.authorName = authorName;
            this.parentTable = parentTable;
        }

        public void onClick(Widget widget) {
            new PopupBelowLauncher(new EditAuthorNamePopup(this, false, authorName)).showPopup(widget);
        }

        public void replaceUserNodeName(String newAuthorName) {
            parentTable.replaceAuthorName(rowIndex, newAuthorName);
        }
    }

    protected void replaceAuthorName(int rowIndex, String newAuthorName) {
        this.setValue(rowIndex, AUTHOR_NAME_COLUMN, newAuthorName,
                getNameWidget(newAuthorName, rowIndex));
        this.refresh();
    }


    /*
     * copycat from: jobresultspage.java
     * see also: BlastWizardUserSequencePage->class:EditUserNodeNameHandler
     */
    private Widget createRemoveAuthorWidget(AuthorTable parentTable, int rowIndex) { // TableRow row  (keep track which row is being deleted)

        ControlImageBundle imageBundle = ImageBundleFactory.getControlImageBundle();
        Image image = imageBundle.getDeleteImage().createImage();
        image.addMouseListener(new HoverImageSetter(image, imageBundle.getDeleteImage(), imageBundle.getDeleteHoverImage()));
        image.addClickListener(new RemoveAuhtorEventHandler(parentTable, rowIndex));
        return image;

    }

    /* copycat from: jobresultspage.java */
    private class RemoveAuhtorEventHandler implements ClickListener, PopupListener, RemoveAuthorListener {

        /*
        private JobInfo _jobStatus;
        private TableRow _row;
        */

        private int rowIndex;
        private RemoveAuthorPopup _removeAuthorDialog;
        private boolean _inProgress;
        private AuthorTable parentTable;

        public RemoveAuhtorEventHandler(AuthorTable parentTable, int rowIndex) {

            this.parentTable = parentTable;
            this.rowIndex = rowIndex;
            _inProgress = false;
            _removeAuthorDialog = null;

            /*
            _jobStatus = jobStatus;
            _row=row;
            */
        }

        public void onClick(Widget widget) {
            startJob(widget);
        }

        public void onPopupClosed(PopupPanel popupPanel, boolean b) {
//            _inProgress=false;
        }

        public void removeAuthor(Integer authorNumber) {

            parentTable.removeAuthorRow(authorNumber);


            /*
                        AsyncCallback removeAuthorCallback = new AsyncCallback() {

                            public void onFailure(Throwable caught) {
                                _logger.error("Remove author failed",caught);
                                finishedRemoveAuthor();
                            }

                            //  On success, populate the table with the DataNodes received
                            public void onSuccess(Object result) {
                                _pagingPanel.removeRow(_row);
                                _logger.debug("Remove job succeded");
                                SystemWebTracker.trackActivity("DeleteJob");
                                finishedRemoveAuthor();
                            }
                        };
            //          _statusservice.markTaskForDeletion(jobId,removeJobCallback);
            */


        }

        private void startJob(Widget widget) {
            if (!_inProgress) {
                _inProgress = true;
                //_removeJobDialog=new RemoveAuthorPopup(_jobStatus,this,false);

                _removeAuthorDialog = new RemoveAuthorPopup(
                        this, false, (String) parentTable.getValue(rowIndex, AUTHOR_NAME_COLUMN).getValue(),
                        (Integer) parentTable.getValue(rowIndex, AUTHOR_NUM_COLUMN).getValue());

                _removeAuthorDialog.addPopupListener(this);
                PopupBelowLauncher removeAuthorLauncher = new PopupBelowLauncher(_removeAuthorDialog);
                removeAuthorLauncher.showPopup(widget);
            }

        }

        private void finishedRemoveAuthor() {
            /*
            if(_removeJobDialog != null) {
                _removeJobDialog.hide();
            }
            _removeJobDialog=null;
            */
        }

    }

    protected void removeAuthorRow(Integer authorNumber) {

        // remove row
        this.removeRow(authorNumber.intValue());

        // shift all the row/author indecies
        for (int row = authorNumber.intValue(); row < this.getRowCount(); row++) {
            this.setValue(row, AUTHOR_NUM_COLUMN, new Integer(row), new Label(String.valueOf(row)));
        }

        this.refresh();

    }


}
