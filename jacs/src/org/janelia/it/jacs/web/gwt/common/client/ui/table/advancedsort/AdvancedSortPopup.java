
package org.janelia.it.jacs.web.gwt.common.client.ui.table.advancedsort;

import com.google.gwt.user.client.ui.*;
import org.janelia.it.jacs.web.gwt.common.client.popup.ModalPopupPanel;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.common.client.ui.ButtonSet;
import org.janelia.it.jacs.web.gwt.common.client.ui.RoundedButton;
import org.janelia.it.jacs.web.gwt.common.client.ui.Span;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.SortableColumn;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * @author Cristian Goina
 */
public class AdvancedSortPopup extends ModalPopupPanel {
    private static Logger _logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.common.client.ui.table.advancedsort.AdvancedSortPopup");

    private SortableColumn[] allSortableColumns;
    private SortableColumn[] sortColumns;
    private SortChoiceElement[] sortChoices;
    private org.janelia.it.jacs.web.gwt.common.client.ui.table.advancedsort.AdvancedSortListener sortListener;

    private class SortColumnsListBox extends ListBox {
        private int columnSortArgIndex;

        private SortColumnsListBox(boolean multiSelect, int columnSortArgIndex) {
            super(multiSelect);
            this.columnSortArgIndex = columnSortArgIndex;
        }

    }

    private class SortChoiceChangeListener implements ChangeListener, ClickListener {
        SortChoiceElement[] sortChoices;

        SortChoiceChangeListener(SortChoiceElement[] sortChoices) {
            this.sortChoices = sortChoices;
        }

        public void onChange(Widget widget) {
            SortChoiceElement choiceElement = null;
            if (widget instanceof SortColumnsListBox) {
                SortColumnsListBox sortArgElement = (SortColumnsListBox) widget;
                choiceElement = sortChoices[sortArgElement.columnSortArgIndex];
            }
            else {
                return;
            }
            if (choiceElement.currentChoiceIndex == choiceElement.sortChoicesBox.getSelectedIndex()) {
                // nothing changed
                return;
            }
            else {
                choiceElement.currentChoiceIndex = choiceElement.sortChoicesBox.getSelectedIndex();
            }
            if (choiceElement.currentChoiceIndex <= 0) {
                sortChoices[choiceElement.choiceElementIndex].disableDirectionChoice();
                for (int i = choiceElement.choiceElementIndex + 1; i < sortChoices.length; i++) {
                    sortChoices[i].disableControls();
                }
                // if no item was selected disable all following sort choices
            }
            else {
                // enable direction choice
                sortChoices[choiceElement.choiceElementIndex].enableDirectionChoice();
                // enable the next sort choice
                if (choiceElement.choiceElementIndex + 1 < sortChoices.length) {
                    sortChoices[choiceElement.choiceElementIndex + 1].enableColumnChoice();
                }
            }
        }

        public void onClick(Widget widget) {
            // for now there's nothing to do here
        }

    }

    private class SortChoiceElement {

        private SortableColumn sortColumn;
        private int choiceElementIndex;
        private int currentChoiceIndex;
        private SortChoiceChangeListener choiceChangeListener;
        private SortColumnsListBox sortChoicesBox;
        private RadioButton sortAscRB;
        private RadioButton sortDescRB;

        SortChoiceElement(SortableColumn sortColumn, Grid underlyingGrid, int choiceElementIndex, SortChoiceChangeListener choiceChangeListener) {
            this.sortColumn = sortColumn;
            this.choiceElementIndex = choiceElementIndex;
            this.choiceChangeListener = choiceChangeListener;
            populateContent(underlyingGrid);
        }

        void disableControls() {
            disableColumnChoice();
            disableDirectionChoice();
        }

        void disableColumnChoice() {
            sortChoicesBox.setSelectedIndex(0); // reset the selection
            sortChoicesBox.setEnabled(false);
        }

        void disableDirectionChoice() {
            sortAscRB.setEnabled(false);
            sortAscRB.setValue(false);
            sortDescRB.setEnabled(false);
            sortDescRB.setValue(false);
        }

        void enableColumnChoice() {
            sortChoicesBox.setEnabled(true);
        }

        void enableDirectionChoice() {
            sortAscRB.setEnabled(true);
            sortAscRB.setValue(true);
            sortDescRB.setEnabled(true);
            sortDescRB.setValue(false);
        }

