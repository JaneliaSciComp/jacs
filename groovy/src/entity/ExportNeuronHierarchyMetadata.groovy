package entity

import org.janelia.it.jacs.model.entity.Entity
import org.janelia.it.jacs.model.entity.EntityConstants

rootId = 2204397075544670353L;

username = "user:leey10";
JacsUtils f = new JacsUtils(username, false);
e = f.e;
file = new PrintWriter("/groups/jacs/jacsDev/saffordt/export_"+rootId+".txt")

Entity root = e.getEntityById(username, rootId)

println "Exporting to "+file

traverse(f, root, file, "");

file.close()
println "Done"

def traverse(JacsUtils f, Entity parent, PrintWriter file, String indent) {
    println "Exporting " + parent.name
    f.loadChildren(parent)
    for (Entity child : parent.getOrderedChildren()) {

        if (child.getEntityTypeName().equals(EntityConstants.TYPE_SAMPLE)) {
            file.println(indent+child.name + "");
        }
        else if (child.getEntityTypeName().equals(EntityConstants.TYPE_NEURON_FRAGMENT)) {
            Entity sample = e.getAncestorWithType(username, child.id, EntityConstants.TYPE_SAMPLE)
            file.println(indent+child.name + " (" + sample.name + ")");
        }
        else if (child.getEntityTypeName().equals(EntityConstants.TYPE_FOLDER)) {
            file.println(indent+child.name + "");
            traverse(f, child, file, indent + "  ")
        }
    }
}









