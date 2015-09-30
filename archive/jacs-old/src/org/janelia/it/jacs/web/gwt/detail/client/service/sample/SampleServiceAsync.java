
package org.janelia.it.jacs.web.gwt.detail.client.service.sample;

import com.google.gwt.user.client.rpc.AsyncCallback;
import org.janelia.it.jacs.model.common.SortArgument;

/**
 * Created by IntelliJ IDEA.
 * User: cgoina
 * Date: Nov 15, 2007
 * Time: 10:14:14 PM
 */
public interface SampleServiceAsync {
    void getNumSampleReads(String sampleAcc, AsyncCallback callback);

    void getPagedSampleReads(String sampleAcc, int startIndex, int numRows, SortArgument[] sortArgs, AsyncCallback callback);
}
