
package org.janelia.it.jacs.web.gwt.common.client.panel;

import com.google.gwt.user.client.ui.*;
import org.janelia.it.jacs.web.gwt.common.client.Constants;
import org.janelia.it.jacs.web.gwt.common.client.popup.ErrorPopupPanel;
import org.janelia.it.jacs.web.gwt.common.client.popup.launcher.PopupCenteredLauncher;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.common.client.ui.RoundedButton;
import org.janelia.it.jacs.web.gwt.common.client.ui.SelectionListener;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Mar 18, 2008
 * Time: 11:47:19 AM
 */
public class FileChooserPanel extends HorizontalPanel {
    private static Logger _logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.common.client.panel.FileChooserPanel");

    public enum FILE_TYPE {
        fasta, fa, fna, fsa, faa, mpfa, ffn, frg, aln, alignment, clustalw, clw, prof, profile,
        qual, qv, seq, txt, info, pep
    }

    public static final String UPLOAD_SEQUENCE_NAME_PARAM = "uploadSequenceName";

    private Label fileUploadResultMessage;
    private HorizontalPanel uploadBar;
    final FormPanel uploadForm = new FormPanel();
    private FileUpload uploader;

    // Local storage for uploaded files
    private String _uploadedFileName;
    private String _sessionDataNodeKey;
    private List<FILE_TYPE> fileTypes = new ArrayList<FILE_TYPE>();

    private RoundedButton uploadFileButton;
    private SelectionListener selectionListener;
    private String _uploadMessage = "File upload successful.";
    private boolean uploadWasSuccessful = false;
    private String sequenceType = null;

    /**
     * Widget to assist in file uploads.  A null fileType list means anything goes.
     *
     * @param selectionListener - callback for selection
     * @param fileTypes         - allowable file types for upload.  Null equals any file type allowed.
     */
    public FileChooserPanel(SelectionListener selectionListener, List<FILE_TYPE> fileTypes) {
        super();
        this.selectionListener = selectionListener;
        this.fileTypes = fileTypes;
        if (null == fileTypes) {
            new PopupCenteredLauncher(new ErrorPopupPanel("The file chooser must be given distinct types"), 250).showPopup(uploadBar);
        }
        this.setVisible(true);
        init();
    }


    public void setUploadMessage(String uploadMessage) {
        _uploadMessage = uploadMessage;
    }

    protected void init() {
        this.setStyleName("UserNewSeqPanel");
        setupUploadPanel();
        setVisible(true);
    }

