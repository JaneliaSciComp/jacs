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

package org.janelia.it.jacs.model.tasks.search;

import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.user_data.search.SearchResultNode;
import org.janelia.it.jacs.model.vo.MultiSelectVO;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.model.vo.ParameterVO;
import org.janelia.it.jacs.model.vo.TextParameterVO;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Arrays;

/**
 * Created by IntelliJ IDEA.
 * User: cgoina
 * Date: Aug 24, 2006
 * Time: 4:47:12 PM
 *
 * @version $Id: SearchTask.java 1 2011-02-16 21:07:19Z tprindle $
 */
public class SearchTask extends Task {
    transient public static final int MATCH_ANY = 0;
    transient public static final int MATCH_ALL = 1;
    transient public static final int MATCH_PHRASE = 3;

    transient public static final int ALL_SEARCH_TOPICS_COMPLETED_SUCCESSFULLY = 0;
    transient public static final int SEARCH_COMPLETED_WITH_ERRORS = -1;
    transient public static final int SEARCH_TIMEDOUT = 2;
    transient public static final int SEARCH_STILL_RUNNING = 1;

    transient public static final String TOPIC_ALL = "all";
    transient public static final String TOPIC_ACCESSION = "accession";
    transient public static final String TOPIC_PROTEIN = "protein";
    transient public static final String TOPIC_CLUSTER = "final_cluster";
    transient public static final String TOPIC_PUBLICATION = "publication";
    transient public static final String TOPIC_PROJECT = "project";
    transient public static final String TOPIC_SAMPLE = "sample";
    transient public static final String TOPIC_WEBSITE = "website";
    transient public static final String TOPIC_GENOME = "genome";
    transient public static final String TOPIC_NCGENE = "ncgene";

    // Parameter Keys
    transient public static final String PARAM_searchString = "searchString";
    transient public static final String PARAM_searchTopic = "searchTopic";
    transient public static final String PARAM_matchFlags = "matchFlags";

    // Status for asynchronous thread management
    // currently supported topics
    transient private static final String[] SUPPORTED_TOPICS = {
            TOPIC_ACCESSION,
            TOPIC_PROTEIN,
            TOPIC_CLUSTER,
            TOPIC_PUBLICATION,
            TOPIC_PROJECT,
            TOPIC_SAMPLE,
            TOPIC_WEBSITE
    };

    public static String matchFlagsToString(int matchFlags) {
        String stringifiedMatchFlags = "any";
        if (matchFlags == MATCH_ALL) {
            stringifiedMatchFlags = "all";
        }
        else if (matchFlags == MATCH_PHRASE) {
            stringifiedMatchFlags = "phrase";
        }
        return stringifiedMatchFlags;
    }

    public SearchTask() {
        super();
        setTaskName("String Search");
    }

    public String getDisplayName() {
        return "Search Task";
    }

    public ParameterVO getParameterVO(String key) throws ParameterException {
        if (key == null) return null;
        String value = getParameter(key);
        if (value == null) return null;
        if (key.equals(PARAM_searchString)) {
            return new TextParameterVO(value);
        }
        if (key.equals(PARAM_searchTopic)) {
            return new MultiSelectVO(Task.listOfStringsFromCsvString(value), Task.listOfStringsFromCsvString(value));
        }
        if (key.equals(PARAM_matchFlags)) {
            return new TextParameterVO(value);
        }
        return null;
    }

    public boolean isParameterRequired(String parameterKeyName) {
        if (!super.isParameterRequired(parameterKeyName)) {
            return false;
        }
        // the only required parameter is the search string
        return PARAM_searchString.equalsIgnoreCase(parameterKeyName);
    }

    public SearchResultNode getSearchResultNode() {
        SearchResultNode searchResultNode = null;
        Set outputNodes = getOutputNodes();
        for (Object outputNode : outputNodes) {
            Node node = (Node) outputNode;
            if (node instanceof SearchResultNode) {
                searchResultNode = (SearchResultNode) node;
                break;
            }
        }
        return searchResultNode;
    }

    public String getSearchString() {
        String searchStringParam = null;
        try {
            searchStringParam = ((TextParameterVO) getParameterVO(PARAM_searchString)).getTextValue();
        }
        catch (Exception ignore) {
        }
        return searchStringParam;
    }

    public void setSearchString(String searchString) {
        setParameter(PARAM_searchString, searchString);
    }

    public List<String> getSearchTopics() {
        MultiSelectVO searchTopicsParam;
        List<String> searchTopics = null;
        try {
            searchTopicsParam = ((MultiSelectVO) getParameterVO(PARAM_searchTopic));
            if (searchTopicsParam != null) {
                searchTopics = searchTopicsParam.getActualUserChoices();
            }
        }
        catch (Exception ignore) {
        }
        return searchTopics;
    }

