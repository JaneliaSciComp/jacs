import org.janelia.it.jacs.model.entity.Entity
import org.janelia.it.jacs.model.entity.EntityData

import com.google.common.collect.ComparisonChain
import com.google.common.collect.Ordering

boolean DEBUG = false

String ownerKey = "group:flylight";
f = new JacsUtils(ownerKey, false);

for(Entity folder : f.e.getEntitiesByNameAndTypeName(ownerKey, "Blocked Data", "Folder")) {
	println folder.name
	
	List<EntityData> eds = new ArrayList<>(f.e.getParentEntityDatas(null, folder.id))
	List<Long> toDelete = new ArrayList<>()
	
	Collections.sort(eds, new Comparator<EntityData>() {
		@Override
		public int compare(EntityData o1, EntityData o2) {
			return ComparisonChain.start()
					.compare(o1.ownerKey, o2.ownerKey, Ordering.natural())
					.compare(o1.id, o2.id, Ordering.natural())
					.result()
		}
	});
	
	for(EntityData ed : eds) {
		boolean delete = ed.id>2260055214962081937 && !ed.ownerKey.equals("group:flylight")
		println ed.id+" "+ed.ownerKey+" "+ed.orderIndex+" "+(delete?"DELETE":"")
		toDelete.add(ed.id)
		if (delete && !DEBUG) {
			f.e.deleteEntityData(ed.ownerKey, ed.id)
		}
	}
}
