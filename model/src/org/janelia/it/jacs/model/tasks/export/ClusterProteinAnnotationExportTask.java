
package org.janelia.it.jacs.model.tasks.export;

import org.janelia.it.jacs.model.common.SortArgument;
import org.janelia.it.jacs.model.vo.BooleanParameterVO;
import org.janelia.it.jacs.model.vo.LongParameterVO;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.model.vo.ParameterVO;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Jun 23, 2008
 * Time: 11:20:05 AM
 */
public class ClusterProteinAnnotationExportTask extends ExportTask {

    public static final String NR_ONLY = "nrOnly";
    public static final String ANNOTATION_ID = "annotationId";

    public ClusterProteinAnnotationExportTask() {
    }

    public ClusterProteinAnnotationExportTask(String annotationId, boolean isNROnly, String exportFormatType,
                                              List<String> accessionList,
                                              List<SortArgument> exportAttributeList) {
        super(exportFormatType, accessionList, exportAttributeList);
        setParameter(NR_ONLY, Boolean.toString(isNROnly));
        setParameter(ANNOTATION_ID, annotationId);
        String filename = "clusterAnnotation";
        if (accessionList != null && accessionList.get(0) != null)
            filename += ("_" + accessionList.get(0));
        setParameter(PARAM_SUGGESTED_FILENAME, formatSuggestedFilename(filename + "." + exportFormatType));
        this.taskName = "Cluster Protein Annotation Export Task";
    }

    public String getDataType() {
        return "ClusterProteinAnnotations";
    }

    public String getDisplayName() {
        return "Export Cluster Protein Annotation Results";
    }

    public ParameterVO getParameterVO(String key) throws ParameterException {
        if (key == null)
            return null;
        ParameterVO pvo = super.getParameterVO(key);
        if (pvo != null)
            return pvo;
        String value = getParameter(key);
        if (value == null)
            return null;
        if (key.equals(NR_ONLY)) {
            return new BooleanParameterVO(value.equalsIgnoreCase("true") ? Boolean.TRUE : Boolean.FALSE);
        }
        if (key.equals(ANNOTATION_ID)) {
            Long tmpLong = new Long(value);
            return new LongParameterVO(tmpLong, tmpLong, tmpLong);
        }
        // No match
        return null;
    }


}