
package org.janelia.it.jacs.model.tasks.eukAnnotation;

import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.vo.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Dec 15, 2008
 * Time: 3:03:13 PM
 */
public class EukaryoticAnnotationTask extends Task {
    transient public static final String TASK_NAME = "eukAnnotation";
    transient public static final String DISPLAY_NAME = "Eukaryotic Annotation Pipeline";

    // Parameter Keys
    transient public static final String PARAM_fragmentFiles = "fragment files";
    transient public static final String PARAM_subjectDatabase = "subject database";
    transient public static final String PARAM_filenamePrefix = "filename prefix";
    transient public static final String PARAM_skipClustalW = "skip clustalw step";
    transient public static final String PARAM_iterateCdHitESTClustering = "iterate cd-hit-est based clustering";
    transient public static final String PARAM_useMsuRdpClassifier = "use MSU RDP Classifier";
    transient public static final String PARAM_useRdpMassaging = "use RDP Massaging";

    // Default values - default overrides
    transient public static final String filenamePrefix_DEFAULT = "mySample";

    public EukaryoticAnnotationTask(Set<Node> inputNodes, String owner, List<Event> events, Set<TaskParameter> taskParameterSet) {
        super(inputNodes, owner, events, taskParameterSet);
        setDefaultValues();
    }

    private void setDefaultValues() {
        setParameter(PARAM_fragmentFiles, "");
        setParameter(PARAM_subjectDatabase, "");
        setParameter(PARAM_filenamePrefix, filenamePrefix_DEFAULT);
        setParameter(PARAM_skipClustalW, Boolean.TRUE.toString());
        setParameter(PARAM_iterateCdHitESTClustering, Boolean.FALSE.toString());
        setParameter(PARAM_useMsuRdpClassifier, Boolean.TRUE.toString());
        setParameter(PARAM_useRdpMassaging, Boolean.TRUE.toString());
        this.taskName = TASK_NAME;
    }

    public EukaryoticAnnotationTask() {
        setDefaultValues();
    }

    public ParameterVO getParameterVO(String key) throws ParameterException {
        if (key == null)
            return null;
        String value = getParameter(key);
        if (value == null)
            return null;
        if (key.equals(PARAM_skipClustalW) || key.equals(PARAM_iterateCdHitESTClustering) ||
                key.equals(PARAM_useMsuRdpClassifier) || key.equals(PARAM_useRdpMassaging)) {
            return new BooleanParameterVO(Boolean.valueOf(value));
        }
        if (key.equals(PARAM_filenamePrefix)) {
            return new TextParameterVO(value, 100);
        }
        if (key.equals(PARAM_fragmentFiles)) {
            return new MultiSelectVO(listOfStringsFromCsvString(value), listOfStringsFromCsvString(value));
        }
        if (key.equals(PARAM_subjectDatabase)) {
            return new SingleSelectVO(getSubjectDatabaseList(), value);
        }
        // No match
        return null;
    }

    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    public boolean isParameterRequired(String parameterKeyName) {
        return super.isParameterRequired(parameterKeyName);
    }

    public List<String> getSubjectDatabaseList() {
        ArrayList<String> tmpList = new ArrayList<String>();
        tmpList.add("archaeal16S");
        tmpList.add("bact16S");
        tmpList.add("chloroplast16S");
        tmpList.add("fungal18S");
        tmpList.add("s12sb18S");
        return tmpList;
    }
}