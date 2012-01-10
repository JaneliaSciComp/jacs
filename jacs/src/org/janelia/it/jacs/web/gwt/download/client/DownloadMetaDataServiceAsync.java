
package org.janelia.it.jacs.web.gwt.download.client;

import com.google.gwt.user.client.rpc.AsyncCallback;
import org.janelia.it.jacs.model.common.SortArgument;
import org.janelia.it.jacs.web.gwt.download.client.model.ProjectImpl;

import java.util.List;

/**
 * Download-Page's Service Async.  Client side callback.  Implement this in javascript, to get notice.
 * User: lfoster
 * Date: Aug 24, 2006
 * Time: 5:43:39 PM
 */
public interface DownloadMetaDataServiceAsync {
    public void getSymbolToProjectMapping(AsyncCallback callback);

    public void getAccessionToPublicationMapping(AsyncCallback callback);

    public void saveOrUpdateProject(ProjectImpl project, AsyncCallback callback);

    public void getProjectByName(String projectName, AsyncCallback callback);

    public void getProjectBySymbol(String projectSymbol, AsyncCallback callback);

    public void getPublicationByAccession(String publicationAccession, AsyncCallback callback);

    public void getNewFiles(AsyncCallback callback);

    public void checkFileLocation(String fileLocation, AsyncCallback callback);

    public void getProjectSamples(String projectSymbol, AsyncCallback callback);

    public void getProjectSamplesByProject(AsyncCallback callback);

    public void getDownloadableFilesBySampleAcc(String sampleAcc, AsyncCallback callback);

    public void getProjectSampleInfo(int startIndex, int endIndex, SortArgument[] sortArgs, List<String> selectedProjectNames, AsyncCallback callback);

    public void getProjectSampleInfo(SortArgument[] sortArgs, AsyncCallback callback);

    public void getNumProjectSampleInfo(List<String> selectedProjectNames, AsyncCallback callback);
}
