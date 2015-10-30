
package org.janelia.it.jacs.web.gwt.download.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import org.janelia.it.jacs.web.gwt.common.client.service.ResultReceiver;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Lfoster
 * Date: Sep 25, 2006
 * Time: 4:19:44 PM
 */
public class PublicationServiceHelper {
    private static Logger _logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.download.client.PublicationServiceHelper");
    private static List EMPTY_LIST = new ArrayList();

    /**
     * Get the service to call, for model information.
     *
     * @return what to call for pub info.
     */
    public static DownloadMetaDataServiceAsync getPublicationService() {
        // Declare/obtain service.
        try {
            DownloadMetaDataServiceAsync publicationService =
                    (DownloadMetaDataServiceAsync) GWT.create(DownloadMetaDataService.class);

            ServiceDefTarget endpoint = (ServiceDefTarget) publicationService;
            if (publicationService == null)
                throw new Exception("Could not find Publication Service Endpoint");

            endpoint.setServiceEntryPoint("download.oas");

            return publicationService;

        }
        catch (Exception ex) {

            logError("Failed to get publication service");
            return null;

        }

    }

    /**
     * Get the collection of all projects
     *
     * @param receiver where to callback and set results (and handle errors)
     */
    public static void populateProjects(final ResultReceiver receiver) {
        DownloadMetaDataServiceAsync publicationService = getPublicationService();
        _logger.debug("Populate projects");
        // Prepare a callback for obtaining the project object.
        AsyncCallback callback = new AsyncCallback() {

            public void onSuccess(Object result) {
                try {
                    receiver.setResult(result);
                }
                catch (Exception ex) {
                    _logger.error("Error on successful 'get all projects' callback");
                }

            }

            public void onFailure(Throwable caught) {
                _logger.error("Server get-all-projects request raised an error: Java exception ");
                receiver.setResult(null);
            }
        };

        // At last, call the service.
        try {
            publicationService.getSymbolToProjectMapping(callback);
        }
        catch (Exception e) {
            logError("Server get-all-projects request raised an error: " +
                    e.getMessage());
            receiver.setResult(null);
        }
    }

    /**
     * Get the project object corresponding to the project name.  Store it in the Project member.
     */
    public static void populateProjectByName(final ResultReceiver receiver, String projectName) {
        if (projectName == null) {
            receiver.setResult(null);
        }

        DownloadMetaDataServiceAsync publicationService = getPublicationService();

        // Prepare a callback for obtaining the project object.
        AsyncCallback callback = new AsyncCallback() {

            public void onSuccess(Object result) {
                try {
                    // let caller handle the null
                    receiver.setResult(result);
                }
                catch (Exception ex) {
                    _logger.error(ex);
                }
            }

            public void onFailure(Throwable caught) {
                _logger.error(caught);
            }
        };

        //  At last, call the service.
        try {
            _logger.debug("Retrieve project using name: " + projectName);
            publicationService.getProjectByName(projectName, callback);
        }
        catch (Exception e) {
            logError("Server Project request raised an error: " +
                    e.getMessage());
        }
    }

    /**
     * Get the project object corresponding to the project symbol.  Store it in the Project member.
     */
    public static void populateProjectBySymbol(final ResultReceiver receiver, String projectSymbol) {
        if (projectSymbol == null) {
            logError("Null project symbol given.  Cannot populate project.");
            receiver.setResult(null);
        }

        DownloadMetaDataServiceAsync publicationService = getPublicationService();

        // Prepare a callback for obtaining the project object.
        AsyncCallback callback = new AsyncCallback() {

            public void onSuccess(Object result) {
                try {
                    // let caller handle the null
                    receiver.setResult(result);
                }
                catch (Exception ex) {
                    logError("Error on successful 'project' callback "
                            + ex.getMessage().substring(0, 240));
                }
            }

            public void onFailure(Throwable caught) {
                logError("Server Project request raised an error; Java exception " +
                        caught.toString().substring(0, 90));
            }
        };

        // At last, call the service.
        try {
            _logger.debug("Retrieve project using symbol: " + projectSymbol);
            publicationService.getProjectBySymbol(projectSymbol, callback);
        }
        catch (Exception e) {
            logError("Server Project request raised an error: " +
                    e.getMessage());
        }
    }

    /**
     * Get the list of new files.
     *
     * @param receiver
     */
    public static void populateNewFiles(final ResultReceiver receiver) {
        DownloadMetaDataServiceAsync publicationService = getPublicationService();

        // Prepare a callback for invoking the service.  After the publicationService
        // has been called (indeed, some time after it has been called), this method
        // will be invoked.
        AsyncCallback callback = new AsyncCallback() {

            public void onSuccess(Object result) {
                try {
                    List newFiles = new ArrayList();
                    newFiles.addAll((List) result);
                    // Content panel depends on what has been loaded here.
                    receiver.setResult(newFiles);

                }
                catch (Exception ex) {
                    logError("Error on success callback: " + ex.getMessage().substring(0, 120));
                }
            }

            public void onFailure(Throwable caught) {
                if (caught.getMessage() == null)
                    logError("null on failure callback");
                else
                    logError(caught.getMessage().substring(0, 120));
                receiver.setResult(EMPTY_LIST);
            }
        };

        try {
            publicationService.getNewFiles(callback);
        }
        catch (Exception ex) {
            logError(ex.getMessage() + " while obtaining list of new files.");
        }

    }

    /**
     * Check location of a file on the server.
     */
    public static void checkFileLocation(final ResultReceiver receiver, String fileLocation) {
        DownloadMetaDataServiceAsync publicationService = getPublicationService();

        // Prepare a callback for invoking the service.  After the publicationService
        // has been called (indeed, some time after it has been called), this method
        // will be invoked.
        AsyncCallback callback = new AsyncCallback() {

            public void onSuccess(Object result) {
                try {
                    receiver.setResult(result);
                }
                catch (Exception ex) {
                    logError("Error on success callback: " + ex.getMessage().substring(0, 120));
                }
            }

            public void onFailure(Throwable caught) {
                if (caught.getMessage() == null)
                    logError("This operation failed on the server");
                else
                    logError(caught.getMessage().substring(0, 120));
                receiver.setResult(Boolean.FALSE);
            }
        };

        try {
            // Must stat a gzip or a zip file, because raw need not be on disk.
            if (!fileLocation.endsWith(".gz") && !fileLocation.endsWith(".zip")) {
                fileLocation = fileLocation + ".zip";
            }
            publicationService.checkFileLocation(fileLocation, callback);
        }
        catch (Exception ex) {
            receiver.setResult(Boolean.FALSE);  // Must continue.  No stopping app over one missing file!
            //logError(ex.getMessage() + " while checking file " + fileLocation);
        }

    }

    private static void logError(String message) {
        Window.alert(message);
    }
}
