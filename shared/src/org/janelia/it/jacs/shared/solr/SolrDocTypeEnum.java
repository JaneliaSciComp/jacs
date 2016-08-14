package org.janelia.it.jacs.shared.solr;

/**
 * Enumeration of the types of documents stored in Solr. This type is stored in the "doc_type" field.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public enum SolrDocTypeEnum {
	ENTITY,
	DOCUMENT,
	SAGE_TERM
}