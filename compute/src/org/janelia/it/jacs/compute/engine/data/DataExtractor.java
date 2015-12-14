
package org.janelia.it.jacs.compute.engine.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.janelia.it.jacs.compute.engine.def.DefCache;
import org.janelia.it.jacs.compute.engine.def.Parameter;
import org.janelia.it.jacs.compute.engine.def.ProcessDef;

/**
 * This class encapsulates the logic used to copy data between service data and
 * process data
 *
 * @author Tareq Nabeel
 */
public class DataExtractor {

    protected DataExtractor() {
    }

    /**
     * Copies state of <code>parameters</code> in <code>from</code> to the destination
     * specified by <code>to</code>
     *
     * @param from       source
     * @param to         destination
     * @param parameters input or output parameters to look for in the source
     * @throws MissingDataException missing data error
     */
    public static void copyData(IProcessData from, IProcessData to, Set<Parameter> parameters) throws MissingDataException {
        copyPrimaryData(from, to);
        if (parameters == null) {
            return;
        }
        for (Parameter parameter : parameters) {
            if (parameter.getValue() != null) {
            	// Parameters with hard coded values are added to the ProcessData later on, when they are really needed.
            }
            else {
                Object obj = from.getLiteralItem(parameter.getName());
                if (obj == null && parameter.isMandatory()) {
                    throw new MissingDataException(parameter.toString());
                }
                else if (obj != null || to.getLiteralItem(parameter.getName())==null) { 
                	// Don't override existing non-null items with nulls. 
                	// Not sure if I like this policy, but it's necessary for certain things to work. 
                    to.putItem(parameter.getName(), obj);
                }
            }
        }

    }

    /**
     * Copies state of all data in <code>from</code> to the destination
     * specified by <code>to</code>
     *
     * @param from source
     * @param to   destination
     */
    public static void copyAllData(IProcessData from, IProcessData to) {
        Set<Map.Entry<String, Object>> entries = from.entrySet();
        for (Map.Entry<String, Object> entry : entries) {
            to.putItem(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Copies state of primary data in <code>from</code> to the destination
     * specified by <code>to</code>
     *
     * @param from source
     * @param to   destination
     * @throws MissingDataException missing data error
     */
    private static void copyPrimaryData(IProcessData from, IProcessData to) throws MissingDataException {
        to.setProcessDefName(from.getProcessDefName());
        to.setActionToProcess(from.getActionToProcess());
        to.setProcessId(from.getProcessId());
//        to.setProcess(from.getProcess());
    }

    /**
     * For each parameter value specified by forEachPDParam that is found in processData, this method
     * creates a copy of processData.  It places each list item in processData thereby replacing
     * the list
     *
     * @param processData    Process or Service data
     * @param forEachPDParam the parameter to look for
     * @return List of process datas
     */
    public static List<IProcessData> createForEachPDs(IProcessData processData, String forEachPDParam) {
        List values = (List) processData.getItem(forEachPDParam);
        if (values == null || values.size() == 0) {
            return null;
        }
        List<IProcessData> forEachPDs = new ArrayList<IProcessData>(values.size());
        for (Object value : values) {
            ProcessData pd = new ProcessData();
            DataExtractor.copyAllData(processData, pd);
            try {
                ProcessDef def = DefCache.getProcessDef(pd.getProcessDefName());
                def.setForEachParam(null);
            }
            catch (MissingDataException e) {
                // this wouldn't happen in this method
            }
            pd.putItem(forEachPDParam, value);
            forEachPDs.add(pd);
        }
        return forEachPDs;
    }
}
