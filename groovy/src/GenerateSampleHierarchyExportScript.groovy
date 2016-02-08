// This script formats a shell script to make directories and format jfs commands to pull files.

import org.janelia.it.jacs.model.entity.Entity
import org.janelia.it.jacs.shared.utils.EntityUtils

import static org.janelia.it.jacs.model.entity.EntityConstants.*
// Globals
username = "user:leey10";
rootId = 2204397075544670353; // leey10 folder "20151111"
targetDir = "/groups/jacs/jacsDev/saffordt/download/drive/";
JacsUtils f = new JacsUtils(username, false);
e = f.e;
c = f.c;
file = new PrintWriter("/groups/jacs/jacsDev/saffordt/export_"+rootId+".sh")

Entity root = e.getEntityById(username, rootId)
targetDir += root.name.replaceAll(" ","_")

println "Exporting to "+targetDir
File rootDir = new File(targetDir)
file.println("mkdir -p "+rootDir.absolutePath)

// For the nice original export, use this.
//createFolders(f, root, rootDir);

// For a brute-force export of everything, use this (some default images may be duplicated)
createParents(f, root, rootDir);

file.close()
println "Done"

def createParents(JacsUtils f, Entity parent, File dir) {
	println "Exporting "+parent.name
	f.loadChildren(parent)
	for (Entity child : parent.getChildren()) {
		if (null!=child.getChildren()&&child.getChildren().size()>0) {
			String childName = child.name.replaceAll(" ","_")
			childName = childName.replace('.','_')
			File childDir = new File(dir, childName)
			file.println("mkdir -p "+childDir.absolutePath)
			// Even though they have children the parent itself might be a file.
			exportFiles(f, child, dir)
			createParents(f, child, childDir)
		}
		else {
			exportFiles(f, child, dir)
		}
	}

}

def exportFiles(JacsUtils f, Entity target, File dir) {
	println "Exporting "+target.name

	String pathTest = target.getValueByAttributeName(ATTRIBUTE_FILE_PATH);
	String jfsTest = target.getValueByAttributeName(ATTRIBUTE_JFS_PATH);
	if (null==pathTest&&null==jfsTest) {
		println "Skipping "+target.getName();
		return;
	}
	String targetPath = (null==pathTest?jfsTest:pathTest);
	// This locks it down to v3dpbd files and lsm metadata files exported.  Comment out or change to suit your need.
	if (!targetPath.endsWith("v3dpbd")&&!targetPath.endsWith("metadata")&&!targetPath.endsWith("mask")&&!targetPath.endsWith("chan")) {return;}
	File targetFile = new File(targetPath);
	file.println("/misc/local/jfs/jfs -command read -path "+targetPath+" -file "+dir+File.separator+targetFile.getName());
}

def createFolders(JacsUtils f, Entity folder, File dir) {
	println "Exporting "+folder.name
	f.loadChildren(folder)
	for (Entity child : EntityUtils.getChildrenOfType(folder, TYPE_FOLDER)) {
		File childDir = new File(dir, child.name.replaceAll(" ","_"))
		file.println("mkdir -p "+childDir.absolutePath)
		createFolders(f, child, childDir)
	}
	for (Entity child : folder.getChildren()) {
		exportFiles(f, child, dir)
	}
}


