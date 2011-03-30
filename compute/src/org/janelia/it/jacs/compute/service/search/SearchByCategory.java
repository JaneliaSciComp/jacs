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

package org.janelia.it.jacs.compute.service.search;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.api.SearchBeanLocal;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;

import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Oct 29, 2007
 * Time: 4:08:07 PM
 */
public class SearchByCategory implements IService {

    private Logger _logger;

    public SearchByCategory() {
    }

    private ArrayList<String> getTopics(IProcessData processData) throws MissingDataException {
        ArrayList<String> topics;
        try {
            String searchType = ((String) processData.getMandatoryItem("SEARCH_TYPE"));
            if ("fast".equalsIgnoreCase(searchType)) {
                topics = (ArrayList<String>) processData.getMandatoryItem("SEARCH_FAST_CATEGORIES");
            }
            else if ("slow".equalsIgnoreCase(searchType)) {
                topics = (ArrayList<String>) processData.getMandatoryItem("SEARCH_SLOW_CATEGORIES");
            }
            else {
                throw new ServiceException("SearchByCategory is trying to execute unknown " + searchType + " search");
            }
            return topics;
        }
        catch (MissingDataException e) {
            throw e;
        }
        catch (Exception e) {
            throw new MissingDataException("Could not load SearchTask for id=" + processData.getProcessId() + " :" + e.getMessage());
        }
    }

    public void execute(IProcessData processData) throws SystemSearchException {
        try {
            _logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            SearchBeanLocal searchBean = EJBFactory.getLocalSearchBean();
            ArrayList<String> topics = getTopics(processData);
            if (topics.size() > 0) {
                searchBean.populateSearchResult(processData.getProcessId(), topics);
            }
        }
        catch (Exception e) {
            _logger.error(e.getMessage(), e);
            throw new SystemSearchException(e);
        }
    }

}
