package org.janelia.it.jacs.compute.engine.def;

import java.util.HashMap;
import java.util.Map;

/**
 * A cache for obtaining parsed process definitions.  
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DefCache {

    private static final DefCache singleton = new DefCache(); 

    private final DefLoader processDefLoader = new DefLoader();
    private final Map<String,ProcessDef> cache = new HashMap<>();

    private DefCache() {
        // Singleton
    }
    
    private synchronized ProcessDef get(String processName) {
        ProcessDef processDef = cache.get(processName);
        if (processDef==null) {
            processDef = processDefLoader.loadProcessDef(processName);
            cache.put(processName, processDef);
        }
        return processDef;
    }

    public static ProcessDef getProcessDef(String processName) {
        return singleton.get(processName);
    }
}
