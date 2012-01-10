
package org.janelia.it.jacs.web.gwt.blast.client.panel;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.ui.*;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.web.gwt.blast.client.BlastService;
import org.janelia.it.jacs.web.gwt.blast.client.BlastServiceAsync;
import org.janelia.it.jacs.web.gwt.common.client.popup.ErrorPopupPanel;
import org.janelia.it.jacs.web.gwt.common.client.popup.launcher.PopupCenteredLauncher;
import org.janelia.it.jacs.web.gwt.common.client.service.DataService;
import org.janelia.it.jacs.web.gwt.common.client.service.DataServiceAsync;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.common.client.util.BlastData;

/**
 * Refactored from BlastSubmitJobPage into reusable standalone class
 */
public class BlastOptionsPanel extends Composite {
    protected static Logger _logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.blast.client.wizard.BlastWizardSubmitJobPage");

    private VerticalPanel _mainPanel;
    protected BlastTaskOptionsPanel _taskOptionsPanel;
    protected BlastData _blastData;

    protected ListBox listBox;

    protected static DataServiceAsync _dataservice = (DataServiceAsync) GWT.create(DataService.class);
    protected static BlastServiceAsync _blastservice = (BlastServiceAsync) GWT.create(BlastService.class);

    public static final String BLAST_OPTIONS_LINK_HELP_PROP = "BlastOptions.HelpURL";

    static {
        ((ServiceDefTarget) _dataservice).setServiceEntryPoint("data.srv");
        ((ServiceDefTarget) _blastservice).setServiceEntryPoint("blast.srv");
    }

    public BlastOptionsPanel(BlastData blastData) {
        setBlastData(blastData);
        init();
    }

    private void init() {
        _mainPanel = new VerticalPanel();
        createProgramPanel();

        initWidget(_mainPanel);
    }

    private void createProgramPanel() {
        // list box of programs
        listBox = new ListBox();
        listBox.addChangeListener(new ProgramSelectedListener());
        _taskOptionsPanel = new BlastTaskOptionsPanel();
        _mainPanel.add(_taskOptionsPanel);
    }

    public void updateBlastPrograms(String querySequenceType, String subjectSequenceType, final AsyncCallback asyncCallback) {
        _logger.debug("UpdateBlastPrograms got " + querySequenceType + "/" + subjectSequenceType);
        _blastservice.getBlastPrograms(querySequenceType, subjectSequenceType, new AsyncCallback() {
            public void onFailure(Throwable caught) {
                _logger.error("getBlastPrograms setService.onFailure()", caught);
                ErrorPopupPanel popup = new ErrorPopupPanel("Could not retrieve the list of sequence analysis programs.");
                new PopupCenteredLauncher(popup, 250).showPopup(listBox);
                asyncCallback.onFailure(caught);
            }

            // On success, populate the table with the DataNodes received
            public void onSuccess(Object result) {
                asyncCallback.onSuccess(null); // call the listener BEFORE the list is populated
                _logger.debug("getBlastPrograms successfully returned: " + ((Task[]) result).length + " tasks");

                // Keep the list around a little while
                Task[] data = (Task[]) result;
                _blastData.setTaskArray(data);

                // Update the list with the display name of these programs
                _logger.debug("Adding the programs to the list box");
                for (Task task : data)
                    listBox.addItem(task.getDisplayName());

                // Set the correct initial value
                if (prePopulated()) {
                    for (int i = 0; i < data.length; i++) {
                        if (data[i].getDisplayName().equals(_blastData.getBlastTask().getDisplayName())) {
                            _logger.debug("Setting the selection to " + i);
                            listBox.setSelectedIndex(i);
                            break;
                        }
                    } // end of for
                    // _blastData.setBlastTask((BlastTask)_blastData.getTaskArray()[listBox.getSelectedIndex()]);
                    _blastData.setBlastTask(data[listBox.getSelectedIndex()]);
                }
                else {
                    _logger.debug("Setting the selection to 0");
                    listBox.setSelectedIndex(0);
                    _blastData.setBlastTask(data[0]);
                }
                _taskOptionsPanel.displayParams(_blastData.getBlastTask());
            }
        });
    }

    public boolean prePopulated() {
        return _blastData.getTaskIdFromParam() != null &&
                !_blastData.getTaskIdFromParam().equals("") &&
                _blastData.getBlastTask() != null;
    }

    public class ProgramSelectedListener implements ChangeListener {
        /**
         * When a user selects a program from the list, retrieve the concrete BlastTask, and populate the
         * program options area with the params from the retreived BlastTask.
         */
        public void onChange(Widget widget) {
            updateOptions();
        }
    }

    private void updateOptions() {
        // Determine the program selected
        _logger.debug("Changed the program type");
        if (null == listBox) {
            _logger.error("The list box is null");
        }
        int selectedIndex = listBox.getSelectedIndex();
        String displayName = "Unknown Task";
        if (selectedIndex != -1) {
            displayName = listBox.getItemText(selectedIndex);
        }
        _logger.debug("Current display name=" + displayName);
        if (null == _blastData) {
            _logger.error("The BlastData cannot be null.");
        }
        if (null == _blastData.getTaskArray()) {
            _logger.error("The TaskArray cannot be null.");
        }
        Task[] tmpTask = _blastData.getTaskArray();
        for (Task task : tmpTask) {
            _logger.debug("Checking " + task.getTaskName());
            if (task.getDisplayName().equalsIgnoreCase(displayName)) {
                _blastData.setBlastTask(task);
                _logger.debug("The BlastData now has task=" + _blastData.getBlastTask().getDisplayName());
                _taskOptionsPanel.displayParams(_blastData.getBlastTask());
                //TODO: notify listener here?
                //setupButtons(); // Update the button state to enable/disable the submit button
                break;
            }
        }
    }

    public ListBox getProgramMenu() {
        return listBox;
    }

    public void setBlastData(BlastData blastData) {
        _blastData = blastData;
    }
}