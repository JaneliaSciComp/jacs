import org.janelia.it.jacs.model.entity.EntityConstants

import static org.janelia.it.jacs.model.entity.EntityConstants.*

import org.apache.log4j.Logger
import org.janelia.it.workstation.api.facade.concrete_facade.ejb.EJBFacadeManager
import org.janelia.it.workstation.api.facade.concrete_facade.ejb.EJBFactory
import org.janelia.it.workstation.api.facade.facade_mgr.FacadeManager
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr
import org.janelia.it.jacs.compute.api.AnnotationBeanRemote
import org.janelia.it.jacs.compute.api.ComputeBeanRemote
import org.janelia.it.jacs.compute.api.EntityBeanRemote
import org.janelia.it.jacs.compute.api.SolrBeanRemote
import org.janelia.it.jacs.shared.solr.SolrResults
import org.janelia.it.jacs.model.entity.Entity
import org.janelia.it.jacs.model.entity.EntityData
import org.janelia.it.jacs.model.user_data.Subject
import org.janelia.it.jacs.shared.utils.EntityUtils
import org.janelia.it.jacs.shared.utils.entity.AbstractEntityLoader

class JacsUtils {

	Logger logger = Logger.getLogger(JacsUtils.class);
	
	EntityBeanRemote e
	AnnotationBeanRemote a
	ComputeBeanRemote c
    ComputeBeanRemote cr
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
        SessionMgr.getSessionMgr().loginSubject();

		this.e = EJBFactory.getRemoteEntityBean()
		this.a = EJBFactory.getRemoteAnnotationBean()
		this.c = EJBFactory.getRemoteComputeBean(false)
        this.cr = EJBFactory.getRemoteComputeBean(true)
		this.s = EJBFactory.getRemoteSolrBean()
		this.subject = c.getSubjectByNameOrKey(subjectNameOrKey)
		this.createDate = new Date()
	}
	
	def SolrResults search(solrQuery) {
		return s.search(solrQuery, false)
	}
	
	def Entity save(Entity entity) {
		if (!persist) return entity
		e.saveOrUpdateEntity(subject.key, entity)
	}
	
	def EntityData save(EntityData ed) {
		if (!persist) return ed
		e.saveOrUpdateEntityData(subject.key, ed)
	}

    def deleteEntityTree(Long entityId) {
        if (persist) {
            e.deleteEntityTreeById(subject.key, entityId)
            println "Deleted entity tree with id "+entityId
        }
    }
	
	def Entity loadChildren(Entity entity) {
		if (entity==null || entity.id==null) return entity
		EntityUtils.replaceChildNodes(entity, e.getChildEntities(subject==null?entity.ownerKey:subject.key, entity.id))
		return entity
	}

    def getRootEntity(String topLevelFolderName) {
        return getRootEntity(subject.key, topLevelFolderName);
    }

    def getRootEntity(String ownerKey, String topLevelFolderName) {
        for(Entity commonRoot : a.getCommonRootEntities(ownerKey)) {
            if (commonRoot.name.equals(topLevelFolderName)) {
                return commonRoot;
            }
        }
        return null;
    }

    def createRootEntity(String topLevelFolderName) {
        if (persist) {
            return e.createFolderInWorkspace(subject.key, e.getDefaultWorkspace(subject.key).getId(), topLevelFolderName).getChildEntity();
        }
        return null
    }

	def verifyOrCreateChildFolder(parentFolder, childName) {
		def folder = parentFolder.children.find({ it.entityTypeName==TYPE_FOLDER && it.name==childName })
		if (folder == null) {
			folder = newEntity(childName, TYPE_FOLDER)
			folder = save(folder)
            if (folder.id) println "Saved folder "+childName+" as "+folder.id
			addToParent(parentFolder, folder)
		}
		return folder
	}
	
	def Entity createSample(name, tilingPattern) {
		Entity sample = newEntity(name, TYPE_SAMPLE)
		sample.setValueByAttributeName(ATTRIBUTE_TILING_PATTERN, tilingPattern)
		sample = save(sample)
		if (sample.id) println "Saved sample as "+sample.id
		return sample
	}
	
	def Entity createSupportingFilesFolder() {
		Entity filesFolder = newEntity("Supporting Files", TYPE_SUPPORTING_DATA)
		filesFolder = save(filesFolder)
        if (filesFolder.id) println "Saved supporting files folder as "+filesFolder.id
		return filesFolder
	}
	
	def Entity newEntity(name, entityTypeName) {
		Entity entity = new Entity()
		entity.name = name
		entity.ownerKey = subject.key
		entity.creationDate = createDate
		entity.updatedDate = createDate
		entity.entityTypeName = entityTypeName
		return entity
	}
	
	def EntityData addToParent(Entity parent, Entity entity) {
		return addToParent(parent, entity, parent.maxOrderIndex+1, ATTRIBUTE_ENTITY)
	}
	
	def EntityData addToParent(Entity parent, Entity entity, Integer index, String attrName) {
		EntityData ed = parent.addChildEntity(entity, attrName)
		ed.orderIndex = index
		save(ed)
        if (ed.id) println "Added "+entity.entityTypeName+"#"+entity.id+" as child of "+parent.entityTypeName+"#"+parent.id;
		return ed
	}

    def AbstractEntityLoader getEntityLoader() {
        return new GroovyEntityLoader(this)
    }
}

class GroovyEntityLoader implements AbstractEntityLoader {

    def JacsUtils j

    public GroovyEntityLoader(JacsUtils j) {
        this.j = j
    }

    public Set<EntityData> getParents(Entity entity) throws Exception {
        return new HashSet<EntityData>(j.e.getParentEntityDatas(entity.id))
    }

    public Entity populateChildren(Entity entity) throws Exception {
        j.loadChildren(entity)
        return entity
    }
}