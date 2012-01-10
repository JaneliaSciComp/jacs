
package org.janelia.it.jacs.web.gwt.blast.client.panel;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.ui.*;
import org.janelia.it.jacs.model.common.BlastableNodeVO;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.web.gwt.blast.client.BlastService;
import org.janelia.it.jacs.web.gwt.blast.client.BlastServiceAsync;
import org.janelia.it.jacs.web.gwt.blast.client.popup.BlastableNodeInfoPopup;
import org.janelia.it.jacs.web.gwt.common.client.SystemWebTracker;
import org.janelia.it.jacs.web.gwt.common.client.core.BrowserDetector;
import org.janelia.it.jacs.web.gwt.common.client.jobs.JobSubmissionListener;
import org.janelia.it.jacs.web.gwt.common.client.panel.RoundedTabPanel;
import org.janelia.it.jacs.web.gwt.common.client.panel.user.RemoveNodeListener;
import org.janelia.it.jacs.web.gwt.common.client.panel.user.RemoveNodePopup;
import org.janelia.it.jacs.web.gwt.common.client.popup.BasePopupPanel;
import org.janelia.it.jacs.web.gwt.common.client.popup.ErrorPopupPanel;
import org.janelia.it.jacs.web.gwt.common.client.popup.launcher.PopupAboveLauncher;
import org.janelia.it.jacs.web.gwt.common.client.popup.launcher.PopupCenteredLauncher;
import org.janelia.it.jacs.web.gwt.common.client.popup.launcher.PopupLauncher;
import org.janelia.it.jacs.web.gwt.common.client.popup.launcher.PopupOffscreenLauncher;
import org.janelia.it.jacs.web.gwt.common.client.service.DataService;
import org.janelia.it.jacs.web.gwt.common.client.service.DataServiceAsync;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.common.client.ui.DoubleClickSelectionListener;
import org.janelia.it.jacs.web.gwt.common.client.ui.HoverImageSetter;
import org.janelia.it.jacs.web.gwt.common.client.ui.RoundedButton;
import org.janelia.it.jacs.web.gwt.common.client.ui.SelectionListener;
import org.janelia.it.jacs.web.gwt.common.client.ui.imagebundles.ControlImageBundle;
import org.janelia.it.jacs.web.gwt.common.client.ui.imagebundles.ImageBundleFactory;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.InfoActionLink;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.SortableColumn;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.SortableTable;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.TablePopulateListener;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.columns.ImageColumn;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.columns.NumericColumn;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.columns.TextColumn;
import org.janelia.it.jacs.web.gwt.common.client.util.BlastData;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;
import org.janelia.it.jacs.web.gwt.common.client.util.NumberUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * @author Michael Press
 */
public class BlastableNodePanel extends Composite {
    private static Logger _logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.blast.client.wizard.BlastWizardSubjectSequencePage");

    private HashMap<String, BlastableNodeVO> _blastDatasetNodeMap;  // Large cache.  todo consider re-query instead.
    private List<BlastableNodeVO> _blastDatasetNodes = new ArrayList<BlastableNodeVO>();
    private String[] setNames = new String[]{PUBLIC_SET, PRIVATE_SET, ALL_SET/*, POPULAR_SET*/};
    private HashMap<String, SortableTable> tableMap = new HashMap<String, SortableTable>();
    private RoundedTabPanel setPanel = new RoundedTabPanel();
    private BlastData _blastData = new BlastData();
    private SelectionListener _selectionListener;
    private DoubleClickSelectionListener _doubleClickSelectionListener;

    public static final String BLAST_SUBJECT_SEQUENCE_HELP_LINK_PROP = "BlastSubject.HelpURL";
    //    public static final String POPULAR_SET   = "Popular Datasets";
    public static final String PUBLIC_SET   = "Public Datasets";
    public static final String PRIVATE_SET  = "Private Datasets";
    public static final String ALL_SET      = "All Datasets";

    private static final int COLUMN_ID = 0;
    private static final int COLUMN_DELETE = 1;
    private static final int COLUMN_ORDER = 2;
    private static final int COLUMN_NAME = 3;
    private static final int COLUMN_LENGTH = 4;
    private static final int COLUMN_SEQUENCE_COUNT = 5;
    private static final int COLUMN_SEQUENCE_TYPE = 6;

    private static final String COLUMN_ID_HEADING = "Id";
    private static final String COLUMN_ORDER_HEADING = "&nbsp;";
    private static final String COLUMN_NAME_HEADING = "Dataset";
    private static final String COLUMN_LENGTH_HEADING = "Length";
    private static final String COLUMN_SEQUENCE_COUNT_HEADING = "# Sequences";
    private static final String COLUMN_SEQUENCE_TYPE_HEADING = "Sequence Type";

