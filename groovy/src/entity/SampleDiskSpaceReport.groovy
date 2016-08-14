package entity

import com.google.common.collect.ComparisonChain
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import org.janelia.it.jacs.model.entity.Entity
import org.janelia.it.jacs.shared.utils.EntityUtils
import org.janelia.it.jacs.shared.utils.entity.AbstractEntityLoader
import org.janelia.it.jacs.shared.utils.entity.EntityVisitor
import org.janelia.it.jacs.shared.utils.entity.EntityVistationBuilder

import static org.janelia.it.jacs.model.entity.EntityConstants.*

int numSamplesPerDataset = 100
int maxToTest = 1000
String outputFilename = "sampleFiles100.json"
String subjectKey = null
f = new JacsUtils(subjectKey, false)
e = f.e
Gson gson = new GsonBuilder().setPrettyPrinting().create()

Map<String,List<JsonElement>> dataSetMap = new HashMap<String,List<JsonElement>>();

List<Entity> dataSets = new ArrayList<Entity>(e.getEntitiesByTypeName(subjectKey, TYPE_DATA_SET));

Collections.sort(dataSets, new Comparator<Entity>() {
    @Override
    int compare(Entity o1, Entity o2) {
        ComparisonChain chain = ComparisonChain.start()
            .compare(o1.ownerKey, o2.ownerKey)
            .compare(o1.name, o2.name);
        return chain.result()
    }
});

for(Entity dataSet : dataSets) {

    String dataSetIdentifier = dataSet.getValueByAttributeName(ATTRIBUTE_DATA_SET_IDENTIFIER)
    if (dataSetIdentifier==null || "".equals(dataSetIdentifier)) continue;

    List<SamplePaths> sampleJsonList = new ArrayList<SamplePaths>()

    List<Entity> samples = new ArrayList<Entity>(e.getUserEntitiesWithAttributeValueAndTypeName(dataSet.ownerKey, ATTRIBUTE_DATA_SET_IDENTIFIER, dataSetIdentifier, TYPE_SAMPLE))

    if (samples.isEmpty()) {
        println "Data set has no samples: "+dataSetIdentifier
    }
    else {
        println "Data set "+dataSetIdentifier+" has "+samples.size()+" samples"
        Collections.shuffle(samples)
        int i=0;
        while (sampleJsonList.size() < numSamplesPerDataset && i<samples.size() && i<maxToTest) {
            Entity sample = samples.get(i++);
            SamplePaths samplePaths = analyze(f, sample, sampleJsonList, gson);
            if (samplePaths!=null) {
                sampleJsonList.add(samplePaths)
            }
        }
        dataSetMap.put(dataSetIdentifier, sampleJsonList)
    }
}

PrintWriter file = new PrintWriter(outputFilename)
file.println(gson.toJson(dataSetMap))
file.close()
println "Wrote "+outputFilename

SamplePaths analyze(JacsUtils f, Entity sample, List<JsonElement> sampleJsonList, Gson gson) {

    AbstractEntityLoader entityLoader = f.getEntityLoader()
    entityLoader.populateChildren(sample)

    def line = sample.getValueByAttributeName(ATTRIBUTE_LINE)
    SamplePaths samplePaths = new SamplePaths(sample.name, line)
    
    def parent = false
    for(Entity subSample : EntityUtils.getChildrenOfType(sample, TYPE_SAMPLE)) {
        LsmFinder lsmFinder = new LsmFinder();
        lsmFinder.findPaths(subSample, entityLoader)
        samplePaths.getLsmPaths().addAll(lsmFinder.paths)
        ResultFinder resultFinder = new ResultFinder();
        resultFinder.findPaths(subSample, entityLoader)
        samplePaths.getResultPaths().addAll(resultFinder.paths)
        parent = true
    }

    if (!parent) {
        LsmFinder lsmFinder = new LsmFinder();
        lsmFinder.findPaths(sample, entityLoader)
        samplePaths.getLsmPaths().addAll(lsmFinder.paths)
        ResultFinder resultFinder = new ResultFinder();
        resultFinder.findPaths(sample, entityLoader)
        samplePaths.getResultPaths().addAll(resultFinder.paths)
    }
    
    if (samplePaths.getLsmPaths().isEmpty()) {
        println "Sample has no LSMs: "+sample.id
        return null
    }
    
    if (samplePaths.getResultPaths().isEmpty()) {
        println "Sample has no results: "+sample.id
        return null
    }
    
    return samplePaths;
}

class LsmFinder {

    List<String> paths = new ArrayList<String>();
    public void findPaths(Entity sample, entityLoader) {
        EntityVistationBuilder.create(entityLoader).startAt(sample)
                .childrenOfType(TYPE_SUPPORTING_DATA)
                .childrenOfType(TYPE_IMAGE_TILE)
                .childrenOfType(TYPE_LSM_STACK)
                .run(new EntityVisitor() {
            public void visit(Entity lsm) throws Exception {
                String filepath = lsm.getValueByAttributeName(ATTRIBUTE_FILE_PATH)
                if (filepath!=null) {
                    paths.add(filepath)
                }
            }
        });
    }
}

class ResultFinder {

    List<String> paths = new ArrayList<String>();
    public void findPaths(Entity sample, final AbstractEntityLoader entityLoader) {
        EntityVistationBuilder.create(entityLoader).startAt(sample)
                .childrenOfType(TYPE_PIPELINE_RUN)
                .childrenOfAttr(ATTRIBUTE_RESULT)
                .run(new EntityVisitor() {
            public void visit(Entity result) throws Exception {
                String filepath = result.getValueByAttributeName(ATTRIBUTE_FILE_PATH)
                if (filepath!=null) {
                    paths.add(filepath)
                }

                entityLoader.populateChildren(result)

                for(Entity subResult : EntityUtils.getChildrenForAttribute(result, ATTRIBUTE_RESULT)) {
                    String subFilepath = subResult.getValueByAttributeName(ATTRIBUTE_FILE_PATH)
                    if (subFilepath!=null) {
                        paths.add(subFilepath)
                    }
                }

            }
        });
    }
}

class SamplePaths {
    private String sampleName;
    private String line;
    private List<String> lsmPaths = new ArrayList<String>();
    private List<String> resultPaths = new ArrayList<String>();

    public SamplePaths(String sampleName, String line) {
        this.sampleName = sampleName
        this.line = line
    }

    List<String> getLsmPaths() {
        return lsmPaths
    }

    List<String> getResultPaths() {
        return resultPaths
    }
}
