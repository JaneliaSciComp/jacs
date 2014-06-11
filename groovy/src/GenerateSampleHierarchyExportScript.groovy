
import static org.janelia.it.jacs.model.entity.EntityConstants.*
import static org.janelia.it.jacs.shared.utils.EntityUtils.*

import org.apache.solr.client.solrj.SolrQuery
import org.janelia.it.jacs.compute.api.SolrBeanRemote
import org.janelia.it.jacs.compute.api.support.SolrResults
import org.janelia.it.jacs.model.entity.Entity
import org.janelia.it.jacs.shared.utils.EntityUtils
import org.janelia.it.jacs.shared.utils.FileUtil

// Globals
username = "user:leey10";
rootId = 2016303114637803609;
targetDir = "/home/rokickik/export/drive/";
JacsUtils f = new JacsUtils(username, false);
e = f.e;
c = f.c;
file = new PrintWriter("export_"+rootId+".sh")

Entity root = e.getEntityById(username, rootId)
targetDir += root.name.replaceAll(" ","_")

println "Exporting to "+targetDir
File rootDir = new File(targetDir)
file.println("mkdir -p "+rootDir.absolutePath)

createFolders(f, root, rootDir);

file.close()
println "Done"

def createFolders(JacsUtils f, Entity folder, File dir) {
	println "Exporting "+folder.name
	f.loadChildren(folder)
	for (Entity child : EntityUtils.getChildrenOfType(folder, TYPE_FOLDER)) {
		File childDir = new File(dir, child.name.replaceAll(" ","_"))
		file.println("mkdir -p "+childDir.absolutePath)
		createFolders(f, child, childDir)
	}
	for (Entity child : EntityUtils.getChildrenOfType(folder, TYPE_SAMPLE)) {
		exportSample(f, child, dir)
	}
}

def exportSample(JacsUtils f, Entity sample, File dir) {
	f.loadChildren(sample)
	
	println "Exporting "+sample.name
	
	Entity run = EntityUtils.getLatestChildOfType(sample, TYPE_PIPELINE_RUN)
	if (run!=null) {
		f.loadChildren(run)
		Entity result = EntityUtils.getLatestChildOfType(run, TYPE_SAMPLE_PROCESSING_RESULT)
		if (result!=null) {
			String filepath = result.getValueByAttributeName(ATTRIBUTE_DEFAULT_3D_IMAGE)
			String ext = filepath.substring(filepath.lastIndexOf(".")+1)
			File targetFile = new File(dir, sample.name+"-unaligned."+ext)
			file.println("cp "+filepath+" "+targetFile.absolutePath)
			//file.println("touch "+targetFile.absolutePath)
		}
		Entity alignment = EntityUtils.getLatestChildOfType(run, TYPE_ALIGNMENT_RESULT)
		if (alignment!=null) {
			String filepath = alignment.getValueByAttributeName(ATTRIBUTE_DEFAULT_3D_IMAGE)
			String ext = filepath.substring(filepath.lastIndexOf(".")+1)
			File targetFile = new File(dir, sample.name+"-aligned."+ext)
			file.println("cp "+filepath+" "+targetFile.absolutePath)
			//file.println("touch "+targetFile.absolutePath)
		}
	}
}