    public static final int DESCRIPTION_MAX_SIZE = 100;
    private AddBlastDatabasePanel dbPanel;

    private String _seqType;
    private static BlastServiceAsync _blastservice = (BlastServiceAsync) GWT.create(BlastService.class);
    private static DataServiceAsync _dataservice = (DataServiceAsync) GWT.create(DataService.class);

    static {
        ((ServiceDefTarget) _blastservice).setServiceEntryPoint("blast.srv");
        ((ServiceDefTarget) _dataservice).setServiceEntryPoint("data.srv");
    }

    public BlastableNodePanel(String seqType, SelectionListener selectionListener, DoubleClickSelectionListener doubleClickSelectionListener,
                              BlastData blastData) {
        dbPanel = new AddBlastDatabasePanel(new DatabaseCreationListener());
        _seqType = seqType;
        _selectionListener = selectionListener;
        _doubleClickSelectionListener = doubleClickSelectionListener;
        _blastDatasetNodeMap = new HashMap<String, BlastableNodeVO>();
        setBlastData(blastData);
        init();
    }

    private void init() {
        VerticalPanel mainPanel = new VerticalPanel();
        mainPanel.setWidth("100%");
        dbPanel.setVisible(false);
        // Add the tables to the tab panel
        for (String setName : setNames) {
            establishTab(setName);
        }
        setPanel.setWidth("100%");
        setPanel.addTabListener(new TabListener() {
            public boolean onBeforeTabSelected(com.google.gwt.user.client.ui.SourcesTabEvents sourcesTabEvents, int index) {
                if ((tableMap.get(setNames[index])).getNumDataRows() == 0) {
                    (tableMap.get(setNames[index])).setWidth("100%");
                    (tableMap.get(setNames[index])).setLoading();
                }
                return true;
            }

            public void onTabSelected(com.google.gwt.user.client.ui.SourcesTabEvents sourcesTabEvents, final int index) {
                DeferredCommand.addCommand(new Command() {
                    public void execute() {
                        if ((tableMap.get(setNames[index])).getNumDataRows() == 0)
                            populateAndSetupTable(setNames[index]);
                    }
                });
            }

        });
        setPanel.selectTab(0);

        mainPanel.add(new HTML("<span class='TitleBoxSectionSpacer'>&nbsp;</span>"));
        mainPanel.add(setPanel);
        mainPanel.add(HtmlUtils.getHtml("&nbsp;", "smallSpacer"));
        mainPanel.add(HtmlUtils.getHtml("&bull;&nbsp;Click once to select.&nbsp;&nbsp;&bull;&nbsp;Double click to select and apply.", "hint"));

        initWidget(mainPanel); // required for Composite
    }

    private void establishTab(final String newTabName) {
        SortableTable tmpTable = formatTable(newTabName);
        ScrollPanel tmpPanel = formatPanel(tmpTable);
        VerticalPanel finalPanel = new VerticalPanel();
        finalPanel.add(tmpPanel);
        // Allow the users to add their own blast databases
        if (PRIVATE_SET.equals(newTabName)) {
            RoundedButton newBlastDatabaseButton = new RoundedButton("Add Blast Database", new ClickListener() {
                public void onClick(Widget widget) {
                    dbPanel.setVisible(!dbPanel.isVisible());
                }
            });
            newBlastDatabaseButton.setWidth("130px");
            finalPanel.add(newBlastDatabaseButton);
            finalPanel.add(dbPanel);
        }
        finalPanel.setWidth("100%");
        finalPanel.setHeight("100%");
        setPanel.add(finalPanel, newTabName);
    }

