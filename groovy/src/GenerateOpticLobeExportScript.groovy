
import static org.janelia.it.jacs.model.entity.EntityConstants.*;
import static org.janelia.it.jacs.shared.utils.EntityUtils.*


import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.shared.utils.EntityUtils

// Globals
username = "system"
targetDir = "/Volumes/Passport/OpticLobes"

f = new JacsUtils(username, false)
e = f.e
c = f.c
s = f.s

for(Entity sample : e.getUserEntitiesWithAttributeValue(username, ATTRIBUTE_DATA_SET_IDENTIFIER, "system_flylight_optic_lobe_tile")) {

    f.loadChildren(sample)
    
    run = sample.getLatestChildOfType(TYPE_PIPELINE_RUN)
    if (run==null) continue
    f.loadChildren(run)
    
    sp = EntityUtils.findChildWithName(run, "Sample Processing")
    
    dir = "\""+targetDir+"/"+sample.name+"/\""
    
    if (!sample.name.contains("Right")) continue;
    
    if (sp != null) {
        f.loadChildren(sp)
        
        supportingFiles = EntityUtils.findChildWithName(sp, "Supporting Files")
        f.loadChildren(supportingFiles)
        
        meta = []
        stack = null
        
        for(Entity sf : supportingFiles.children) {
            if (sf.name.endsWith("metadata")) {
                file = sf.getValueByAttributeName(ATTRIBUTE_FILE_PATH)
                file = file.replaceAll("/groups/scicomp","/Volumes")
                meta.add("\""+file+"\"")
            }
            else if ((sf.name.startsWith("stitched")||sf.name.startsWith("merged")) && sf.name.endsWith("v3dpbd")) {
                file = sf.getValueByAttributeName(ATTRIBUTE_FILE_PATH)
                file = file.replaceAll("/groups/scicomp","/Volumes")
                stack = file
                //"\""+file+"\""
            }
        }
        
        if (stack!=null) {
            
//            ns = sp.getLatestChildOfType(TYPE_NEURON_SEPARATOR_PIPELINE_RESULT)
//            if (ns==null) continue
//            f.loadChildren(ns)
//            
//            supportingFiles = EntityUtils.findChildWithName(ns, "Supporting Files")
//            f.loadChildren(supportingFiles)
//            
//            labelFile = EntityUtils.findChildWithName(supportingFiles, "ConsolidatedLabel.v3dpbd")
//            
//            labelFilepath = labelFile.getValueByAttributeName(ATTRIBUTE_FILE_PATH)
//            labelFilepath = labelFilepath.replaceAll("/groups/scicomp","/Volumes")
//            labelFilepath = "\""+labelFilepath+"\""
//            
//            println "mkdir "+dir
//            meta.each {
//                println "cp "+it+" "+dir
//            }
//            println "cp "+stack+" "+dir
//            println "cp "+labelFilepath+" "+dir
            println stack
        }
        
        
    }
    
    sample.setEntityData(null)
}


