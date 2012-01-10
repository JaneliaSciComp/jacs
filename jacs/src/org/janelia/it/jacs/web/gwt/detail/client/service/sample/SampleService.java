
package org.janelia.it.jacs.web.gwt.detail.client.service.sample;

import com.google.gwt.user.client.rpc.RemoteService;
import org.janelia.it.jacs.model.common.SortArgument;
import org.janelia.it.jacs.model.genomics.Read;
import org.janelia.it.jacs.web.gwt.common.client.service.GWTServiceException;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: cgoina
 * Date: Nov 15, 2007
 * Time: 10:14:14 PM
 */
public interface SampleService extends RemoteService {
    Integer getNumSampleReads(String sampleAcc) throws GWTServiceException;

    List<Read> getPagedSampleReads(String sampleAcc, int startIndex, int numRows, SortArgument[] sortArgs)
            throws GWTServiceException;
}
