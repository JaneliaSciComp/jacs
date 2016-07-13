package entity

import static org.janelia.it.jacs.model.entity.EntityConstants.*
import static org.janelia.it.jacs.shared.utils.EntityUtils.*

import org.apache.solr.client.solrj.SolrQuery
import org.apache.solr.common.SolrDocument
import org.janelia.it.jacs.compute.api.support.MappedId
import org.janelia.it.jacs.model.entity.Entity
import org.janelia.it.jacs.model.entity.EntityConstants
import org.janelia.it.jacs.shared.utils.EntityUtils
import org.janelia.it.jacs.shared.utils.FileUtil
import org.janelia.it.jacs.shared.utils.StringUtils

// Globals
username = "user:leey10";
rootId = 2016303114637803609;
targetDir = "/home/rokickik/export/";
JacsUtils f = new JacsUtils(username, false);
e = f.e;
c = f.c;

Entity root = e.getEntityById(username, rootId)
targetDir += root.name.replaceAll(" ","_")

println "Exporting to "+targetDir
File rootDir = new File(targetDir)
FileUtil.ensureDirExists(rootDir.absolutePath)

createFolders(f, root, rootDir);

println "Done"

def createFolders(JacsUtils f, Entity folder, File dir) {
	println "Exporting "+folder.name
	f.loadChildren(folder)
	for (Entity child : EntityUtils.getChildrenOfType(folder, TYPE_FOLDER)) {
		File childDir = new File(dir, child.name.replaceAll(" ","_"))
		FileUtil.ensureDirExists(childDir.absolutePath)
		createFolders(f, child, childDir)
	}
	for (Entity child : EntityUtils.getChildrenOfType(folder, TYPE_SAMPLE)) {
		exportSample(f, child, dir)
	}
}

def exportSample(JacsUtils f, Entity sample, File dir) {
	f.loadChildren(sample)

	File sampleDir = new File(dir, sample.name.replaceAll(" ","_"))
	FileUtil.ensureDirExists(sampleDir.absolutePath)

	println "Exporting "+sample.name

	sample2lsm = [:]
	lsm2sample = [:]
	ids = [sample.id]
	upMapping = []
	downMapping = [
		TYPE_SUPPORTING_DATA,
		TYPE_IMAGE_TILE,
		TYPE_LSM_STACK
	]
	mappedIds = e.getProjectedResults(username, ids, upMapping, downMapping);
	for (MappedId mappedId : mappedIds) {
		if (sample2lsm.containsKey(mappedId.originalId)) continue
			sample2lsm[mappedId.originalId] = mappedId.mappedId
		lsm2sample[mappedId.mappedId] = mappedId.originalId
	}

	lsm2sample.keySet().each {
		lsmId = it
		SolrQuery query2 = new SolrQuery("id:"+lsmId)
		Iterator<SolrDocument> i = f.s.search(username, query2, true).response.results.iterator();
		while (i.hasNext()) {
			SolrDocument doc = i.next();

			def lsmName = doc.get("name")
			String filename = lsmName.replaceAll(".lsm(.bz2)?","-metadata.txt")
			
			File file = new File(sampleDir, filename)
			pw = new PrintWriter(file)

			props = [:]
			
			for(String key : doc.keySet()) {
				if (!key.startsWith("sage_") && !key.startsWith("channel_colors") && !key.equals("id")) continue;
				if (key.equals("sage_id_txt")) continue;
				String value = doc.get(key)
				if (value==null) continue
				if (value.startsWith('[')) {
					value = value.substring(1, value.length()-1)
				}
				props.put(key, value)
			}

			List<String> keys = new ArrayList<String>(props.keySet())
			Collections.sort(keys)
			for(String key : keys) {
				pw.println(getFieldName(key)+": "+props.get(key))
			}
			
			pw.close()
		}
	}
}

def String getFieldName(String key) {
	if (StringUtils.isEmpty(key)) return "";
	if (!key.equals("sage_id_l")) {
		key = key.replaceFirst("sage_","")
	}
	int lastUnderscore = key.lastIndexOf('_')
	if (lastUnderscore>0) {
		key = key.substring(0, lastUnderscore)
	}
	return StringUtils.underscoreToTitleCase(key);
}