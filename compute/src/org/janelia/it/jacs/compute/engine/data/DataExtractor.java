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

package org.janelia.it.jacs.compute.engine.data;

import org.janelia.it.jacs.compute.engine.def.Parameter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
     */
    public static void copyData(IProcessData from, IProcessData to, Set<Parameter> parameters) throws MissingDataException {
        copyPrimaryData(from, to);
        if (parameters == null) {
            return;
        }
        for (Parameter parameter : parameters) {
            if (parameter.getValue() != null) {
                to.putItem(parameter.getName(), parameter.getValue());
            }
            else {
                Object obj = from.getItem(parameter.getName());
                if (obj == null && parameter.isMandatory()) {
                    throw new MissingDataException(parameter.toString());
                }
                else {
                    to.putItem(parameter.getName(), obj);
                }
            }
            //saveParameter(from, to, parameter);
        }

    }

//    private static void saveParameter(IProcessData from, IProcessData to, Parameter parameter) {
//        //We would need some changes to DB to implement this
//    }

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
     */
    private static void copyPrimaryData(IProcessData from, IProcessData to) throws MissingDataException {
        to.setProcessDef(from.getProcessDef());
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
                pd.getProcessDef().setForEachParam(null);
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
