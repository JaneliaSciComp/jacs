
package org.janelia.it.jacs.web.gwt.detail.client.service.protein;

import com.google.gwt.user.client.rpc.RemoteService;
import org.janelia.it.jacs.model.common.SortArgument;
import org.janelia.it.jacs.model.genomics.ProteinAnnotation;
import org.janelia.it.jacs.model.genomics.ProteinClusterMember;
import org.janelia.it.jacs.web.gwt.common.client.service.GWTServiceException;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: cgoina
 * Date: Nov 15, 2007
 * Time: 10:14:14 PM
 */
public interface ProteinDetailService extends RemoteService {
    ProteinClusterMember getProteinClusterInfo(String proteinAcc)
            throws GWTServiceException;

    List<ProteinAnnotation> getProteinAnnotations(String proteinAcc, SortArgument[] sortArgs)
            throws GWTServiceException;
}
