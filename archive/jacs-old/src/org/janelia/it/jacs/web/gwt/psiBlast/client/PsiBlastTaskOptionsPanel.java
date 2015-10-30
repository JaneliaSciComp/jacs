
package org.janelia.it.jacs.web.gwt.psiBlast.client;

import org.janelia.it.jacs.model.vo.ParameterVO;
import org.janelia.it.jacs.web.gwt.blast.client.panel.BlastTaskOptionsPanel;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Nov 24, 2008
 * Time: 4:34:47 PM
 */
public class PsiBlastTaskOptionsPanel extends BlastTaskOptionsPanel {
    protected boolean displayParameter(String parameterKeyName, ParameterVO tmpParam) {
        return super.displayParameter(parameterKeyName, tmpParam) /*&& !(
                PsiBlastTask.PARAM_query_node_id.equalsIgnoreCase(parameterKeyName) ||
                PsiBlastTask.PARAM_subjectDatabases.equalsIgnoreCase(parameterKeyName) ||
                PsiBlastTask.PARAM_alignmentViewOptions.equalsIgnoreCase(parameterKeyName) ||
                PsiBlastTask.PARAM_databaseSize.equalsIgnoreCase(parameterKeyName) ||
                PsiBlastTask.PARAM_outputFileName.equalsIgnoreCase(parameterKeyName) ||
                PsiBlastTask.PARAM_querySequenceProtein.equalsIgnoreCase(parameterKeyName)||
                PsiBlastTask.PARAM_loopParameter.equalsIgnoreCase(parameterKeyName))*/;
    }
}