    private SortableTable formatTable(String newTabName) {
        _logger.debug("formatting table " + newTabName);
        ImageColumn deleteColumn = new ImageColumn("&nbsp;", false, (PRIVATE_SET.equals(newTabName)));
        SortableTable tmpTable = new SortableTable();
        tmpTable.addColumn(new TextColumn(COLUMN_ID_HEADING, /*sortable*/false, /*visible*/ false));
        tmpTable.addColumn(deleteColumn);                       // node delete icon
        tmpTable.addColumn(new NumericColumn(COLUMN_ORDER_HEADING, /*no popup*/"", /*sortable*/ false, /*visible*/ false));
        tmpTable.addColumn(new TextColumn(COLUMN_NAME_HEADING));
        tmpTable.addColumn(new NumericColumn(COLUMN_LENGTH_HEADING));
        tmpTable.addColumn(new NumericColumn(COLUMN_SEQUENCE_COUNT_HEADING));
        tmpTable.addColumn(new TextColumn(COLUMN_SEQUENCE_TYPE_HEADING));
        tmpTable.setHighlightSelect(true);
        tmpTable.setWidth("100%");
        tmpTable.setDefaultSortColumns(new SortableColumn[]{
                new SortableColumn(COLUMN_ORDER, COLUMN_ORDER_HEADING, SortableColumn.SORT_ASC)
        });
        tmpTable.addSelectionListener(new MyTableListener(tmpTable));
        tmpTable.addDoubleClickSelectionListener(new MyDoubleClickListener(tmpTable));

        // IE won't resize the popup after the data loads until there's a browser event, so force one with an offscreen popup
        if (BrowserDetector.isIE())
            tmpTable.addPopulateListener(new TablePopulateListener() {
                public void onBusy(Widget widget) {
                }

                public void onBusyDone(Widget widget) {
                    new PopupOffscreenLauncher(new PopupPanel()).showPopup(null);
                }
            });

        tmpTable.setWidth("100%");
        tmpTable.setLoading();
        tableMap.put(newTabName, tmpTable);

        return tmpTable;
    }

    private ScrollPanel formatPanel(SortableTable tmpTable) {
        ScrollPanel tmpPanel = new ScrollPanel(tmpTable);
        tmpPanel.setWidth("100%");
        tmpPanel.setHeight("350px");
        return tmpPanel;
    }

    public void realize() {
        DeferredCommand.addCommand(new Command() {
            public void execute() {
                realizeImpl();
            }
        });
    }

    public void realize(final int tabIndex) {
        DeferredCommand.addCommand(new Command() {
            public void execute() {
                realizeImpl(tabIndex);
            }
        });
    }

    private void realizeImpl() {
        realizeImpl(0);
    }

    private void realizeImpl(final int tabIndex) {
        // If the data model is empty, go get the data
        if (null == _blastDatasetNodes || _blastDatasetNodes.size() == 0) {
            _blastservice.getBlastableSubjectSets(_seqType, new AsyncCallback() {
                public void onFailure(Throwable caught) {
                    ErrorPopupPanel popup = new ErrorPopupPanel("Error retrieving datasets.  Please reload this page and try again.");
                    new PopupCenteredLauncher(popup, 250).showPopup(null);
                    _logger.error("setService.onFailure()");
                }

                //  On success, populate the table with the DataNodes received.
                //  Also, load any required metadata into appropriate map.
                public void onSuccess(Object result) {
                    BlastableNodeVO[] resultNodes = (BlastableNodeVO[]) result;
                    for (BlastableNodeVO resultNode : resultNodes) {
                        _blastDatasetNodeMap.put(resultNode.getDatabaseObjectId(), resultNode);
                        _blastDatasetNodes.add(resultNode);
                    }
                    setPanel.selectTab(tabIndex);
                }
            });
        }
    }

    private void populateAndSetupTable(String setName) {
        for (BlastableNodeVO _blastDatasetNode : _blastDatasetNodes) {
            addDataNodeBySetName(_blastDatasetNode, setName);
        }
        // check the tables have values
        for (SortableTable sortableTable : tableMap.values()) {
            if (0 == sortableTable.getNumDataRows()) {
                sortableTable.setNoData("There are no datasets currently defined.");
            }
        }
        setupTableBySetName(setName);
    }

    private void setupTableBySetName(String setName) {
        tableMap.get(setName).refresh();
    }

    /**
     * Add a single data node to the sequence list.
     *
     * @param node    to add.
     * @param setName to change
     */
    private void addDataNodeBySetName(BlastableNodeVO node, String setName) {
        try {
            if (node == null) {
                _logger.error("Null node passed in.");
                return;
            }
            if (null != node.getNodeName()) {
                // Add data to the proper tables
                if (setName.equals(PUBLIC_SET)) {
                    if (Node.VISIBILITY_PUBLIC.equals(node.getVisibility())) {
                        addRowToTable(PUBLIC_SET, node);
                    }
                }
                else if (setName.equals(PRIVATE_SET)) {
                    if (Node.VISIBILITY_PRIVATE.equals(node.getVisibility())) {
                        addRowToTable(PRIVATE_SET, node);
                    }
                }
//                else if (setName.equals(POPULAR_SET)) {
//                    // do nothing
//                }
                else if (setName.equals(ALL_SET)) {
                    addRowToTable(ALL_SET, node);
                }
                else {
                    _logger.error("Do not recognize setName=" + setName);
                }
            }
            else {
                _logger.error("Could not add node=" + node.getDatabaseObjectId() + " no node name.");
            }
        }
        catch (Throwable e) {
            _logger.error("Error adding a node to the sub seq list.\n" + e.getMessage());
        }

    }

