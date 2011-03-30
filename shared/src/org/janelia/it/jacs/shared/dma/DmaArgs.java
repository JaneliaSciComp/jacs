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

package org.janelia.it.jacs.shared.dma;

import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.shared.utils.FileUtil;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * This class represents the configurable parameters in DMA.
 *
 * @author Tareq Nabeel
 */
public class DmaArgs {

    private String[] fastaInputs;

    private boolean doFastaImport;
    private boolean doInternalEntityImport;
    private boolean doExternalEntityImport;
    private boolean doSequenceImport = true;
    private boolean doBlastDBCreation;

    private boolean allReport;
    private String[] truncateTables;
    private boolean recreateIndexes;
    private Set<Long> idsOfBlastNodesToRegenerate = new HashSet<Long>();

    private boolean createScratchTables;
    private boolean isAscFileOrder;
    private boolean checkForExistingEntitiesDuringFastaImport;
    private boolean keepFasta = true;

    private Long externalEntityId;

    private static final String KEY_FASTA_IMORT = SystemConfigurationProperties.getString("dma.param.abbrev.fastaImport");
    private static final String KEY_INTERNAL_ENTITY_IMPORT = SystemConfigurationProperties.getString("dma.param.abbrev.internalEntityImport");
    private static final String KEY_EXTERNAL_ENTITY_IMPORT = SystemConfigurationProperties.getString("dma.param.abbrev.externalEntityImport");
    private static final String KEY_SEQUENCE_IMPORT = SystemConfigurationProperties.getString("dma.param.abbrev.biosequenceImport");
    private static final String KEY_GENERATE_DATASETS = SystemConfigurationProperties.getString("dma.param.abbrev.generateBlastableDatasets");
    private static final String KEY_TRUNCATE = SystemConfigurationProperties.getString("dma.param.abbrev.truncateTableList");
    private static final String KEY_RECREATE_INDEXES = SystemConfigurationProperties.getString("dma.param.abbrev.recreateIndexes");
    private static final String KEY_CREATE_SCRATCH_TABLES = SystemConfigurationProperties.getString("dma.param.abbrev.createScatchTables");
    private static final String KEY_ALL_REPORT = SystemConfigurationProperties.getString("dma.param.abbrev.verboseEntityImportReporting");
    private static final String KEY_FILE_ORDER = SystemConfigurationProperties.getString("dma.param.abbrev.processFileSizeOrder");
    private static final String KEY_CHCK_FOR_EXISTG_ENTITIES_IN_FASTA_IMPORT = SystemConfigurationProperties.getString("dma.param.abbrev.checkForExistingEntitiesDuringFastaImport");
    private static final String KEY_KEEP_FASTA = SystemConfigurationProperties.getString("dma.param.abbrev.keepFasta");

    public DmaArgs(String[] args) throws IOException {
        if (args != null && args.length > 0) {
            for (String arg : args) {
                String[] nameValue = arg.split("=");
                if (nameValue.length > 1) {
                    nameValue[0] = nameValue[0].trim();
                    nameValue[1] = nameValue[1].trim();
                }
                else {
                    throw new IllegalArgumentException("name and value must be separated by =");
                }
                String[] values = getArgValues(nameValue[1]);
                if (nameValue[0].equals(KEY_FASTA_IMORT)) {
                    doFastaImport = true;
                    fastaInputs = values;
                }
                else if (nameValue[0].equals(KEY_EXTERNAL_ENTITY_IMPORT)) {
                    doExternalEntityImport = true;
                    externalEntityId = validateExternalEntityId(nameValue[0], values[0]);
                }
                else if (nameValue[0].equals(KEY_SEQUENCE_IMPORT)) {
                    doSequenceImport = validateBoolean(nameValue[0], values[0]);
                }
                else if (nameValue[0].equals(KEY_INTERNAL_ENTITY_IMPORT)) {
                    doInternalEntityImport = validateBoolean(nameValue[0], values[0]);
                }
                else if (nameValue[0].equals(KEY_GENERATE_DATASETS)) {
                    doBlastDBCreation = true;
                    populateIdList(KEY_GENERATE_DATASETS, values, idsOfBlastNodesToRegenerate);
                }
                else if (nameValue[0].equals(KEY_TRUNCATE)) {
                    truncateTables = values;
                }
                else if (nameValue[0].equals(KEY_RECREATE_INDEXES)) {
                    recreateIndexes = validateBoolean(nameValue[0], values[0]);
                }
                else if (nameValue[0].equals(KEY_CREATE_SCRATCH_TABLES)) {
                    createScratchTables = validateBoolean(nameValue[0], values[0]);
                }
                else if (nameValue[0].equals(KEY_ALL_REPORT)) {
                    allReport = validateBoolean(nameValue[0], values[0]);
                }
                else if (nameValue[0].equals(KEY_FILE_ORDER)) {
                    isAscFileOrder = validateFileOrder(nameValue[0], values[0]);
                }
                else if (nameValue[0].equals(KEY_CHCK_FOR_EXISTG_ENTITIES_IN_FASTA_IMPORT)) {
                    checkForExistingEntitiesDuringFastaImport = validateBoolean(nameValue[0], values[0]);
                }
                else if (nameValue[0].equals(KEY_KEEP_FASTA)) {
                    keepFasta = validateBoolean(nameValue[0], values[0]);
                }
                else {
                    throw new IllegalArgumentException("Invalid argument:" + nameValue[0] + getUsage());
                }
            }
        }
        else {
            throw new IllegalArgumentException("At least one argument must be specified" + getUsage());
        }
    }

