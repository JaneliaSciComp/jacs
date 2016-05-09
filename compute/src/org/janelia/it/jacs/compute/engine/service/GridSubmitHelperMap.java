
/**
 * Created by IntelliJ IDEA.
 * User: sreenath
 * Date: Jun 9, 2009
 * Time: 4:16:06 PM
 *
 */
package org.janelia.it.jacs.compute.engine.service;

import java.util.*;

public class GridSubmitHelperMap {

    public static String ORIGINAL_SERVICE_KEY = "ORIGINAL_SERVICE";
    public static String ORIGINAL_QUEUE_MESSAGE_KEY = "ORIGINAL_QUEUE_MESSAGE";
    public static String PROCESS_OBJECT = "PROCESS_OBJ";
    public static String JOB_FAILURE_COUNTER_KEY = "JOB_FAILURE_COUNTER";

    private static GridSubmitHelperMap gridSubmitHelperMap = new GridSubmitHelperMap();

    private Map<String, Map<String, Object>> dataMap = new HashMap<String, Map<String, Object>>();

    public static GridSubmitHelperMap getInstance() {
        return gridSubmitHelperMap;
    }

    private GridSubmitHelperMap() {

    }

    public synchronized void addToDataMap(String key, Map<String, Object> value) {
        dataMap.put(key, value);
    }

    public Map<String, Object> getFromDataMap(String key) {
    	Map<String, Object> map = dataMap.get(key);
    	if (map == null) return null;
        return Collections.synchronizedMap(map);
    }

    public synchronized Set<String> getDataMapKeys() {
        return new HashSet<String>(dataMap.keySet());
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
        if (proc!=null) {
        	proc.destroy();
        }
        dataMap.remove(key);
        System.gc();
    }
}
