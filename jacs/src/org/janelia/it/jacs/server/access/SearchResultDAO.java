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

package org.janelia.it.jacs.server.access;

import org.janelia.it.jacs.model.common.SortArgument;
import org.janelia.it.jacs.model.search.SearchHit;
import org.janelia.it.jacs.server.access.hibernate.DaoException;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: cgoina
 */
public interface SearchResultDAO extends DAO {

    int getNumSearchResultsByTaskId(Long searchTaskId, List<String> categories)
            throws DaoException;

    List<SearchHit> getPagedSearchResultsByTaskId(Long searchTaskId,
                                                  List<String> categories,
                                                  int startIndex,
                                                  int numRows,
                                                  SortArgument[] sortArgs)
            throws DaoException;

    List<String> getSearchResultCategoriesByTaskId(Long searchTaskId)
            throws DaoException;

    Map<String, Integer> getNumCategoryResults(Long taskId, Collection<String> categoryList)
            throws DaoException;

}
