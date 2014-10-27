import java.text.DecimalFormat

import org.janelia.it.jacs.model.entity.Entity
import org.janelia.it.jacs.model.entity.EntityConstants
import org.janelia.it.jacs.model.entity.EntityData
import org.janelia.it.jacs.model.tasks.Task
import org.janelia.it.jacs.model.user_data.Subject
import org.janelia.it.jacs.shared.utils.EntityUtils
import org.janelia.it.jacs.shared.utils.StringUtils
import org.janelia.it.workstation.api.facade.concrete_facade.ejb.EJBFacadeManager
import org.janelia.it.workstation.api.facade.facade_mgr.FacadeManager
import org.janelia.it.workstation.gui.framework.exception_handlers.ExitHandler
import org.janelia.it.workstation.gui.framework.exception_handlers.UserNotificationExceptionHandler
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr
import org.janelia.it.workstation.gui.util.panels.ApplicationSettingsPanel
import org.janelia.it.workstation.gui.util.panels.DataSourceSettingsPanel
import org.janelia.it.workstation.gui.util.panels.ViewerSettingsPanel
import org.janelia.it.workstation.shared.util.ConsoleProperties

class CleanQiScoresScript {
	
	private static final boolean DEBUG = true;
	private static final DecimalFormat dfScore = new DecimalFormat("0.0000");
	private final PrintWriter file;
	private final JacsUtils f;
	private String context;
	
	public CleanQiScoresScript() {
		f = new JacsUtils(null, false)
		file = new PrintWriter(new FileOutputStream("clean_qi_scores.log"), true)
		login()
	}
	
	public void run() {
		println("Starting Clean Qi Scores Process")
		for(Subject subject : f.c.getSubjects()) {
			run(subject.key)
		}
		println("Done")
		file.close()
		
	}
	
