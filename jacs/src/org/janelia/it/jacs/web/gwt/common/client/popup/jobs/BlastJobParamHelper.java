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

package org.janelia.it.jacs.web.gwt.common.client.popup.jobs;

import com.google.gwt.user.client.ui.HTML;
import org.janelia.it.jacs.model.tasks.blast.BlastTask;
import org.janelia.it.jacs.shared.tasks.BlastJobInfo;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Helper class should disappear!
 */
public class BlastJobParamHelper {
    public static Map<String, String> createBlastPopupParamMap(BlastJobInfo job) {
        HashMap<String, String> newMap = new HashMap<String, String>();
        Map<String, String> paramMap = job.getParamMap();
        String queryName = job.getQueryName();

        // Concat subject names
        String subjectName = "";
        for (String s : job.getSubjectIdsToNames().values())
            subjectName += (s + " ");
        subjectName = subjectName.trim();

        for (String key : job.getParamMap().keySet()) {
            String value = paramMap.get(key);
            // Switching the parameter keys around is dangerous business.  Keys should probably always stay in one context.
            if (key.equals(BlastTask.PARAM_query) && queryName != null) {
                newMap.put(BlastTask.PARAM_query, queryName);
                newMap.put(BlastTask.PARAM_query + " Id", value);
            }
            else if (key.equals(BlastTask.PARAM_subjectDatabases) && subjectName != null) {
                newMap.put(BlastTask.PARAM_subjectDatabases, subjectName);
                newMap.put(BlastTask.PARAM_subjectDatabases + " Id", value);
            }
            else {
                newMap.put(key, value);
            }
        }

        return newMap;
    }

    /**
     * Returns bulleted HTML list of all of the subject query names
     *
     * @param job job to get the subjects from
     * @return HTML of the subject names
     */
    public static HTML getConcatenatedSubjectNames(BlastJobInfo job) {
        String returnString = "";
        if (null != job && null != job.getAllSubjectNames() && job.getAllSubjectNames().size() > 0) {
            StringBuffer buf = new StringBuffer();
            for (int i = 0; i < job.getAllSubjectNames().size(); i++) {
                buf.append("&bull;&nbsp;").append(job.getAllSubjectNames().get(i)).append("<br/>");
            }
            returnString = buf.toString();
        }
        return HtmlUtils.getHtml(returnString, "infoText");
    }


}