    private void setupUploadPanel() {
        // Create a Form (required for FileUpload widget)
        setVerticalAlignment(HorizontalPanel.ALIGN_MIDDLE);
        setHorizontalAlignment(HorizontalPanel.ALIGN_LEFT);
        uploadForm.setAction("fileUpload.htm");
        uploadForm.setEncoding(FormPanel.ENCODING_MULTIPART);
        uploadForm.setMethod(FormPanel.METHOD_POST);

        uploadBar = new HorizontalPanel();
        uploader = new FileUpload();
        uploader.setName("uploadFormElement");
        uploadBar.add(uploader);

        uploadForm.setWidget(uploadBar);
        uploadBar.setHorizontalAlignment(HorizontalPanel.ALIGN_LEFT);
        uploadBar.setVerticalAlignment(HorizontalPanel.ALIGN_MIDDLE);
        add(uploadForm);

        // Add a 'submit' button.
        uploadBar.add(HtmlUtils.getHtml("&nbsp;", "spacer"));
        uploadFileButton = new RoundedButton("Upload", new ClickListener() {
            public void onClick(Widget sender) {
                Hidden nameParam = new Hidden();
                nameParam.setName(UPLOAD_SEQUENCE_NAME_PARAM);
                nameParam.setValue("testFile");
                uploadBar.add(nameParam);
                uploadForm.submit();
            }
        });
        uploadFileButton.setEnabled(true);
        uploadBar.add(uploadFileButton);

        // Add an event handler to the form.
        uploadForm.addFormHandler(new FormHandler() {

            public void onSubmitComplete(FormSubmitCompleteEvent event) {
                // When the form submission is successfully completed, this event is
                // fired. Assuming the service returned a response of type text/plain,
                // we can get the result text here (see the FormPanel documentation for
                // further explanation).
                if (fileUploadResultMessage != null) {
                    uploadBar.remove(fileUploadResultMessage);
                    fileUploadResultMessage = null;
                }
                try {
                    if (event == null) {
                        _logger.error("onSubmitComplete() event returned null");
                    }
                    else {
                        _logger.debug("OnSubmitComplete - processing return message");
                    }
                    if (event != null && null != event.getResults() && !"".equals(event.getResults())) {
                        String returnMessage = event.getResults();
                        _logger.info("Return message=" + returnMessage);
                        if (returnMessage.indexOf(Constants.ERROR_TEXT_SEPARATOR) != -1) {
                            handleError(returnMessage);
                            return;
                        }
                        // See FileUploadController for construction of this Array
                        // First get rid of any surrounding html added by the servlet writer
                        String[] outerArr = returnMessage.split(Constants.OUTER_TEXT_SEPARATOR);
                        _logger.info("outerArr member count=" + outerArr.length);
                        String innerMessage = outerArr[1];
                        _logger.info("innerMessage=" + innerMessage);
                        String[] msgArr = innerMessage.split(Constants.INNER_TEXT_SEPARATOR);
                        _logger.info("msgArr member count=" + msgArr.length);
                        _sessionDataNodeKey = msgArr[0];
                        _logger.info("sessionDataNodeKey=" + _sessionDataNodeKey);
                        _uploadedFileName = msgArr[2];
                        _logger.info("setting message results");

                        //set sequence type
                        sequenceType = msgArr[1];
                        addFileUploadResultMessage(_uploadMessage, "FileUploadMessage");
                        uploadWasSuccessful = true;
                        validateFormAndEnableApplyButton();
                    }
                    else {
                        // This might be a 'back' or 'change' transition
                        _logger.info("return message not qualified for processing (back button probably pressed)");
                    }
                }
                catch (Throwable e) {
                    _logger.error("Error onSubmitComplete(): " + e.getMessage());
                    uploadWasSuccessful = false;
                    validateFormAndEnableApplyButton();
                    if (uploader.getFilename() == null || uploader.getFilename().equals("")) {
                        addFileUploadResultMessage("File upload was not possible.  Filename was not specified.", "FileUploadErrorMessage");
                    }
                    else {
                        addFileUploadResultMessage("Upload of file " + uploader.getFilename() + " was not successful.", "FileUploadErrorMessage");
                    }
                }
            }

            public void onSubmit(FormSubmitEvent event) {
                // This event is fired just before the form is submitted. We can take
                // this opportunity to perform validation.
                String tmpFilename = uploader.getFilename();
                _logger.debug("Submitting " + tmpFilename + ".....");
                for (FILE_TYPE fileType : fileTypes) {
                    if (tmpFilename.toLowerCase().endsWith(fileType.toString())) {
                        uploadButtonChecked();
                        return;
                    }
                }
                new PopupCenteredLauncher(new ErrorPopupPanel("The file selected must carry a known extension."), 250).showPopup(uploadBar);
                event.setCancelled(true);
                clear();
                selectionListener.onUnSelect(null);
            }

            private void handleError(String returnMessage) {
                String[] outerArr = returnMessage.split(Constants.ERROR_TEXT_SEPARATOR);
                String errorMessage = outerArr[1];
                _logger.debug("ErrorMessage: " + errorMessage);
                ErrorPopupPanel popup = new ErrorPopupPanel(errorMessage);
                new PopupCenteredLauncher(popup, 250).showPopup(uploadFileButton);
            }

        });
    }

    public String getUploadedFileName() {
        return _uploadedFileName;
    }

    public String getSessionDataNodeKey() {
        return _sessionDataNodeKey;
    }

    private boolean validateFormAndEnableApplyButton() {
        boolean isValid = false;

        // Check on whether page is ready to go
        if (uploadWasSuccessful) {
            isValid = true;
            selectionListener.onSelect(_uploadedFileName);
        }
        else {
            selectionListener.onUnSelect(null);
            clear();
        }
        return isValid;
    }

    private void uploadButtonChecked() {
        validateFormAndEnableApplyButton();
    }

    public String getSequenceType() {
        return sequenceType;
    }

    private void addFileUploadResultMessage(String message, String style) {
        if (fileUploadResultMessage != null) {
            uploadBar.remove(fileUploadResultMessage);
        }
        fileUploadResultMessage = new Label(message);
        fileUploadResultMessage.setStyleName(style);
        uploadBar.add(fileUploadResultMessage);
    }

    public void clear() {
        // New sequence tab
        if (fileUploadResultMessage != null) {
            uploadBar.remove(fileUploadResultMessage);
        }
        // todo Need to clear the file selection field.
        uploader.setName("");
    }

}