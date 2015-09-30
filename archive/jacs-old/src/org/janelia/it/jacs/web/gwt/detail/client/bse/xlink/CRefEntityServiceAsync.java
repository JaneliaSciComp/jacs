
package org.janelia.it.jacs.web.gwt.detail.client.bse.xlink;

import com.google.gwt.user.client.rpc.AsyncCallback;
import org.janelia.it.jacs.model.common.SortArgument;


/**
 * GWT AsyncService for retrieving correlated entities
 *
 * @author Cristian Goina
 */
public interface CRefEntityServiceAsync {

    /**
     * @param readAccNo the accession number of the entity of interest
     * @return the number of correlated assembly (scaffolds) entities
     * @throws org.janelia.it.jacs.web.gwt.common.client.service.GWTServiceException
     *
     */
    void getNumAssembledScaffoldForReadByAccNo(String readAccNo, AsyncCallback async);

    /**
     * Required by the Google Web Toolkit compiler - Must be directly before the argument
     */
    void getAssembledScaffoldForReadByAccNo(String readAccNo, int startIndex, int numRecords, SortArgument[] sortArgs, AsyncCallback async);

    /**
     * @param entityAccNo the accession number of the entity of interest
     * @return the number of correlated ncRNA entities
     * @throws org.janelia.it.jacs.web.gwt.common.client.service.GWTServiceException
     *
     */
    void getNumRelatedNCRNAs(String entityAccNo, AsyncCallback async);

    /**
     * Required by the Google Web Toolkit compiler - Must be directly before the argument
     */
    void getRelatedNCRNAs(String entityAccNo, int startIndex, int numRecords, SortArgument[] sortArgs, AsyncCallback async);

    /**
     * @param entityAccNo the accession number of the entity of interest
     * @return the number of correlated ORF entities
     * @throws org.janelia.it.jacs.web.gwt.common.client.service.GWTServiceException
     *
     */
    void getNumRelatedORFs(String entityAccNo, AsyncCallback async);

    /**
     * @param entityAccNo
     * @param startIndex
     * @param numRecords
     * @param sortArgs
     */
    void getRelatedORFsAndRNAs(String entityAccNo, int startIndex, int numRecords, SortArgument[] sortArgs, AsyncCallback async);


    /**
     * Required by the Google Web Toolkit compiler - Must be directly before the argument
     */
    void getRelatedORFs(String entityAccNo, int startIndex, int numRecords, SortArgument[] sortArgs, AsyncCallback async);

    void getNumFeatures(String entityAccNo, AsyncCallback async);

    /**
     */
    void getFeatures(String entityAccNo, int startIndex, int numRecords, SortArgument[] sortArgs, AsyncCallback async);

    /**
     * @param scaffoldAccNo the accession number of the entity of interest
     * @return the number of reads that are part of the given scaffold
     * @throws org.janelia.it.jacs.web.gwt.common.client.service.GWTServiceException
     *
     */
    void getNumOfReadsForScaffoldByAccNo(String scaffoldAccNo, AsyncCallback async);

    /**
     * Required by the Google Web Toolkit compiler - Must be directly before the argument
     */
    void getReadsForScaffoldByAccNo(String scaffoldAccNo, int startIndex, int numRecords, SortArgument[] sortArgs, AsyncCallback async);

    /**
     * @param scaffoldAccNo the accession number of the entity of interest
     * @return the number of peptides that are part of the given scaffold
     * @throws org.janelia.it.jacs.web.gwt.common.client.service.GWTServiceException
     *
     */
    void getNumOfPeptidesForScaffoldByAccNo(String scaffoldAccNo, AsyncCallback async);

    /**
     * Required by the Google Web Toolkit compiler - Must be directly before the argument
     */
    void getPeptidesForScaffoldByAccNo(String scaffoldAccNo, int startIndex, int numRecords, SortArgument[] sortArgs, AsyncCallback async);
}
