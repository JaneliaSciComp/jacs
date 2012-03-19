
package org.janelia.it.jacs.web.gwt.detail.client.bse;

import com.google.gwt.user.client.rpc.AsyncCallback;
import org.janelia.it.jacs.web.gwt.detail.client.DetailServiceAsync;

import java.util.List;

/**
 * GWT RemoteService for retrieving data needed for BSEntity DetailSubPanels
 *
 * @author Tareq Nabeel
 */
public interface BSEntityServiceAsync extends DetailServiceAsync {
    public void getBioSequence(String bseEntityId, AsyncCallback callback);

    public void getEntitySampleByAcc(String acc, AsyncCallback callback);

    public void getEntitySampleByName(String name, AsyncCallback callback);

    public void getSamplesByLibraryId(Long libraryId, AsyncCallback callback);

    public void getSampleBySampleAcc(String acc, AsyncCallback callback);

    public void getSequenceUIData(String bseEntityId, int clearRangeBegin, int clearRangeEnd, int charsPerLine, AsyncCallback callback);

    public void getBseEntity(String accession, AsyncCallback callback);

    public void getTaxonSynonyms(Integer taxonId, AsyncCallback callback);

    public void getExternalEvidenceLinks(List evidence, AsyncCallback callback);
}
