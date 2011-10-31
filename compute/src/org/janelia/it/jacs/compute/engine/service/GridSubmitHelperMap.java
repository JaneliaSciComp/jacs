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

/**
 * Created by IntelliJ IDEA.
 * User: sreenath
 * Date: Jun 9, 2009
 * Time: 4:16:06 PM
 *
 */
package org.janelia.it.jacs.compute.engine.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class GridSubmitHelperMap {

    public static String ORIGINAL_SERVICE_KEY = "ORIGINAL_SERVICE";
    public static String ORIGINAL_QUEUE_MESSAGE_KEY = "ORIGINAL_QUEUE_MESSAGE";
    public static String PROCESS_OBJECT = "PROCESS_OBJ";
    public static String JOB_FAILURE_COUNTER_KEY = "JOB_FAILURE_COUNTER";

    private static GridSubmitHelperMap gridSubmitHelperMap = new GridSubmitHelperMap();

    private Map<String, Map> dataMap = Collections.synchronizedMap(new HashMap<String, Map>());

    public static GridSubmitHelperMap getInstance() {
        return gridSubmitHelperMap;
    }

    private GridSubmitHelperMap() {

    }

    public synchronized void addToDataMap(String key, Map<String, Object> value) {
        dataMap.put(key, value);
    }

    public Map<String, Object> getFromDataMap(String key) {
        return dataMap.get(key);
    }

    public synchronized Set<String> getDataMapKeys() {
        return dataMap.keySet();
    }

    /**
     * This method is intended to clean up the job data after completion, but was causing problems until we added the
     * explicit calls to proc.destroy() and System.gc().  Before that, pipes for the procs were getting left around
     * until the JVM decided to gc.  This caused pipes to pile up until the parent JBoss process died.
     *
     * @param key - unique job key
     */
    public synchronized void removeFromDataMap(String key) {
        HashMap<String, Object> originalObjectMap = (HashMap<String, Object>) dataMap.get(key);
        Process proc = (Process) originalObjectMap.get(PROCESS_OBJECT);
        proc.destroy();
        dataMap.remove(key);
        System.gc();
    }
}
