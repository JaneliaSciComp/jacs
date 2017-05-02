package org.janelia.jacs2.dao;

import org.janelia.it.jacs.model.domain.Subject;
import org.janelia.it.jacs.model.domain.sample.DataSet;

public interface DatasetDao extends DomainObjectDao<DataSet> {
    DataSet findByName(Subject subject, String datasetName);
}
