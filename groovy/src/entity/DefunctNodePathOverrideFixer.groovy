package entity

import org.janelia.it.jacs.model.user_data.FileNode

boolean DEBUG = false

String ownerKey = "group:leetlab"
String appendee = ".bz2"

final JacsUtils f = new JacsUtils(ownerKey, false)

Scanner scanner = new Scanner(new File("/groups/jacs/jacsShare/saffordTest/defunctNode.server.log"));
while (scanner.hasNextLine()) {
    String tmpLine = scanner.nextLine();
    if (tmpLine.contains("cannot be deleted:")) {
        String tmpPath = tmpLine.substring(tmpLine.lastIndexOf(":")+1).trim();
        String tmpName = tmpPath.substring(tmpPath.lastIndexOf("/")+1);
        FileNode tmpNode = (FileNode)f.c.getNodeById(Long.valueOf(tmpName));
        if (null!=tmpNode) {
            File testFile = new File(tmpPath);
            if (testFile.exists()) {
                println "Path = "+tmpPath+" while override = "+tmpNode.pathOverride;
                if (null==tmpNode.getPathOverride()) {
                    tmpNode.setPathOverride(tmpPath);
                    f.c.saveOrUpdateNode(tmpNode);
                }
            }
            else {
                println "ERROR: Entity path does not exist: "+tmpPath;
            }
        }
    }
}

println "Done"

