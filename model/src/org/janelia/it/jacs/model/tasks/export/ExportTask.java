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

package org.janelia.it.jacs.model.tasks.export;

import com.google.gwt.user.client.rpc.IsSerializable;
import org.janelia.it.jacs.model.common.SortArgument;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.vo.MultiSelectVO;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.model.vo.ParameterVO;
import org.janelia.it.jacs.model.vo.TextParameterVO;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Jun 20, 2008
 * Time: 2:25:36 PM
 */
public abstract class ExportTask extends Task implements IsSerializable, Serializable {

    public static final String COMPRESSION_NONE = "None";

    public static final String PARAM_EXPORT_FORMAT_TYPE = "exportFormatType";
    public static final String PARAM_ACCESSION_LIST = "accessionList";
    public static final String PARAM_EXPORT_ATTRIBUTE_LIST = "exportAttributeList";
    public static final String PARAM_SUGGESTED_FILENAME = "suggestedFilename";
    public static final String PARAM_SUGGESTED_COMPRESSION_TYPE = "suggestedCompressionType";

    public ExportTask() {
    }

    public ExportTask(String exportFormatType, List<String> accessionList,
                      List<SortArgument> exportAttributeList) {
        setParameter(PARAM_EXPORT_FORMAT_TYPE, exportFormatType);
        setParameter(PARAM_ACCESSION_LIST, csvStringFromList(accessionList));
        setParameter(PARAM_SUGGESTED_COMPRESSION_TYPE, COMPRESSION_NONE);
        setParameter(PARAM_EXPORT_ATTRIBUTE_LIST, csvStringFromSortArgumentList(exportAttributeList));
    }

    protected String formatSuggestedFilename(String suggestedFilename) {
        return suggestedFilename;
    }

    public abstract String getDataType();

    public ParameterVO getParameterVO(String key) throws ParameterException {
        if (key == null)
            return null;
        String value = getParameter(key);
        if (value == null)
            return null;
        if (key.equals(PARAM_ACCESSION_LIST)) {
            return new MultiSelectVO(listOfStringsFromCsvString(value), listOfStringsFromCsvString(value));
        }
        if (key.equals(PARAM_EXPORT_ATTRIBUTE_LIST)) {
            return new MultiSelectVO(listOfStringsFromCsvString(value), listOfStringsFromCsvString(value));
        }
        if (key.equals(PARAM_EXPORT_FORMAT_TYPE)) {
            return new TextParameterVO(value);
        }
        if (key.equals(PARAM_SUGGESTED_COMPRESSION_TYPE)) {
            return new TextParameterVO(value);
        }
        if (key.equals(PARAM_SUGGESTED_FILENAME)) {
            return new TextParameterVO(value);
        }
        // No match
        return null;
    }

    public String getExportFormatType() {
        return getParameter(PARAM_EXPORT_FORMAT_TYPE);
    }

    public List<String> getAccessionList() {
        return listOfStringsFromCsvString(getParameter(PARAM_ACCESSION_LIST));
    }

    public List<String> getExportAttributeList() {
        return listOfStringsFromCsvString(getParameter(PARAM_EXPORT_ATTRIBUTE_LIST));
    }

    // This only captures name not sort values, etc
    // The toString on SortArgument captures the direction but there is no way to provide "<name> asc" and reconstitute the SortArgument object
    public List<SortArgument> getSortArguments() {
        List<String> getExportAttributeList = getExportAttributeList();
        if (getExportAttributeList == null) {
            return null;
        }
        else {
            ArrayList<SortArgument> saList = new ArrayList<SortArgument>();
            for (String s : getExportAttributeList) {
                saList.add(new SortArgument(s));
            }
            return saList;
        }
    }

    public String getSuggestedCompressionType() {
        return getParameter(PARAM_SUGGESTED_COMPRESSION_TYPE);
    }

    public void setSuggestedCompressionType(String suggestedCompressionType) {
        setParameter(PARAM_SUGGESTED_COMPRESSION_TYPE, suggestedCompressionType);
    }

    public String getSuggestedFilename() {
        return getParameter(PARAM_SUGGESTED_FILENAME);
    }

    protected String csvStringFromSortArgumentList(List<SortArgument> sortArgs) {
        StringBuffer sb = new StringBuffer("");
        if (sortArgs != null) {
            for (int i = 0; i < sortArgs.size(); i++) {
                SortArgument s = sortArgs.get(i);
                if (i != 0)
                    sb.append(",");
                sb.append(s.getSortArgumentName());
            }
        }
        return sb.toString();
    }
}
