import org.janelia.it.jacs.model.domain.gui.search.criteria.Criteria
import spock.lang.*
import org.janelia.it.jacs.compute.wsrest.DataViewsWebService
import org.janelia.it.jacs.model.domain.sample.*
import org.janelia.it.jacs.model.domain.screen.*
import org.janelia.it.jacs.model.domain.gui.search.Filter
import org.janelia.it.jacs.shared.utils.DomainQuery
import org.janelia.it.jacs.compute.access.mongodb.DomainDAO


/*
 * tests the endpoints of the RESTful server to make sure
 */
class DataViewsSpec extends Specification {
    def dvService
    def testFilter
    def testUser
    DomainDAO dao

    def setup() {
        def testSample = new Sample([
                id: 1980402565539430407L,
                name: "TZL_stg14-RQZ00390_A3",
                age: "A",
                line: "TZL_stg14"
        ])
        def updateSample = new Sample([
                id: 1980402565539430407L,
                name: "TZL_stg14-RQZ00390_A3",
                slideCode: "RQZ00390_A3"
        ])
        def testFragment = new NeuronFragment([
                id: 1980402565539430408L,
                number: new Integer(1),
                filepath: "fileloc:fragmentloc"
        ])
        def testPatternMask = new PatternMask([
                id: 1980402565539430409L,
                maskSetName: "PatternAnnotation",
                normalized: false,
                filepath: "/nobackup/jacs/jacsData/filestore/system/ScreenPipeline/247/394/1697449610152247394/ScreenSample/457/378/1697453422879457378/"
        ])
        testFilter = new Filter([
                searchType : "testSearchType",
                searchString : "testSearch",
                sort : "ascending"
        ])
        testUser = "user:testuser"
        def testSampleList = [testSample]
        def testFragmentList = [testFragment]
        def testPatternMaskList = [testPatternMask]
        dao = Stub()
        def testSampleIds = [1980402565539430407L]
        def testFragmentIds = [1980402565539430408L]
        def testPatternMaskIds = [1980402565539430409L]
        def testuser = "user:testuser"
        dao.getDomainObjects(testuser, "sample", testSampleIds) >> testSampleList
        dao.getDomainObjects(testuser, "fragment", testFragmentIds) >> testFragmentList
        dao.getDomainObjects(testuser, "patternMask", testPatternMaskIds) >> testPatternMaskList
        dao.updateProperty(testuser, "sample", 1980402565539430407L, "slideCode", "RQZ00390_A3") >> updateSample
        dao.save(testuser, testFilter) >> testFilter
        dvService = new DataViewsWebService()
        dvService.setDao(dao)
    }


    def "query for a Sample"() {
        given:
        def sampleQuery = new DomainQuery([
                subjectKey: testUser,
                objectType: "sample",
                objectIds: new ArrayList<Long>([1980402565539430407L])
        ])

        when:
        def results = dvService.getObjectDetails(sampleQuery)

        then:
        results.indexOf("\"_id\":1980402565539430407") != -1
        results.indexOf("\"name\":\"TZL_stg14-RQZ00390_A3\"") != -1
    }

    def "query for a Neuron Fragment"() {
        given:
        def fragmentQuery = new DomainQuery([
                subjectKey: testUser,
                objectType: "fragment",
                objectIds: new ArrayList<Long>([1980402565539430408L])
        ])

        when:
        def results = dvService.getObjectDetails(fragmentQuery)

        then:
        results.indexOf("\"_id\":1980402565539430408") != -1
        results.indexOf("\"number\":1") != -1
    }


    def "query for a PatternMask"() {
        given:
        def patternMaskQuery = new DomainQuery([
                subjectKey: testUser,
                objectType: "patternMask",
                objectIds: new ArrayList<Long>([1980402565539430409L])
        ])

        when:
        def results = dvService.getObjectDetails(patternMaskQuery)

        then:
        results.indexOf("\"_id\":1980402565539430409") != -1

    }

    def "update a Sample"() {
        given:
        def updateSampleQuery = new DomainQuery([
                subjectKey: testUser,
                objectType: "sample",
                objectIds: new ArrayList<Long>([1980402565539430407L]),
                propertyName: "slideCode",
                propertyValue: "RQZ00390_A3"
        ])

        when:
        def results = dvService.updateObjectProperty(updateSampleQuery)

        then:
        results.indexOf("\"slideCode\":\"RQZ00390_A3\"") != -1
    }


    def "create a Filter"() {
        when:
        def results = dvService.createFilter(testUser, testFilter)

        then:
        results.indexOf("\"searchType\":\"testSearchType\"") != -1
    }


    def "update a Filter"() {
        when:
        def results = dvService.updateFilter(testUser, testFilter)

        then:
        results.indexOf("\"searchType\":\"testSearchType\"") != -1
    }
}
