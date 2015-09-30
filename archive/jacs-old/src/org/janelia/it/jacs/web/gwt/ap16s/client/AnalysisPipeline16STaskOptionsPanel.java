
package org.janelia.it.jacs.web.gwt.ap16s.client;

import org.janelia.it.jacs.model.tasks.ap16s.AnalysisPipeline16sTask;
import org.janelia.it.jacs.model.vo.ParameterVO;
import org.janelia.it.jacs.web.gwt.common.client.panel.user.TaskOptionsPanel;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Nov 24, 2008
 * Time: 4:34:47 PM
 */
public class AnalysisPipeline16STaskOptionsPanel extends TaskOptionsPanel {
    protected boolean displayParameter(String parameterKeyName, ParameterVO tmpParam) {
        return super.displayParameter(parameterKeyName, tmpParam) && !(
                AnalysisPipeline16sTask.PARAM_fragmentFiles.equalsIgnoreCase(parameterKeyName) ||
                        AnalysisPipeline16sTask.PARAM_subjectDatabase.equalsIgnoreCase(parameterKeyName) ||
                        AnalysisPipeline16sTask.PARAM_ampliconSize.equalsIgnoreCase(parameterKeyName) ||
                        AnalysisPipeline16sTask.PARAM_primer1Defline.equalsIgnoreCase(parameterKeyName) ||
                        AnalysisPipeline16sTask.PARAM_primer1Sequence.equalsIgnoreCase(parameterKeyName) ||
                        AnalysisPipeline16sTask.PARAM_primer2Defline.equalsIgnoreCase(parameterKeyName) ||
                        AnalysisPipeline16sTask.PARAM_primer2Sequence.equalsIgnoreCase(parameterKeyName) ||
                        AnalysisPipeline16sTask.PARAM_readLengthMinimum.equalsIgnoreCase(parameterKeyName) ||
                        AnalysisPipeline16sTask.PARAM_minAvgQV.equalsIgnoreCase(parameterKeyName) ||
                        AnalysisPipeline16sTask.PARAM_maxNCount.equalsIgnoreCase(parameterKeyName) ||
                        AnalysisPipeline16sTask.PARAM_qualFile.equalsIgnoreCase(parameterKeyName) ||
                        AnalysisPipeline16sTask.PARAM_minIdentCount.equalsIgnoreCase(parameterKeyName)
        );
    }
}