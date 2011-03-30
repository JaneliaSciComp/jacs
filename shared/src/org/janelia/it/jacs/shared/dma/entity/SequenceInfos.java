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

package org.janelia.it.jacs.shared.dma.entity;

import org.janelia.it.jacs.model.genomics.Assembly;
import org.janelia.it.jacs.shared.dma.importer.AssemblyFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * This class encapsulates a list of Sequence Info entries.  It keeps track of information
 * such as number of new entities and the accessions of pre-existing entities i.e. information
 * about Sequence Info objects.
 *
 * @author Tareq Nabeel
 */
public class SequenceInfos {

    private Map<String, SequenceInfo> parsedSequenceInfos = new HashMap<String, SequenceInfo>();

    private Map<String, SequenceInfo> importSequenceInfos = new HashMap<String, SequenceInfo>();

    private Map<String, SequenceInfo> existingSequenceInfos = new HashMap<String, SequenceInfo>();

    private Map<String, Assembly> parsedAssemblies = new HashMap<String, Assembly>();
//    private Map<String, Assembly> importAssemblies = new HashMap<String,Assembly>();
//    private Map<String, Assembly> updateAssemblies = new HashMap<String,Assembly>();

    private String parsedAccessionsStr = null;
    private String existingAccessionsStr = null;

    //    private String importAssemblyAccessionsStr = null;
    private String parsedAssemblyAccessionsStr = null;
//    private String updateAssemblyAccessionsStr = null;

    private int entitiesWithoutIds = 0;
    private int sequencesWithoutIds = 0;

    public void add(SequenceInfo info) {
        parsedSequenceInfos.put(info.getCameraAcc(), info);
        importSequenceInfos.put(info.getCameraAcc(), info);
        existingSequenceInfos.put(info.getCameraAcc(), info);
        Assembly assembly = AssemblyFactory.createAssembly(info);
        if (assembly != null) {
            parsedAssemblies.put(assembly.getAssemblyAcc(), assembly);
//            importAssemblies.put(assembly.getAssemblyAcc(),assembly);
//            updateAssemblies.put(assembly.getAssemblyAcc(),assembly);
        }
        entitiesWithoutIds++;
        sequencesWithoutIds++;
    }

    public void assignImportId(String camerAcc, long entityId, long sequenceId) {
        SequenceInfo seqInfo = importSequenceInfos.get(camerAcc);
        if (seqInfo != null) {
            seqInfo.setEntityId(entityId);
            seqInfo.setSeqId(sequenceId);
            entitiesWithoutIds--;
            sequencesWithoutIds--;
        }
    }

    public int getEntitiesWithoutIds() {
        return entitiesWithoutIds;
    }

    public int getSequencesWithoutIds() {
        return sequencesWithoutIds;
    }

    public int importSize() {
        return importSequenceInfos.size();
    }

//    public String getImportAccessionsStr() {
//        if (importAccessionsStr!=null) {
//            return importAccessionsStr;
//        } else {
//            buildImportAccessionsStr();
//            return importAccessionsStr;
//        }
//    }
//
//    public String getImportAssemblyAccessionsStr() {
//        if (importAssemblyAccessionsStr!=null) {
//            return importAssemblyAccessionsStr;
//        } else {
//            buildImportAssemblyAccessionsStr();
//            return importAssemblyAccessionsStr;
//        }
//    }
//
//

    //

    public String getExistingAccessionsStr() {
        if (existingAccessionsStr != null) {
            return existingAccessionsStr;
        }
        else {
            buildExistingAccessionsStr();
            return existingAccessionsStr;
        }
    }

//    public String getUpdateAssemblyAccessionsStr() {
//        if (updateAssemblyAccessionsStr!=null) {
//            return updateAssemblyAccessionsStr;
//        } else {
//            buildUpdateAssemblyAccessionsStr();
//            return updateAssemblyAccessionsStr;
//        }
//    }

    private void buildExistingAccessionsStr() {
        existingAccessionsStr = buildAccessions(existingSequenceInfos);
    }

//   private void buildUpdateAssemblyAccessionsStr() {
//        updateAssemblyAccessionsStr = buildAccessions(updateAssemblies);
//    }

    private void buildImportAccessionsStr() {
        buildAccessions(importSequenceInfos);
    }
//
//    private void buildImportAssemblyAccessionsStr() {
//        importAssemblyAccessionsStr = buildAccessions(importAssemblies);
//    }

    public Map<String, SequenceInfo> getImportSequenceInfos() {
        return importSequenceInfos;
    }

//    public Map<String,Assembly> getImportAssemblies() {
//        return importAssemblies;
//    }

    public void filterOutImportSequenceInfos(Collection<SequenceInfo> sequenceInfos) {
        getImportSequenceInfos().values().removeAll(sequenceInfos);
        buildImportAccessionsStr();
    }

//    public void filterImportAssemblies(Collection<Assembly> assemblies) {
//        getImportAssemblies().values().removeAll(assemblies);
////        buildImportAssemblyAccessionsStr();
//    }

    public void filterOutExistingSequenceInfos(Collection<SequenceInfo> sequenceInfos) {
        getExistingSequenceInfos().values().removeAll(sequenceInfos);
        buildExistingAccessionsStr();
    }

//    public void filterOutUpdateAssemblies(Collection<Assembly> assemblies) {
//        getUpdateAssemblies().values().removeAll(assemblies);
//        buildUpdateAssemblyAccessionsStr();
//    }

    public String getParsedAccessionsStr() {
        if (parsedAccessionsStr != null) {
            return parsedAccessionsStr;
        }
        else {
            parsedAccessionsStr = buildAccessions(parsedSequenceInfos);
            return parsedAccessionsStr;
        }
    }

    public String getParsedAssemblyAccessionsStr() {
        if (parsedAssemblyAccessionsStr != null) {
            return parsedAssemblyAccessionsStr;
        }
        else {
            parsedAssemblyAccessionsStr = buildAccessions(parsedAssemblies);
            return parsedAssemblyAccessionsStr;
        }
    }

    public Map<String, SequenceInfo> getParsedSequenceInfos() {
        return parsedSequenceInfos;
    }

    public Map<String, Assembly> getParsedAssemblies() {
        return parsedAssemblies;
    }

    public Map<String, SequenceInfo> getExistingSequenceInfos() {
        return existingSequenceInfos;
    }

//    public Map<String,Assembly> getUpdateAssemblies() {
//        return updateAssemblies;
//    }

    private String buildAccessions(Map accMap) {
        StringBuilder builder = new StringBuilder("(");
        if (accMap.size() > 0) {
            for (Object s : accMap.keySet()) {
                builder.append("'");
                builder.append(s);
                builder.append("',");
            }
            builder.setLength(builder.length() - 1);
        }
        builder.append(")");
        return builder.toString();
    }


}
