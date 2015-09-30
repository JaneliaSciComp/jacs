
package org.janelia.it.jacs.web.gwt.prokAnnot.client.panel;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.ui.*;
import org.janelia.it.jacs.model.prokPipeline.ProkGenomeVO;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.web.gwt.common.client.panel.TitledPanel;
import org.janelia.it.jacs.web.gwt.common.client.popup.ErrorPopupPanel;
import org.janelia.it.jacs.web.gwt.common.client.popup.launcher.PopupCenteredLauncher;
import org.janelia.it.jacs.web.gwt.common.client.service.DataService;
import org.janelia.it.jacs.web.gwt.common.client.service.DataServiceAsync;
import org.janelia.it.jacs.web.gwt.common.client.ui.SmallRoundedButton;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.comparables.FormattedDateTime;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Jul 17, 2009
 * Time: 9:13:58 AM
 */
public class ProkGenomeStatusPanel extends VerticalPanel {

    private String _targetGenome;
    private ProkGenomeVO _prokGenome;
    private HTML targetOutputDirectory = new HTML("", true);
    private TitledPanel eventPanel = new TitledPanel("Data Events", false);
    private FlexTable eventTable = new FlexTable();
    private static DataServiceAsync _dataservice = (DataServiceAsync) GWT.create(DataService.class);

    static {
        ((ServiceDefTarget) _dataservice).setServiceEntryPoint("data.srv");
    }

    public ProkGenomeStatusPanel(String targetGenome) {
        super();
        _targetGenome = targetGenome;
        init();
        getData();
    }

    private void init() {
        eventTable.setHTML(0, 0, HtmlUtils.getHtml("Timestamp", "prompt").toString());
        eventTable.setHTML(0, 1, HtmlUtils.getHtml("Type", "prompt").toString());
        eventTable.setHTML(0, 2, HtmlUtils.getHtml("Description", "prompt").toString());
        eventPanel.add(eventTable);
        eventPanel.setVisible(false);

        HorizontalPanel tmpPanel = new HorizontalPanel();
        tmpPanel.add(HtmlUtils.getHtml("Organism Output Directory:", "prompt"));
        tmpPanel.add(HtmlUtils.getHtml("&nbsp;", "text"));
        tmpPanel.add(targetOutputDirectory);

        SmallRoundedButton eventsButton = new SmallRoundedButton("Toggle Event History >");
        eventsButton.setWidth("140px");
        eventsButton.addClickListener(new MyClickListener());

        this.add(tmpPanel);
        this.add(eventsButton);
        this.add(eventPanel);

    }

    class MyClickListener implements ClickListener {
        public void onClick(Widget widget) {
            eventPanel.setVisible(!eventPanel.isVisible());
        }
    }

    private void getData() {
        if (null == _targetGenome || "".equals(_targetGenome)) {
            return;
        }
        _dataservice.getProkGenomeVO(_targetGenome, new AsyncCallback() {
            public void onFailure(Throwable throwable) {
                new PopupCenteredLauncher(new ErrorPopupPanel("Unable to access history data for genome: " + _targetGenome), 250).showPopup(null);
            }

            public void onSuccess(Object o) {
                _prokGenome = (ProkGenomeVO) o;
                //getOutputFiles(_prokGenome.getTargetOutputDirectory());
                displayPanel();
            }
        });
    }

    private void displayPanel() {
        targetOutputDirectory.setHTML(_prokGenome.getTargetOutputDirectory());
        targetOutputDirectory.setVisible(true);

        // Add the events into the table
        removeRows();
        for (Event event : _prokGenome.getEvents()) {
            addRow(event);
        }
    }

    public void setGenome(String targetGenome) {
        _targetGenome = targetGenome;
        getData();

    }

    /**
     * Add a row to the flex table.
     *
     * @param tmpEvent - the event to add to the table
     */
    private void addRow(Event tmpEvent) {
        int numRows = eventTable.getRowCount();
        eventTable.setWidget(numRows, 0, HtmlUtils.getHtml(new FormattedDateTime(tmpEvent.getTimestamp().getTime(), true).toString(), "nowrapText"));
        eventTable.setWidget(numRows, 1, HtmlUtils.getHtml(tmpEvent.getEventType(), "nowrapText"));
        eventTable.setWidget(numRows, 2, HtmlUtils.getHtml(tmpEvent.getDescription(), "text"));
    }

    /**
     * Remove a row from the flex table.
     */
    private void removeRows() {
        int numRows = eventTable.getRowCount();
        while (numRows > 1) {
            eventTable.removeRow(numRows - 1);
            numRows--;
        }
    }


}