    private void addRowToTable(String targetTableName, BlastableNodeVO node) {
        SortableTable tmpTable = tableMap.get(targetTableName);

        int row = tmpTable.getNumDataRows() + 1;
        tmpTable.setValue(row, COLUMN_ID, node.getDatabaseObjectId());
        tmpTable.setValue(row, COLUMN_DELETE, "", createRemoveNodeWidget(node));
        tmpTable.setValue(row, COLUMN_ORDER, node.getOrder());
        tmpTable.setValue(row, COLUMN_NAME, node.getNodeName(), getNameWidget(node));
        tmpTable.setValue(row, COLUMN_LENGTH, getFormattedLongAsComparable(node.getLength()));
        tmpTable.setValue(row, COLUMN_SEQUENCE_COUNT, getFormattedLongAsComparable(node.getSequenceCount()));
        tmpTable.setValue(row, COLUMN_SEQUENCE_TYPE, node.getSequenceType());
    }

    /**
     * Creates the node remove widget
     * @param nodeId
     * @return returns the node remove widget
     */
    private Widget createRemoveNodeWidget(BlastableNodeVO nodeId) {
        ControlImageBundle imageBundle = ImageBundleFactory.getControlImageBundle();
        Image image = imageBundle.getDeleteImage().createImage();
        image.addMouseListener(new HoverImageSetter(image, imageBundle.getDeleteImage(), imageBundle.getDeleteHoverImage()));
        image.addClickListener(new RemoveNodeEventHandler(nodeId));
        return image;
    }

    private Widget getNameWidget(BlastableNodeVO node) {
        HorizontalPanel panel = new HorizontalPanel();

        InfoActionLink infoLink = new InfoActionLink("");
        BasePopupPanel popup = new BlastableNodeInfoPopup("Reference Dataset Details", node, /*realizeNow*/ false);
        infoLink.setPopup(popup);
        panel.add(infoLink);

        panel.add(HtmlUtils.getHtml("&nbsp;", "smallSpacer"));
        panel.add(HtmlUtils.getHtml(node.getNodeName(), "text", "nowrap"));

        return panel;
    }

    private class DatabaseCreationListener implements JobSubmissionListener {
        public void onFailure(Throwable caught) {
        }

        public void onSuccess(String jobId) {
            clearAllTableData();
            clearBlastDatasetNodes();
            int privateTabIndex = getTabIndexForSetName(BlastableNodePanel.PRIVATE_SET);
            realize(privateTabIndex);
        }
    }

    private class RemoveNodeEventHandler implements ClickListener, RemoveNodeListener, PopupListener {
        private BlastableNodeVO node;
        boolean inProgress;
        RemoveNodePopup popup;


        public RemoveNodeEventHandler(BlastableNodeVO nodeId) {
            this.node = nodeId;
            inProgress=false;
            popup=null;
        }

        public void onClick(Widget widget) {
            startPopup(widget);
        }

        public void onPopupClosed(PopupPanel popupPanel, boolean b) {
            inProgress=false;
        }

        public void removeNode(final String nodeId) {
            AsyncCallback removeUserCallback = new AsyncCallback() {

                public void onFailure(Throwable caught) {
                    _logger.error("Remove node" + nodeId + " failed", caught);
                    finishedPopup();
                }

                public void onSuccess(Object result) {
                    _logger.debug("Remove node succeded");
                    SystemWebTracker.trackActivity("DeleteDatabaseNode");
                    finishedPopup();
                    // Clear any hover attribute
                    for (Object o : tableMap.keySet()) {
                        String key = (String) o;
                        SortableTable sortableTable = tableMap.get(key);
                        sortableTable.clearHover();
                        sortableTable.refresh();
                    }

                }

            };
            _dataservice.deleteNode(nodeId, removeUserCallback);
        }

        private void startPopup(Widget widget)
        {
            if(!inProgress) {
                inProgress=true;
                popup=new RemoveNodePopup(node.getNodeName(), node.getDatabaseObjectId(),this,false);
                popup.addPopupListener(this);
                PopupLauncher launcher=new PopupAboveLauncher(popup);
                launcher.showPopup(widget);
            }
        }