        SortableColumn getCurrentSortColumn() {
            SortableColumn currentSortableColumn = null;
            if (currentChoiceIndex > 0) {
                // if currentChoiceIndex is > 0 the selection is guaranteed not to be empty 
                String columnHeading = sortChoicesBox.getItemText(currentChoiceIndex);
                String columnName = sortChoicesBox.getValue(currentChoiceIndex);
                int columnPosition = -1;
                // this is a bit inefficient to scan all columns to find the position
                for (int i = 0; i < allSortableColumns.length; i++) {
                    if (columnHeading != null && columnHeading.equals(allSortableColumns[i].getColumnHeading())) {
                        columnPosition = allSortableColumns[i].getColumnPosition();
                        break;
                    }
                    else if (columnName != null && columnName.equals(allSortableColumns[i].getSortArgumentName())) {
                        columnPosition = allSortableColumns[i].getColumnPosition();
                        break;
                    }
                }
                if (columnPosition >= 0) {
                    int sortDirection = SortableColumn.SORT_NOTSET;
                    if (sortAscRB.getValue()) {
                        sortDirection = SortableColumn.SORT_ASC;
                    }
                    else if (sortDescRB.getValue()) {
                        sortDirection = SortableColumn.SORT_DESC;
                    }
                    currentSortableColumn = new SortableColumn(columnPosition, columnHeading, columnName, sortDirection);
                }
                else {
                    _logger.warn("Invalid position or no column found for " +
                            "(" + columnName + "," + columnHeading + ")");
                }
            }
            return currentSortableColumn;
        }

        private void populateContent(Grid underlyingGrid) {
            currentChoiceIndex = -1;
            sortChoicesBox = new SortColumnsListBox(false, choiceElementIndex);
            sortChoicesBox.addItem("", ""); // first item will always be empty for the case when no sorting is desired
            // we use a different index for setting the current selection because
            // allsortablecolumns may contain empty strings and then the index in the allsortablecolumns and
            // the index of the choicebox may no longer be in sync
            int choiceIndex = 0;
            for (int i = 0; i < allSortableColumns.length; i++) {
                String currentSortColHeading = allSortableColumns[i].getColumnHeading();
                String currentSortColName = allSortableColumns[i].getSortArgumentName();
                if ((currentSortColHeading == null || currentSortColHeading.trim().length() == 0) &&
                        (currentSortColName == null || currentSortColName.trim().length() == 0)) {
                    // ignore empty columns
                    continue;
                }
                else if (currentSortColHeading != null && currentSortColHeading.trim().length() > 0) {
                    if (currentSortColName != null && currentSortColName.trim().length() > 0) {
                        sortChoicesBox.addItem(currentSortColHeading, currentSortColName);
                    }
                    else {
                        sortChoicesBox.addItem(currentSortColHeading);
                    }
                }
                else {
                    // invalid colHeading but currentSortColName is valid
                    sortChoicesBox.addItem(currentSortColName, currentSortColName);
                }
                choiceIndex++;
                if (sortColumn != null) {
                    if (allSortableColumns[i].getColumnPosition() == sortColumn.getColumnPosition()) {
                        currentChoiceIndex = choiceIndex;
                    }
                }
            }
            Span ascLabel = new Span("&nbsp;Ascending&nbsp;", "text");
            sortAscRB = new RadioButton("sortDirGroup" + String.valueOf(choiceElementIndex), ascLabel.toString(), true);
            sortAscRB.setStyleName("advancedSortDirectionRadioButton");
            Span descLabel = new Span("&nbsp;Descending&nbsp;", "text");
            sortDescRB = new RadioButton("sortDirGroup" + String.valueOf(choiceElementIndex), descLabel.toString(), true);
            sortDescRB.setStyleName("advancedSortDirectionRadioButton");
            sortChoicesBox.setVisibleItemCount(1);
            sortChoicesBox.addChangeListener(choiceChangeListener);
            String prompt;
            if (choiceElementIndex == 0) {
                prompt = "Sort By:";
            }
            else {
                prompt = "Then By:";
            }
            underlyingGrid.setWidget(choiceElementIndex, 0, HtmlUtils.getHtml(prompt, "prompt"));
            underlyingGrid.setWidget(choiceElementIndex, 1, sortChoicesBox);
            underlyingGrid.setWidget(choiceElementIndex, 2, sortAscRB);
            underlyingGrid.setWidget(choiceElementIndex, 3, sortDescRB);
            if (currentChoiceIndex >= 0) {
                if (sortColumn.getSortDirection() == SortableColumn.SORT_DESC) {
                    sortDescRB.setValue(true);
                }
                else {
                    sortAscRB.setValue(true);
                }
                sortChoicesBox.setSelectedIndex(currentChoiceIndex);
            }
            else {
                disableControls();
            }
        }

    }

