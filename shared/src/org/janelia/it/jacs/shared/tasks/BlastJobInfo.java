
package org.janelia.it.jacs.shared.tasks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Michael Press
 */
public class BlastJobInfo extends JobInfo {
    private String _defline;
    private boolean subjectWithSample;
    private String _program;
    private String _queryNodeId;
    private Map<String, String> _subjectIdsToNames = new HashMap<String, String>();

    // Has to be String and not numeric !!! Known GWT issue where it rounds numbers it can't handle
    private String blastResultFileNodeId;

    public static final String SORT_BY_NUM_HITS = "hits";
    public static final String SORT_BY_QUERY_SEQ = "querySeq";
    public static final String SORT_BY_SUBJECT_DB = "subjDB";
    public static final String SORT_BY_DEFLINE = "defline";
    public static final String SORT_BY_PROGRAM = "program";

    public BlastJobInfo() {
        super();
    }

    /**
     * Map of subject ids onto names.
     *
     * @return desired map (subject id, name)
     */
    public Map<String, String> getSubjectIdsToNames() {
        return _subjectIdsToNames;
    }

    public void setSubjectIdsToNames(Map<String, String> subjectIdsToNames) {
        if (subjectIdsToNames != null) {
            this._subjectIdsToNames = subjectIdsToNames;
        }
    }

    public List<String> getAllSubjectNames() {
        return new ArrayList<String>(_subjectIdsToNames.values());
    }

    public void addSubject(String subjectId, String subjectName) {
        if (subjectName != null && subjectName.length() > 0) {
            _subjectIdsToNames.put(subjectId, subjectName);
        }
    }

    public void setQueryDefline(String defline) {
        _defline = defline;
    }

    public String getDefline() {
        return _defline;
    }

    public void setDefline(String defline) {
        _defline = defline;
    }

    public String getProgram() {
        return _program;
    }

    public void setProgram(String program) {
        this._program = program;
    }

    public boolean isSubjectWithSample() {
        return subjectWithSample;
    }

    public void setSubjectWithSample(boolean subjectWithSample) {
        this.subjectWithSample = subjectWithSample;
    }

    public String getBlastResultFileNodeId() {
        return blastResultFileNodeId;
    }

    public void setBlastResultFileNodeId(String blastResultFileNodeId) {
        this.blastResultFileNodeId = blastResultFileNodeId;
    }

    public String getQueryNodeId() {
        return _queryNodeId;
    }

    public void setQueryNodeId(String QueryNodeId) {
        this._queryNodeId = QueryNodeId;
    }

}