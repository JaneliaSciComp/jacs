
import static org.janelia.it.jacs.model.entity.EntityConstants.*;
import static org.janelia.it.jacs.shared.utils.EntityUtils.*;

import java.util.List

import org.apache.commons.io.FileUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.janelia.it.jacs.compute.api.SolrBeanRemote;
import org.janelia.it.jacs.shared.solr.SolrResults;
import org.janelia.it.jacs.model.entity.Entity;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

username = "user:korffw";
folderId = 2282602235210760337l;
JacsUtils f = new JacsUtils(username, false);
e = f.e;
c = f.c;
SolrBeanRemote s = f.s;

Entity folder = f.e.getEntityById(username, folderId)
f.loadChildren(folder)
for(Entity child : folder.getOrderedChildren()) {
	String filepath = child.getValueByAttributeName(ATTRIBUTE_FILE_PATH)
	println child.name
	println filepath
	File file = new File(filepath)
	File copy = new File("/misc/public/ForWyatt/", file.getName())
	FileUtils.copyFile(file, copy)
}