    private void populateIdList(String arg, String[] values, Set<Long> ids) {
        if (values[0].equals("all")) {
            return;
        }
        if (values.length > 0) {
            for (String s : values) {
                try {
                    ids.add(Long.valueOf(s));
                }
                catch (NumberFormatException e) {
                    // Path supplied to ids file
                    try {
                        String[] idValues = FileUtil.getFileContentsAsString(s.trim()).trim().split(",");
                        populateIdList(arg, idValues, ids);
                    }
                    catch (IOException ioe) {
                        throw new RuntimeException("populateIdList encountered exception processing " + arg, ioe);
                    }
                }
            }
        }
    }

    private boolean validateFileOrder(String name, String value) {
        if (value.equals("asc") || value.equals("desc")) {
            return value.equals("asc");
        }
        else {
            throw new IllegalArgumentException(value + " is invalid for " + name + ".  Valid values include asc or desc");
        }
    }

    private Long validateExternalEntityId(String name, String value) {
        if (!value.equals("all")) {
            try {
                return Long.valueOf(value);
            }
            catch (NumberFormatException e) {
                throw new IllegalArgumentException(value + " is invalid for " + name + ".  Valid values include all or a Long");
            }
        }
        return null;
    }

    private boolean validateBoolean(String name, String value) {
        if (value.equals("true") || value.equals("false")) {
            return Boolean.valueOf(value);
        }
        else {
            throw new IllegalArgumentException(value + " is invalid for " + name + ".  Valid values include true or false");
        }
    }

    private String[] getArgValues(String value) {
        if (value == null || value.length() == 0) {
            throw new IllegalArgumentException("value cannot be null or empty for a parameter");
        }
        return value.split(",");
    }


    public boolean doFastaImport() {
        return doFastaImport;
    }

    public void setDoFastaImport(boolean doFastaImport) {
        this.doFastaImport = doFastaImport;
    }

    public boolean doSequenceImport() {
        return doSequenceImport;
    }

    public boolean doInternalEntityImport() {
        return doInternalEntityImport;
    }

    public boolean doExternalEntityImport() {
        return doExternalEntityImport;
    }

    public boolean doBlastDBCreation() {
        return doBlastDBCreation;
    }

    public boolean allReport() {
        return allReport;
    }

    public void setAllReport(boolean allReport) {
        this.allReport = allReport;
    }

    public String[] getTruncateTables() {
        return truncateTables;
    }

    public void setTruncateTables(String[] truncateTables) {
        this.truncateTables = truncateTables;
    }

    public boolean recreateIndexes() {
        return recreateIndexes;
    }

    public void setRecreateIndexes(boolean recreateIndexes) {
        this.recreateIndexes = recreateIndexes;
    }

    public Set<Long> getIdsOfBlastNodesToRegenerate() {
        return idsOfBlastNodesToRegenerate;
    }

    public void setIdsOfBlastNodesToRegenerate(Set<Long> idsOfBlastNodesToRegenerate) {
        this.idsOfBlastNodesToRegenerate = idsOfBlastNodesToRegenerate;
    }

    public boolean createScratchTables() {
        return createScratchTables;
    }

    public void setCreateScratchTables(boolean createScratchTables) {
        this.createScratchTables = createScratchTables;
    }

    public String[] getFastaInputs() {
        return fastaInputs;
    }

    public void setFastaInputs(String[] fastaInputs) {
        this.fastaInputs = fastaInputs;
    }

    public boolean checkForExistingEntitiesDuringFastaImport() {
        return checkForExistingEntitiesDuringFastaImport;
    }

    public boolean keepFasta() {
        return keepFasta;
    }

    public void setKeepFasta(boolean keepFasta) {
        this.keepFasta = keepFasta;
    }

    private static String getUsage() throws IOException {
        return FileUtil.getResourceAsString("dma_usage.txt");
    }

    /**
     */
    public static String getKeyAndDescription(String paramKey, String description, Object defaultValue) {
        StringBuffer paddedVal = new StringBuffer();
        int numCharsToPad = 25 - paramKey.length();
        paddedVal.append("\n");
        paddedVal.append(paramKey);
        for (int i = 0; i < numCharsToPad; ++i) {
            paddedVal.append(" ");
        }
        paddedVal.append("\t");
        paddedVal.append(description);
        numCharsToPad = 70 - description.length();
        for (int i = 0; i < numCharsToPad; ++i) {
            paddedVal.append(" ");
        }
        paddedVal.append(defaultValue.toString());
        return paddedVal.toString();
    }

    public Long getExternalEntityId() {
        return externalEntityId;
    }

    public void setExternalEntityId(Long externalEntityId) {
        this.externalEntityId = externalEntityId;
    }

    public boolean isAscFileOrder() {
        return isAscFileOrder;
    }

}
