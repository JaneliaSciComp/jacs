package entity

import org.janelia.it.jacs.model.entity.Entity
import org.janelia.it.jacs.model.entity.EntityConstants
import org.janelia.it.jacs.shared.utils.entity.EntityVisitor
import org.janelia.it.jacs.shared.utils.entity.EntityVistationBuilder

class AddMissingChanSpecScript {
    static final DEFAULT_LSM_CHANSPEC = "rss"
    static final OWNER = "wolfft"
    static final OWNER_KEY = "user:"+OWNER
    JacsUtils f = new JacsUtils(OWNER_KEY, true)

    def main() {

        try {
            def roots = f.e.getEntitiesByNameAndTypeName(OWNER_KEY, "Rubin Lab Central Complex Tanya", "Folder")
            for(Entity root : roots) {
                println "Walking "+root.id
                walkTree(root)
            }
        }
        catch (Throwable t) {
            t.printStackTrace()
        }
        System.exit(0)
    }

    def walkTree(Entity entity) {
        f.loadChildren(entity)
        for(Entity child : entity.children) {
            def type = child.entityTypeName
            if (type.equals("Sample")) {
                processSample(child)
            }
            else if (type.equals("Folder")) {
                walkTree(child)
            }
        }
    }

    def processSample(Entity sample) {
        EntityVistationBuilder.create(f.getEntityLoader())
                .startAt(sample)
                .childrenOfType(EntityConstants.TYPE_SUPPORTING_DATA)
                .childrenOfType(EntityConstants.TYPE_IMAGE_TILE)
                .childrenOfType(EntityConstants.TYPE_LSM_STACK)
                .run(new EntityVisitor() {
            public void visit(Entity lsmEntity) throws Exception {
                String chanSpec = lsmEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_CHANNEL_SPECIFICATION)
                if (chanSpec==null) {
                    f.e.setOrUpdateValue(OWNER_KEY, lsmEntity.id, EntityConstants.ATTRIBUTE_CHANNEL_SPECIFICATION, DEFAULT_LSM_CHANSPEC)
                    println "Setting default chanspec for "+lsmEntity.name
                }
            }
        });

        // free memory
        sample.setEntityData(null)
    }
}

new AddMissingChanSpecScript().main()