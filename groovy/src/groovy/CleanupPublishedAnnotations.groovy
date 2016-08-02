import org.janelia.it.jacs.model.domain.ontology.Annotation
import org.janelia.it.jacs.model.domain.ontology.Ontology
import org.janelia.it.jacs.model.domain.ontology.OntologyTerm
import org.janelia.it.jacs.model.domain.ontology.OntologyTermReference
import org.janelia.it.jacs.model.domain.sample.*
import org.janelia.it.jacs.model.domain.support.DomainDAO

/**
 * Clean up existing published annotations which were not migrated correctly to NG.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
class CleanupPublishedAnnotationsScript {
	
	public static final String ANNOTATION_EXPORT_20X = "Publish20xToMBEW";
	public static final String ANNOTATION_EXPORT_63X = "Publish63xToMBEW";
	public static final String ANNOTATION_EXPORTED_20X = "Published20xToWeb";
	public static final String ANNOTATION_EXPORTED_63X = "Published63xToWeb";
    public static final String PUBLICATION_OWNER = "group:workstation_users";
    public static final String PUBLICATION_ONTOLOGY_NAME = "Publication";
	
	private Ontology publicationOntology;
	private OntologyTerm publishedTerm20x;
	private OntologyTerm publishedTerm63x;
	
    private boolean DEBUG = true
    private DomainDAO dao = DomainDAOManager.instance.dao
	private String ownerKey = "user:asoy"
	
    public void run() {
		
		this.publicationOntology = getPublicationOntology();
		this.publishedTerm20x = getPublishedTerm(publicationOntology, ANNOTATION_EXPORTED_20X);
		this.publishedTerm63x = getPublishedTerm(publicationOntology, ANNOTATION_EXPORTED_63X);
		
        for(Sample sample : dao.getCollectionByClass(Sample.class).find("{ownerKey:#}", ownerKey).as(Sample.class)) {
			
			Annotation published = null
			boolean publish20x = false
			boolean publish63x = false
			
			for(Annotation annotation : dao.getAnnotations(null, org.janelia.it.jacs.model.domain.Reference.createFor(sample))) {
				if (annotation.getName().equals(ANNOTATION_EXPORT_20X)) {
					publish20x = true
				}
				else if (annotation.getName().equals(ANNOTATION_EXPORT_63X)) {
					publish63x = true
				}
				else if (annotation.getName().startsWith("Published")) {
					published = annotation
				}
			}
			
			if (published!=null) {
				println(sample.name+ " - changing existing single annotation ('"+published.name+"') to: ")
				
				if (publish20x) {
					println("  "+ANNOTATION_EXPORTED_20X)
					if (!DEBUG) {
						dao.createAnnotation(PUBLICATION_OWNER, published.target, OntologyTermReference.createFor(publishedTerm20x), null)
					}
				}
				if (publish20x) {
					println("  "+ANNOTATION_EXPORTED_63X)
					if (!DEBUG) {
						dao.createAnnotation(PUBLICATION_OWNER, published.target, OntologyTermReference.createFor(publishedTerm63x), null)
					}
				}
				
				if (!DEBUG) {
					dao.remove(published.ownerKey, published)
				}
				
			}
			else if (publish20x || publish63x) {
				println(sample.name+" - sample should be published but isn't!")
			}
		
        }
    }

    private Ontology getPublicationOntology() throws Exception {

        Ontology publicationOntology = null;
        for(Ontology ontology : dao.getDomainObjectsByName(PUBLICATION_OWNER, Ontology.class, PUBLICATION_ONTOLOGY_NAME)) {
            if (publicationOntology==null) {
                publicationOntology = ontology;
            }
            else {
                println("More than one ontology found! Make sure that "+PUBLICATION_OWNER+" only has a single Ontology named "+PUBLICATION_ONTOLOGY_NAME);
            }
        }
        
        if (publicationOntology!=null) { 
            return publicationOntology;
        }
        else {
            throw new IllegalStateException("No publication ontology found. Make sure that "+PUBLICATION_OWNER+" has an Ontology named "+PUBLICATION_ONTOLOGY_NAME);
        }
    }
    
    private OntologyTerm getPublishedTerm(Ontology publicationOntology, String termName) {
        OntologyTerm publishedTerm = publicationOntology.findTerm(termName);
        
        if (publishedTerm==null) {
            throw new IllegalStateException("No ontology term owned by "+PUBLICATION_OWNER+" was found with name '"+termName+"'");
        }
        
        return publishedTerm;
    }
}

new CleanupPublishedAnnotationsScript().run()