        private void finishedPopup()
        {
            if(popup != null) {
                popup.hide();
            }
            popup=null;
        }

    }


    public BlastableNodeVO getBlastNodeForId(String nodeId){
        if (null!=nodeId && _blastDatasetNodeMap.containsKey(nodeId)){
            return _blastDatasetNodeMap.get(nodeId);
        }
        return null;
    }

    public class MyTableListener implements SelectionListener {
        private SortableTable targetTable;

        public MyTableListener(SortableTable targetTable) {
            this.targetTable = targetTable;
        }

        public void onSelect(String rowString) {
            int row = Integer.valueOf(rowString);
            String targetId = targetTable.getText(row, COLUMN_ID);
            if (null == targetId || "".equals(targetId) || targetTable.getText(1, 0).indexOf("No Data") > 0) {
                return;
            }
            _logger.debug("User selected subject sequence " + targetId);
            // Clear the old values and put the node in the set of selected subjects
            _blastData.getSubjectSequenceDataNodeMap().clear();
            _blastData.setMostRecentlySelectedSubjectSequenceType(targetTable.getText(row, COLUMN_SEQUENCE_TYPE));
            _blastData.getSubjectSequenceDataNodeMap().put(targetId, _blastDatasetNodeMap.get(targetId));
            notifySelectionListenerOnSelect(targetId);

            // Clear any hover attribute
            for (Object o : tableMap.values()) {
                SortableTable sortableTable = (SortableTable) o;
                sortableTable.clearHover();
            }
        }

        public void onUnSelect(String value) {
            notifySelectionListenerOnUnSelect(value);
        }
    }

    public class MyDoubleClickListener implements DoubleClickSelectionListener {
        private SortableTable targetTable;

        public MyDoubleClickListener(SortableTable targetTable) {
            this.targetTable = targetTable;
        }

        public void onSelect(String rowString) {
            int row = Integer.valueOf(rowString);
            String targetId = targetTable.getText(row, COLUMN_ID);
            if (null == targetId || "".equals(targetId) || targetTable.getText(1, 0).indexOf("No Data") > 0)
                return;
            _logger.debug("User double-clicked subject sequence " + targetId);

            // Clear the old values &&  put the node in the set of selected subjects
            _blastData.getSubjectSequenceDataNodeMap().clear();
            _blastData.setMostRecentlySelectedSubjectSequenceType(targetTable.getText(row, COLUMN_SEQUENCE_TYPE));
            _blastData.getSubjectSequenceDataNodeMap().put(targetId, _blastDatasetNodeMap.get(targetId));
            notifySelectionListenerOnDoubleClickSelect(targetId);

            // Clear any hover attribute
            for (Object o : tableMap.keySet()) {
                String key = (String) o;
                SortableTable sortableTable = tableMap.get(key);
                sortableTable.clearHover();
            }
        }

    }

    private void notifySelectionListenerOnSelect(String nodeId) {
        if (_selectionListener != null)
            _selectionListener.onSelect(nodeId);
    }

    private void notifySelectionListenerOnUnSelect(String nodeId) {
        if (_selectionListener != null)
            _selectionListener.onUnSelect(nodeId);
    }

    private void notifySelectionListenerOnDoubleClickSelect(String nodeId) {
        if (_doubleClickSelectionListener != null)
            _doubleClickSelectionListener.onSelect(nodeId);
    }


    private Comparable getFormattedLongAsComparable(final String value) {
        return new Comparable() {
            Long _longValue = (value == null || value.trim().equals("") ? 0 : new Long(value));
            String _value = NumberUtils.formatLong(_longValue);

            public int compareTo(Object o) {
                String[] sArr = o.toString().split(",");
                StringBuffer sb = new StringBuffer("");
                for (String aSArr : sArr) {
                    sb.append(aSArr);
                }
                Long l2 = new Long(sb.toString());
                return _longValue.compareTo(l2);
            }

            public String toString() {
                return _value;
            }
        };
    }

    public void setBlastData(BlastData blastData) {
        _blastData = blastData;
    }

    public void clear() {
        for (SortableTable table : tableMap.values())
            table.clearSelect();
    }

    public void clearAllTableData() {
        for (SortableTable table : tableMap.values()) {
            table.clearData();
        }
    }

    public void clearBlastDatasetNodes() {
        _blastDatasetNodes.clear();
    }

    public int getTabIndexForSetName(String name) {
        for (int i = 0; i < setNames.length; i++) {
            String n = setNames[i];
            if (n.equals(name))
                return i;
        }
        return 0; // cannot match, so return default
    }

}

