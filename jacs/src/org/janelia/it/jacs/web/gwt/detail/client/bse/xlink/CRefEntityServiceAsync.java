/*
 * Copyright (c) 2010-2011, J. Craig Venter Institute, Inc.
 *
 * This file is part of JCVI VICS.
 *
 * JCVI VICS is free software; you can redistribute it and/or modify it
 * under the terms and conditions of the Artistic License 2.0.  For
 * details, see the full text of the license in the file LICENSE.txt.  No
 * other rights are granted.  Any and all third party software rights to
 * remain with the original developer.
 *
 * JCVI VICS is distributed in the hope that it will be useful in
 * bioinformatics applications, but it is provided "AS IS" and WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTIES including but not limited to
 * implied warranties of merchantability or fitness for any particular
 * purpose.  For details, see the full text of the license in the file
 * LICENSE.txt.
 *
 * You should have received a copy of the Artistic License 2.0 along with
 * JCVI VICS.  If not, the license can be obtained from
 * "http://www.perlfoundation.org/artistic_license_2_0."
 */

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
