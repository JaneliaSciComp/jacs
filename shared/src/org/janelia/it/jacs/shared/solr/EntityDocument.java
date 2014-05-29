package org.janelia.it.jacs.shared.solr;

import org.apache.solr.common.SolrDocument;
import org.janelia.it.jacs.model.entity.Entity;

/**
 * A holder for a SOLR document along with its associated Hibernate entity.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class EntityDocument {

	private Entity entity;
	private SolrDocument document;
	
	public EntityDocument(Entity entity, SolrDocument document) {
		this.entity = entity;
		this.document = document;
	}

	public Entity getEntity() {
		return entity;
	}

	public SolrDocument getDocument() {
		return document;
	}
}