	public void run(String subjectKey) {
		file.println("Processing "+subjectKey)
		for(Entity result : f.e.getUserEntitiesByTypeName(subjectKey, EntityConstants.TYPE_ALIGNMENT_RESULT)) {
			context = subjectKey+"/"+result.id
			
			f.loadChildren(result);
			Entity supportingData = EntityUtils.getSupportingData(result)
			if (supportingData==null) {
				file.println(context+" - No Supporting Files file present")
				continue
			}
			f.loadChildren(supportingData)
			
			int numProcessed = 0
			for(Entity propertiesEntity : EntityUtils.getChildrenOfType(supportingData, EntityConstants.TYPE_TEXT_FILE)) {
				
				String propertiesFilepath = propertiesEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH)
				File propertiesFile = SessionMgr.getSessionMgr().getCachedFile(propertiesFilepath, false)
				if (propertiesFile==null) {
					file.println(context+" - properties file is missing on the filesystem")
					continue
				}
				Properties properties = new Properties();
				properties.load(new FileReader(propertiesFile));
				String stackFilename = properties.getProperty("alignment.stack.filename");
				
				Entity stackEntity = EntityUtils.findChildWithName(supportingData, stackFilename)
				if (stackEntity==null) {
					stackFilename = stackFilename.replaceFirst("v3draw", "v3dpbd")
					stackEntity = EntityUtils.findChildWithName(supportingData, stackFilename)
					if (stackEntity==null) {
						// Check for sample block
						boolean blocked = false;
						Entity sample = f.e.getAncestorWithType(propertiesEntity.ownerKey, propertiesEntity.id, EntityConstants.TYPE_SAMPLE)
						if (sample!=null) {
							if ("Blocked".equals(sample.getValueByAttributeName(EntityConstants.ATTRIBUTE_STATUS))) {
								blocked = true
							}
							else {
								sample = f.e.getAncestorWithType(sample.ownerKey, sample.id, EntityConstants.TYPE_SAMPLE)
								if (sample!=null && "Blocked".equals(sample.getValueByAttributeName(EntityConstants.ATTRIBUTE_STATUS))) {
									blocked = true
								}
							}
						}
						if (!blocked) {
							file.println(context+" - properties file cites '"+stackFilename+"' but there is no entity with that name")
						}
						continue
					}
				}
				
				String scoreNcc = properties.getProperty("alignment.quality.score.ncc");
				String scoreJbaQm = properties.getProperty("alignment.quality.score.jbaqm");
				String scoresQiCsv = properties.getProperty("alignment.quality.score.qi"); // The three comma-delimited scores from QiScore.csv
				
				// Parse everything into Doubles to use a consistent decimal format
				if (!StringUtils.isEmpty(scoreNcc)) {
					String formattedScoreNcc = dfScore.format(Double.parseDouble(scoreNcc));
					setNccScore(stackEntity, formattedScoreNcc);
				}
				
				if (!StringUtils.isEmpty(scoreJbaQm)) { 
					String formattedScoreJbaQm = dfScore.format(Double.parseDouble(scoreJbaQm));
					setModelViolationScore(stackEntity, formattedScoreJbaQm);
				}
				
				// Derive all Qi and inconsistency (1-Qi) scores
				processQiScoreCsv(stackEntity, scoresQiCsv);
				
				numProcessed++
			}
			
			if (numProcessed==0) {
			
				String scoresQiCsv = getQiScoresFromFile(supportingData)
				if (scoresQiCsv!=null) {
					file.println(context+" - No properties, but QiScore.csv is available")
					Entity stackEntity = null
					for(Entity image : supportingData.getChildren()) {
						if (image.getEntityTypeName().equals(EntityConstants.TYPE_IMAGE_3D) && image.name.startsWith("Aligned")) {
							if (stackEntity!=null) {
								file.println(context+" - Multiple aligned images found, using "+stackEntity.name)
							}
							else {
								stackEntity = image
							}
						}
					}
					processQiScoreCsv(stackEntity, scoresQiCsv);
				}
			}
		
			// Clear memory	
			result.setEntityData(null);
		}
	}
	
	private String getQiScoresFromFile(Entity supportingData) {
		
		// No properties file, but is there a qiScore CSV file?
		Entity qiScoreCsv = EntityUtils.findChildWithName(supportingData, "QiScore.csv")
		if (qiScoreCsv==null) {
			return null
		}
		
		String csvQiScores = null;
		String csvFilepath = qiScoreCsv.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH)
		File csvFile = SessionMgr.getSessionMgr().getCachedFile(csvFilepath, false)
		if (csvFile==null) {
			file.println(context+" - QiScore.csv file is missing on the filesystem")
				return null
		}
		else {
			Scanner scanner = new Scanner(new FileReader(csvFile))
			int i = 0
			while (scanner.hasNext()) {
				String line = scanner.nextLine()
				if (i==1) {
					csvQiScores = line
				}
				i++
			}
			if (csvQiScores==null) {
				file.println(context+" - No scores found in QiScore.csv")
				return null
			}
		}
		
		return csvQiScores?.trim()
	}
	
	private void processQiScoreCsv(Entity alignedImage, String scoresQiCsv) throws Exception {
	
		if (StringUtils.isEmpty(scoresQiCsv)) return;
			
		List<Double> qiScores = new ArrayList<Double>();
		List<Double> inconsistencyScores = new ArrayList<Double>();
		for(String scoreQi : Task.listOfStringsFromCsvString(scoresQiCsv)) {
			try {
				Double d_scoresQi = Double.parseDouble(scoreQi);
				qiScores.add(d_scoresQi);
				inconsistencyScores.add(1-d_scoresQi);
			}
			catch (NumberFormatException e) {
				file.println(context+" - Error parsing double: "+e);
			}
		}
	
		setQiScore(alignedImage, getFormattedWeightedAverage(qiScores));
		setQiScores(alignedImage, getFormattedCSV(qiScores));
		setInconsistencyScore(alignedImage, getFormattedWeightedAverage(inconsistencyScores));
		setInconsistencyScores(alignedImage, getFormattedCSV(inconsistencyScores));
	}
	
	public void setInconsistencyScore(Entity entity, String value) throws Exception {
		setAttributeIfNecessary(entity, "Alignment Inconsistency Score", value);
	}
	
	public void setInconsistencyScores(Entity entity, String value) throws Exception {
		setAttributeIfNecessary(entity, "Alignment Inconsistency Scores", value);
	}

	public void setQiScore(Entity entity, String value) throws Exception {
		setAttributeIfNecessary(entity, "Alignment Qi Score", value);
	}
	
	public void setQiScores(Entity entity, String value) throws Exception {
		setAttributeIfNecessary(entity, "Alignment Qi Scores", value);
	}
	
	public void setModelViolationScore(Entity entity, String value) throws Exception {
		setAttributeIfNecessary(entity, "Alignment Model Violation Score", value);
	}
	
	public void setNccScore(Entity entity, String value) throws Exception {
		setAttributeIfNecessary(entity, "Alignment Normalized Cross Correlation Score", value);
	}
	
	private void setAttributeIfNecessary(Entity entity, String attributeName, String value) throws Exception {
		if (entity==null || StringUtils.isEmpty(value)) return;
		EntityData currEd = entity.getEntityDataByAttributeName(attributeName);
		if (currEd==null || !currEd.getValue().equals(value)) {
			//file.println(context+" - Updating value of "+attributeName+" from "+currEd?.value+" to "+value)
			if (!DEBUG) {
				f.e.setOrUpdateValue(entity.ownerKey, entity.id, attributeName, value);
			}
		}
	}
	
	private String getFormattedCSV(List<Double> scores) {
		StringBuilder sb = new StringBuilder();
		for(Double score : scores) {
			if (sb.length()>0) sb.append(",");
			sb.append(dfScore.format(score));
		}
		return sb.toString();
	}
	
	private String getFormattedWeightedAverage(List<Double> scores) {
		return dfScore.format(getJBAWeightedAverage(scores));
	}
	
	private Double getJBAWeightedAverage(List<Double> scores) {
		if (scores.size()!=3) {
			file.println("  Expected three scores for computing weighted average, but got "+scores.size());
			return null;
		}
		return getJBAWeightedAverage(scores.get(0), scores.get(1), scores.get(2));
	}
	
	private double getJBAWeightedAverage(double s1, double s2, double s3) {
		return s1 * 0.288 + s2 * 0.462 + s3 * 0.25;
	}
	
	private boolean areNotEqual(String o1, String o2) {
		if (StringUtils.isEmpty(o1) && StringUtils.isEmpty(o2)) return false
		return !o1.equals(o2)
	}
	
	// Need this in order to use WebDAV
	private void login() {
		try {
		   // This try block is copied from ConsoleApp. We may want to consolidate these in the future.
		   ConsoleProperties.load();
		   FacadeManager.registerFacade(FacadeManager.getEJBProtocolString(), EJBFacadeManager.class, "JACS EJB Facade Manager");
		   final SessionMgr sessionMgr = SessionMgr.getSessionMgr();
		   sessionMgr.registerExceptionHandler(new UserNotificationExceptionHandler());
		   sessionMgr.registerExceptionHandler(new ExitHandler());
		   sessionMgr.registerPreferenceInterface(ApplicationSettingsPanel.class, ApplicationSettingsPanel.class);
		   sessionMgr.registerPreferenceInterface(DataSourceSettingsPanel.class, DataSourceSettingsPanel.class);
		   sessionMgr.registerPreferenceInterface(ViewerSettingsPanel.class, ViewerSettingsPanel.class);
		   SessionMgr.getSessionMgr().loginSubject();
		}
		catch (Exception e) {
		   SessionMgr.getSessionMgr().handleException(e);
		   SessionMgr.getSessionMgr().systemExit();
		}
	}
}

CleanQiScoresScript script = new CleanQiScoresScript();
script.run();


