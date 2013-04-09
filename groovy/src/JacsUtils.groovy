import org.janelia.it.FlyWorkstation.api.facade.concrete_facade.ejb.EJBFacadeManager;
import org.janelia.it.FlyWorkstation.api.facade.concrete_facade.ejb.EJBFactory
import org.janelia.it.FlyWorkstation.api.facade.facade_mgr.FacadeManager;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr
import org.janelia.it.jacs.compute.api.AnnotationBeanRemote
import org.janelia.it.jacs.compute.api.ComputeBeanRemote
import org.janelia.it.jacs.compute.api.EntityBeanRemote
import org.janelia.it.jacs.compute.api.SolrBeanRemote;
import org.janelia.it.jacs.compute.api.support.SolrResults;
import org.janelia.it.jacs.model.entity.Entity
import org.janelia.it.jacs.model.entity.EntityData
import org.janelia.it.jacs.model.user_data.Subject
import org.janelia.it.jacs.shared.utils.EntityUtils
import static org.janelia.it.jacs.model.entity.EntityConstants.*

class JacsUtils {

	EntityBeanRemote e
	AnnotationBeanRemote a
	ComputeBeanRemote c
	SolrBeanRemote s
	Subject subject
	Date createDate
	boolean persist
	
	JacsUtils(String subjectNameOrKey) {
		this(subjectNameOrKey, true)
	}
		
	JacsUtils(String subjectNameOrKey, boolean persist) {
		this.persist = persist
		
		FacadeManager.registerFacade(FacadeManager.getEJBProtocolString(), EJBFacadeManager.class, "JACS EJB Facade Manager");
		// This initializes the EJBFactory
		SessionMgr.getSessionMgr();
		
		this.e = EJBFactory.getRemoteEntityBean()
		this.a = EJBFactory.getRemoteAnnotationBean()
		this.c = EJBFactory.getRemoteComputeBean(true)
		this.s = EJBFactory.getRemoteSolrBean()
		this.subject = c.getSubjectByNameOrKey(subjectNameOrKey)
		this.createDate = new Date()
	}
	
	def SolrResults search(solrQuery) {
		return s.search(solrQuery, false)
	}
	
	def Entity save(Entity entity) {
		if (!persist) return entity
		e.saveOrUpdateEntity(entity)
	}
	
	def EntityData save(EntityData ed) {
		if (!persist) return ed
		e.saveOrUpdateEntityData(ed)
	}
	
	def Entity loadChildren(Entity entity) {
		if (entity==null || entity.id==null) return entity
		EntityUtils.replaceChildNodes(entity, e.getChildEntities(subject.key, entity.id))
		return entity
	}
	
	def verifyOrCreateChildFolder(parentFolder, childName) {
		def folder = parentFolder.children.find({ it.entityType.name==TYPE_FOLDER && it.name==childName })
		if (folder == null) {
			folder = newEntity(childName, TYPE_FOLDER)
			folder = save(folder)
			println "Saved folder "+childName+" as "+folder.id
			addToParent(parentFolder, folder, null, ATTRIBUTE_ENTITY)
		}
		return folder
	}
	
	def Entity createSample(name, tilingPattern) {
		Entity sample = newEntity(name, TYPE_SAMPLE)
		sample.setValueByAttributeName(ATTRIBUTE_TILING_PATTERN, tilingPattern)
		sample = save(sample)
		println "Saved sample as "+sample.id
		return sample
	}
	
	def Entity createSupportingFilesFolder() {
		Entity filesFolder = newEntity("Supporting Files", TYPE_SUPPORTING_DATA)
		filesFolder = save(filesFolder)
		println "Saved supporting files folder as "+filesFolder.id
		return filesFolder
	}
	
	def Entity newEntity(name, entityTypeName) {
		Entity entity = new Entity()
		entity.name = name
		entity.ownerKey = subject.key
		entity.creationDate = createDate
		entity.updatedDate = createDate
		entity.entityType = e.getEntityTypeByName(entityTypeName)
		return entity
	}
	
	def EntityData addToParent(Entity parent, Entity entity) {
		return addToParent(parent, entity, parent.maxOrderIndex+1, ATTRIBUTE_ENTITY)
	}
	
	def EntityData addToParent(Entity parent, Entity entity, Integer index, String attrName) {
		EntityData ed = parent.addChildEntity(entity, attrName)
		ed.orderIndex = index
		save(ed)
		println "Added "+entity.entityType.name+"#"+entity.id+" as child of "+parent.entityType.name+"#"+parent.id;
		return ed
	}
	
}
