package entity

import org.janelia.it.jacs.model.entity.Entity
import org.janelia.it.jacs.shared.utils.entity.EntityVisitor
import org.janelia.it.jacs.shared.utils.entity.EntityVistationBuilder
import org.janelia.it.jacs.model.entity.EntityConstants

class AnnotatedSampleExportScript {
 
    private static final String OWNER_KEY = "user:leey10"
	private static final String GRANTEE_KEY = "user:aboucharl"
    private JacsUtils f
    
    public AnnotatedSampleExportScript() {
        this.f = new JacsUtils(OWNER_KEY, false)
    }
    
    public void run() {
		long folderId = 2153965520716562577L;
		
		NeuronWalker walker = new NeuronWalker(f);
		
		Entity folder = f.e.getEntityById(OWNER_KEY, folderId);
		f.loadChildren(folder)
		for(Entity child : folder.getOrderedChildren()) {
			
			//f.e.grantPermissions(child.ownerKey, child.id, GRANTEE_KEY, "r", true);
			
			f.loadChildren(child)
			walker.walk(child)
		}
    }
	
	class NeuronWalker {
	
		JacsUtils f
		Entity sample;
		Entity neuronFragments;
	
		public NeuronWalker(JacsUtils f) {
			this.f = f
		}
	
		def walk(Entity sample) {
			this.sample = sample
			this.neuronFragments = null
			EntityVistationBuilder.create(f.getEntityLoader()).startAt(sample)
					.childrenOfType(EntityConstants.TYPE_PIPELINE_RUN).last()
					.childrenOfType(EntityConstants.TYPE_SAMPLE_PROCESSING_RESULT).last()
					.childrenOfType(EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT).last()
					.childOfName("Neuron Fragments")
					.run(new EntityVisitor() {
				public void visit(Entity entity) throws Exception {
					neuronFragments = entity
				}
			});
		
		
			List<Entity> sampleAnnotations = f.a.getAnnotationsForEntity(null, sample.id)
			println sample.name+"\t"+getCSV(sampleAnnotations)
			
			if (neuronFragments!=null) {
				f.loadChildren(neuronFragments)
				for(Entity fragment : neuronFragments.getOrderedChildren()) {
					List<Entity> neuronAnnotations = f.a.getAnnotationsForEntity(null, fragment.id)
					println "\t"+fragment.name+"\t"+getCSV(neuronAnnotations)
				}
			}
		}
		
		private String getCSV(List<Entity> entities) {
			StringBuilder sb = new StringBuilder();
			for (Entity entity : entities) {
				if (sb.length()>0) sb.append(", ")
				sb.append(entity.name)
			}
			return sb.toString()
		}
		
	}
}
    
AnnotatedSampleExportScript script = new AnnotatedSampleExportScript()
script.run()
System.exit(0)