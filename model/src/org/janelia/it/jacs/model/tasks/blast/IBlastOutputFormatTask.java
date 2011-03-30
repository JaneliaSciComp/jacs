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
