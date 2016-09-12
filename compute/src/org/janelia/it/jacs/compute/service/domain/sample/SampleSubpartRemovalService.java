/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.jacs.compute.service.domain.sample;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import java.util.Collection;
import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.service.domain.AbstractDomainService;
import org.janelia.it.jacs.model.domain.sample.Sample;

/**
 * Removes parts of the sample.  Time-of-writing, this means whole
 * anatomical areas.  Could be changed to tiles within such areas.
 *
 * @author fosterl
 */
public class SampleSubpartRemovalService extends AbstractDomainService {
    // comma-separated
    private static final String SUPBART_NAMES = "SUBPART_NAMES";
    
    private Long sampleId;
    private Collection<String> subpartNames;

    private Logger logger = Logger.getLogger(SampleSubpartRemovalService.class);

    @Override
    public void execute() {
        try {
            extractParameters(processData);
            
            // Get the sample.
            Sample sample = domainHelper.getRequiredSample(data);
            
            // Find things to omit from the sample.
            // @todo
            
            // Find the secondary artifacts on file system bound by things
            // we are omitting from our in-datastore model of the sample.
            // @todo
        } catch (Exception ex) {
            
        }
    }
    
    /** Pull all params of interest as field variables. */
    private void extractParameters(IProcessData processData) throws Exception {
        subpartNames = ImmutableList.copyOf(
                Splitter.on(',')
                        .trimResults()
                        .omitEmptyStrings()
                        .split((String) processData.getMandatoryItem(SUPBART_NAMES)));
    }
    
}
