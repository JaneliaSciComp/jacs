
package org.janelia.it.jacs.model.tasks.recruitment;

import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.vo.*;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Apr 17, 2007
 * Time: 1:05:55 PM
 */
public class RecruitmentViewerFilterDataTask extends RecruitmentViewerTask {

    public static final String DISPLAY_NAME = "Recruitment Viewer Filter Data";
    public static final String NUM_HITS = "numHits";
    public static final String PERCENT_ID_MIN = "pidMin";
    public static final String PERCENT_ID_MAX = "pidMax";
    public static final String REF_BEGIN_COORD = "refBegin";
    public static final String REF_END_COORD = "refEnd";
    public static final String MATE_BITS = "mateBit";
    public static final String SAMPLE_LIST = "sampleList";
    public static final String INITIAL_MATE_BITS = "1111111111111111";
    public static final String ANNOTATION_FILTER_STRING = "annotFilter";
    public static final String MATE_SPAN_POINT = "mateSpanPoint";
    public static final String COLORIZATION_TYPE = "colorizationType";
    public static final String COLORIZATION_SAMPLE = "sample";
    public static final String COLORIZATION_MATE = "mate";

    public RecruitmentViewerFilterDataTask() {
        super();
    }

    // NOTE:  Potential problem exists when Parameters are initially different than the setParameters below. 
    public RecruitmentViewerFilterDataTask(Set inputNodes, String owner, List events, Set parameters, String subject,
                                           String query, Long numHits, int percentIDMin, int percentIDMax,
                                           double referenceBeginCoordinate, double referenceEndCoordinate, List sampleList,
                                           String mateBits, String annotationFilterString, String mateSpanPoint, String colorizationType) {
        super(inputNodes, owner, events, parameters, subject, query);
        setParameter(PERCENT_ID_MIN, Double.toString(percentIDMin));
        setParameter(PERCENT_ID_MAX, Double.toString(percentIDMax));
        setParameter(REF_BEGIN_COORD, Double.toString(referenceBeginCoordinate));
        setParameter(REF_END_COORD, Double.toString(referenceEndCoordinate));
        setParameter(NUM_HITS, numHits.toString());
        String sampleListString = csvStringFromCollection(sampleList);
        setParameter(SAMPLE_LIST, sampleListString);
        setParameter(MATE_BITS, mateBits);
        setParameter(ANNOTATION_FILTER_STRING, annotationFilterString);
        setParameter(MATE_SPAN_POINT, mateSpanPoint);
        setParameter(COLORIZATION_TYPE, colorizationType);
        this.taskName = "Recruitment Viewer Filter Data Task";
    }

    public ParameterVO getParameterVO(String key) throws ParameterException {
        if (key == null)
            return null;
        ParameterVO pvo = super.getParameterVO(key);
        if (pvo != null)
            return pvo;
        String value = getParameter(key);
        if (key.equals(PERCENT_ID_MIN))
            return new DoubleParameterVO(new Double(value));
        if (key.equals(PERCENT_ID_MAX))
            return new DoubleParameterVO(new Double(value));
        if (key.equals(REF_BEGIN_COORD))
            return new DoubleParameterVO(new Double(value));
        if (key.equals(REF_END_COORD))
            return new DoubleParameterVO(new Double(value));
        if (key.equals(NUM_HITS))
            return new LongParameterVO((long) 0, (long) 999999999, new Long(value));
        if (key.equals(SAMPLE_LIST))
            return new MultiSelectVO(Task.listOfStringsFromCsvString(value), Task.listOfStringsFromCsvString(value));
        if (key.equals(MATE_BITS))
            return new TextParameterVO(value, value.length());
        if (key.equals(ANNOTATION_FILTER_STRING)) {
            return new TextParameterVO(value, value.length());
        }
        if (key.equals(MATE_SPAN_POINT)) {
            return new TextParameterVO(value, value.length());
        }
        if (key.equals(COLORIZATION_TYPE)) {
            return new TextParameterVO(value, value.length());
        }
        return null;
    }

    public long getNumHits() throws ParameterException {
        return ((LongParameterVO) getParameterVO(NUM_HITS)).getActualValue();
    }

    public void setNumHits(Long newNumHits) {
        setParameter(NUM_HITS, newNumHits.toString());
    }

    public int getPercentIdMin() throws ParameterException {
        return ((DoubleParameterVO) getParameterVO(PERCENT_ID_MIN)).getActualValue().intValue();
    }

    public int getPercentIdMax() throws ParameterException {
        return ((DoubleParameterVO) getParameterVO(PERCENT_ID_MAX)).getActualValue().intValue();
    }

    public double getReferenceBegin() throws ParameterException {
        return ((DoubleParameterVO) getParameterVO(REF_BEGIN_COORD)).getActualValue();
    }

    public double getReferenceEnd() throws ParameterException {
        return ((DoubleParameterVO) getParameterVO(REF_END_COORD)).getActualValue();
    }

    public List getSampleList() throws ParameterException {
        return ((MultiSelectVO) getParameterVO(SAMPLE_LIST)).getActualUserChoices();
    }

    public String getSampleListAsCommaSeparatedString() throws ParameterException {
        StringBuffer sbuf = new StringBuffer();
        for (Iterator iterator = ((MultiSelectVO) this.getParameterVO(SAMPLE_LIST)).getActualUserChoices().iterator(); iterator.hasNext();) {
            String sample = (String) iterator.next();
            sbuf.append(sample);
            if (iterator.hasNext()) {
                sbuf.append(",");
            }
        }
        String returnString = sbuf.toString();
        if (null != returnString && returnString.endsWith(",")) {
            returnString = returnString.substring(0, returnString.length() - 1);
        }
        return returnString;
    }

    public String getMateBits() throws ParameterException {
        return getParameter(MATE_BITS);
    }

    public String getAnnotationFilterString() throws ParameterException {
        return getParameter(ANNOTATION_FILTER_STRING);
    }

    public String getMateSpanPoint() {
        return getParameter(MATE_SPAN_POINT);
    }

    public String getColorizationType() {
        return getParameter(COLORIZATION_TYPE);
    }
}