    public AdvancedSortPopup(SortableColumn[] allSortableColumns,
                             SortableColumn[] sortColumns,
                             org.janelia.it.jacs.web.gwt.common.client.ui.table.advancedsort.AdvancedSortListener sortListener,
                             boolean realizeNow) {
        super("Advanced Sort", realizeNow);
        this.allSortableColumns = allSortableColumns;
        this.sortColumns = sortColumns;
        this.sortListener = sortListener;
    }

    protected ButtonSet createButtons() {
        RoundedButton[] tmpButtons = new RoundedButton[2];
        tmpButtons[0] = new RoundedButton("Sort", new ClickListener() {
            public void onClick(Widget widget) {
                submit();
            }
        });
        tmpButtons[1] = new RoundedButton("Cancel", new ClickListener() {
            public void onClick(Widget widget) {
                hide();
            }
        });
        return new ButtonSet(tmpButtons);
    }

    /**
     * For subclasses to supply dialog content
     */
    protected void populateContent() {
        Grid sortChoiceGrid = new Grid(3, 4);
        sortChoiceGrid.setCellPadding(2);
        sortChoiceGrid.setCellSpacing(2);
        // create and populate the choices
        sortChoices = new SortChoiceElement[3];
        SortChoiceChangeListener choiceChangeListener = new SortChoiceChangeListener(sortChoices);
        for (int i = 0; i < sortChoices.length; i++) {
            SortableColumn currentSortCol = null;
            if (sortColumns != null && i < sortColumns.length) {
                currentSortCol = sortColumns[i];
            }
            sortChoices[i] = new SortChoiceElement(currentSortCol, sortChoiceGrid, i, choiceChangeListener);
            if (currentSortCol == null ||
                    currentSortCol.getSortArgumentName() == null ||
                    currentSortCol.getSortArgumentName().length() == 0) {
                // if no sort column is set for the current element but this is the first choice or
                // the previous choice has a column set
                // then enable the column choice for the current element
                if (i > 0) {
                    if (sortChoices[i - 1].sortColumn != null) {
                        sortChoices[i].enableColumnChoice();
                    }
                }
                else {
                    sortChoices[i].enableColumnChoice();
                }
            }
        }
        add(sortChoiceGrid);
        add(HtmlUtils.getHtml("&nbsp;", "text"));
    }

    private void
    submit() {
        SortableColumn[] currentSortChoices = getCurrentSortChoices();
        if (compareSortChoices(sortColumns, currentSortChoices) != 0) {
            // if there have been any changes in sorting options notify the sort listener
            if (_logger.isDebugEnabled()) {
                _logger.debug("Selected sort options: " + Arrays.asList(currentSortChoices).toString());
            }
            sortListener.sortBy(currentSortChoices);
        }
        hide();
    }

    private SortableColumn[] getCurrentSortChoices() {
        ArrayList currentSortChoiceList = new ArrayList();
        for (int i = 0; i < sortChoices.length; i++) {
            SortableColumn sortColumn = sortChoices[i].getCurrentSortColumn();
            if (sortColumn != null) {
                currentSortChoiceList.add(sortColumn);
            }
            else {
                break;
            }
        }
        SortableColumn[] currentSortChoices = new SortableColumn[currentSortChoiceList.size()];
        for (int i = 0; i < currentSortChoices.length; i++) {
            currentSortChoices[i] = (SortableColumn) currentSortChoiceList.get(i);
        }
        return currentSortChoices;
    }

    private int compareSortChoices(SortableColumn[] sct1, SortableColumn[] sct2) {
        int result = 0;
        int sct1Length = sct1 != null ? sct1.length : 0;
        int sct2Length = sct2 != null ? sct2.length : 0;
        for (int i = 0; ; i++) {
            if (i < sct1Length && i < sct2Length) {
                if (sct1[i].getColumnPosition() != sct2[i].getColumnPosition()) {
                    result = sct1[i].getColumnPosition() - sct2[i].getColumnPosition();
                    break;
                }
                else if (sct1[i].getSortDirection() != sct2[i].getSortDirection()) {
                    result = sct1[i].getSortDirection() - sct2[i].getSortDirection();
                    break;
                }
            }
            else if (i < sct1Length) {
                result = -1;
                break;
            }
            else if (i < sct2Length) {
                result = 1;
                break;
            }
            else {
                // both lists reached the end at the same time
                break;
            }
        }
        return result;
    }

}
