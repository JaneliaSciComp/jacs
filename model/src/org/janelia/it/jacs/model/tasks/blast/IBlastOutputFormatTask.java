
package org.janelia.it.jacs.model.tasks.blast;

import org.janelia.it.jacs.model.tasks.ITask;
import org.janelia.it.jacs.model.vo.ParameterException;

import java.math.BigInteger;

/**
 * User: aresnick
 * Date: Jun 23, 2009
 * Time: 9:01:46 AM
 * <p/>
 * Description:
 */
public interface IBlastOutputFormatTask extends ITask {
    // methods required to return non null values to avoid BlastXMLWriter null pointer errors
    public Double getExpectationValue() throws ParameterException; // (Double.valueOf(task.getParameterVO(BlastTask.PARAM_evalue).getStringValue()));

    public String getFilterQuerySequence() throws ParameterException; // jbParametersType.setParametersFilter(task.getParameterVO(BlastTask.PARAM_filter).getStringValue());

    public BigInteger getGapExtensionCost() throws ParameterException; // jbParametersType.setParametersGapExtend(BigInteger.valueOf(Long.valueOf(task.getParameterVO(BlastTask.PARAM_gapExtendCost).getStringValue())));

    public BigInteger getGapOpeningCost() throws ParameterException; // jbParametersType.setParametersGapOpen(BigInteger.valueOf(Long.valueOf(task.getParameterVO(BlastTask.PARAM_gapOpenCost).getStringValue())));

    public String getMatrix() throws ParameterException; // jbParametersType.setParametersMatrix(task.getParameterVO(BlastTask.PARAM_matrix).getStringValue());

    // methods can return nulls without risk of BlastXMLWriter null pointer errors
    public BigInteger getMatchReward() throws ParameterException; // jbParametersType.setParametersScMatch(BigInteger.valueOf(Long.valueOf(task.getParameterVO(BlastNTask.PARAM_matchReward).getStringValue())));

    public BigInteger getMismatchPenalty() throws ParameterException; // jbParametersType.setParametersScMismatch(BigInteger.valueOf(Long.valueOf(task.getParameterVO(BlastNTask.PARAM_mismatchPenalty).getStringValue())));
}
