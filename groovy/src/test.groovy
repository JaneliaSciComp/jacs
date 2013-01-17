import org.janelia.it.FlyWorkstation.api.facade.concrete_facade.ejb.EJBFactory 
import org.janelia.it.jacs.compute.api.AnnotationBeanRemote
import org.janelia.it.jacs.compute.api.ComputeBeanRemote
import org.janelia.it.jacs.model.entity.Entity
import org.janelia.it.jacs.model.entity.EntityData
import org.janelia.it.jacs.model.user_data.User
import org.janelia.it.jacs.shared.utils.EntityUtils
import org.janelia.it.jacs.model.entity.EntityConstants

String username = 'rokickik'
Long id = 1759865966809841762

// Globals
f = new JacsUtils(username, false)
e = f.e
a = f.a

Entity entity = e.getEntityById(id+"")
printEntityTree(entity)

entity = e.annexEntityTree(id, username)
printEntityTree(entity)


def printEntityTree(Entity tree) {
	printEntityTree(tree, "")
}

def printEntityTree(Entity tree, String indent) {
	println indent+""+tree.name +" ("+tree.user.userLogin+")"
	f.loadChildren(tree)
	tree.orderedChildren.each {
		printEntityTree(it, indent+"  ")
	}
}

