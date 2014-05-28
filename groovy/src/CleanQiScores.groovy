import java.text.DecimalFormat

import org.janelia.it.workstation.api.facade.concrete_facade.ejb.EJBEntityLoader
import org.janelia.it.jacs.model.entity.Entity
import org.janelia.it.jacs.model.entity.EntityConstants
import org.janelia.it.jacs.model.entity.EntityData
import org.janelia.it.jacs.model.tasks.Task
import org.janelia.it.jacs.shared.utils.StringUtils
import org.janelia.it.jacs.shared.utils.entity.EntityVisitor
import org.janelia.it.jacs.shared.utils.entity.EntityVistationBuilder

boolean DEBUG = false

DecimalFormat dfScore = new DecimalFormat("#.######");
final JacsUtils f = new JacsUtils(null, false)

Set<String> subjectKeys = new HashSet<String>();
for(Entity dataSet : f.e.getEntitiesByTypeName(null, EntityConstants.TYPE_DATA_SET)) {
    subjectKeys.add(dataSet.getOwnerKey());
}

println "Found users with data sets: "+subjectKeys
for(String subjectKey : subjectKeys) {
    println "Processing "+subjectKey;

    final EJBEntityLoader entityLoader = new EJBEntityLoader(f.e)
    for(Entity result : f.e.getUserEntitiesByTypeName(subjectKey, EntityConstants.TYPE_ALIGNMENT_RESULT)) {
        if (!result.name.equals("JBA Alignment")) continue
        EntityVistationBuilder.create(entityLoader).startAt(result)
        .childrenOfType(EntityConstants.TYPE_SUPPORTING_DATA)
        .childrenOfType(EntityConstants.TYPE_IMAGE_3D)
        .run(new EntityVisitor() {
                public void visit(Entity image) throws Exception {
				
                    EntityData scoreQiEd = image.getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_ALIGNMENT_QI_SCORE);
                    if (scoreQiEd==null) return;
                    String scoreQi = scoreQiEd.value
				
                    Double score1MinusQiCombined = null;
                    List<Double> inconsistencyScores = new ArrayList<Double>();
                    if (!StringUtils.isEmpty(scoreQi)) {
                        for(String qiScore : Task.listOfStringsFromCsvString(scoreQi)) {
                            try {
                                double score = Double.parseDouble(qiScore);
                                inconsistencyScores.add(score);
                            }
                            catch (NumberFormatException e) {
                                println "Error parsing double: "+e;
                            }
                        }
                        if (inconsistencyScores.size()==3) {
                            double q1 = inconsistencyScores.get(0);
                            double q2 = inconsistencyScores.get(1);
                            double q3 = inconsistencyScores.get(2);
                            score1MinusQiCombined = q1 * 0.288 + q2 * 0.462 + q3 * 0.25;
                        }
                        else if (inconsistencyScores.size()==1) {
                            score1MinusQiCombined = inconsistencyScores.get(0);
                        }
                        else {
                            println "WARN: Unsupported number of inconsistency scores: ["+scoreQi+"]";
                        }
                    }
				
                    String newScore = dfScore.format(score1MinusQiCombined);
				
                    if (DEBUG) {
                        println scoreQi+" -> "+newScore
                    }
                    else {
                        scoreQiEd.setValue(newScore);
                        f.e.saveOrUpdateEntityData(image.ownerKey, scoreQiEd)
                        f.e.setOrUpdateValue(image.ownerKey, image.id, "Alignment Inconsistency Scores", scoreQi)
                    }
				
                }
            });
		
        result.setEntityData(null);
    }
}

println "Done"