    public void setSearchTopics(List<String> searchTopics) {
        if (searchTopics != null && searchTopics.contains(TOPIC_ALL)) {
            setParameter(PARAM_searchTopic, csvStringFromList(getAllSupportedTopics()));
        }
        else {
            setParameter(PARAM_searchTopic, csvStringFromList(searchTopics));
        }
    }

//    public String getSearchTopicsAsCSV() {
//        MultiSelectVO searchTopicsParam;
//        List searchTopics = null;
//        try {
//            searchTopicsParam = ((MultiSelectVO) getParameterVO(PARAM_searchTopic));
//            if (searchTopicsParam != null) {
//                searchTopics = searchTopicsParam.getActualUserChoices();
//            }
//        }
//        catch (Exception ignore) {
//        }
//        String csvTopics = "all";
//        if (searchTopics != null && searchTopics.size() > 0) {
//            StringBuffer topicsBuffer = new StringBuffer();
//            for (Object searchTopic : searchTopics) {
//                if (topicsBuffer.length() > 0) {
//                    topicsBuffer.append(',');
//                }
//                topicsBuffer.append((String) searchTopic);
//            }
//            csvTopics = topicsBuffer.toString();
//        }
//        return csvTopics;
//    }

    public int getMatchFlags() {
        TextParameterVO matchFlagsParam = null;
        try {
            matchFlagsParam = (TextParameterVO) getParameterVO(PARAM_matchFlags);
        }
        catch (Exception ignore) {
        }
        int matchFlags = MATCH_ANY;
        if (matchFlagsParam != null && matchFlagsParam.getStringValue() != null) {
            if (matchFlagsParam.getStringValue().equalsIgnoreCase("all")) {
                matchFlags = MATCH_ALL;
            }
            else if (matchFlagsParam.getStringValue().equalsIgnoreCase("phrase")) {
                matchFlags = MATCH_PHRASE;
            }
        }
        return matchFlags;
    }

    public void setMatchFlags(int matchFlags) {
        setParameter(PARAM_matchFlags, matchFlagsToString(matchFlags));
    }

    public String getMatchFlagsAsString() {
        return matchFlagsToString(getMatchFlags());
    }

    public List<String> getAllSupportedTopics() {
        ArrayList<String> allSupportedTopicList = new ArrayList<String>();
        allSupportedTopicList.addAll(Arrays.asList(SUPPORTED_TOPICS));
        return allSupportedTopicList;
    }

    public boolean isTopicSupported(String topic) {
        for (String SUPPORTED_TOPIC : SUPPORTED_TOPICS) {
            if (SUPPORTED_TOPIC.equals(topic)) {
                return true;
            }
        }
        return false;
    }

    /**
     * checks whether the task has completed
     *
     * @return 0 if the task has completed successfully
     *         -1 if the task has completed with errors
     *         1 if the task is still running
     */
    public int hasCompleted() {
        Event lastEvent = getLastEvent();
        if (lastEvent.getEventType().equals(Event.COMPLETED_EVENT)) {
            return ALL_SEARCH_TOPICS_COMPLETED_SUCCESSFULLY;
        }
        else if (lastEvent.getEventType().equals(Event.ERROR_EVENT)) {
            return SEARCH_COMPLETED_WITH_ERRORS;
        }
        List<String> searchTopics = getSearchTopics();
        if (searchTopics != null && searchTopics.contains(SearchTask.TOPIC_ALL)) {
            searchTopics.addAll(getAllSupportedTopics());
        }
        // check task event's list to see if all subtasks completed with or without errors
        boolean subTaskErrorFound = false;
        for (Object o : getEvents()) {
            Event searchTaskEvent = (Event) o;
            boolean subTaskCompleted = false;
            if (searchTaskEvent.getEventType().equals(Event.SUBTASKCOMPLETED_EVENT)) {
                subTaskCompleted = true;
            }
            else if (searchTaskEvent.getEventType().equals(Event.SUBTASKERROR_EVENT)) {
                subTaskCompleted = true;
                subTaskErrorFound = true;
            }
            if (subTaskCompleted) {
                String searchCategory = searchTaskEvent.getDescription();
                if (null!=searchTopics) {
                    searchTopics.remove(searchCategory);
                }
            }
        }
        if (null!=searchTopics && searchTopics.isEmpty()) {
            if (subTaskErrorFound) {
                return SEARCH_COMPLETED_WITH_ERRORS;
            }
            else {
                return ALL_SEARCH_TOPICS_COMPLETED_SUCCESSFULLY;
            }
        }
        else {
            return SEARCH_STILL_RUNNING; // still running
        }
    }

}
