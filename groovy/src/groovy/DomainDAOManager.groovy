import org.janelia.it.jacs.model.domain.support.DomainDAO

/**
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DomainDAOManager {

    private static DomainDAOManager instance;

    protected DomainDAO dao;

    private DomainDAOManager() {
        try {
//            this.dao = new DomainDAO("mongodb1,mongodb2,mongodb3", "jacs", "flyportalApp", "f@br0s@urusW")
            this.dao = new DomainDAO("dev-mongodb", "jacs")
        }
        catch (UnknownHostException e) {
            println "Error connecting to Mongo";
            e.printStackTrace();
        }
    }

    public static DomainDAOManager getInstance() {
        if (instance==null) {
            instance = new DomainDAOManager();
        }
        return instance;
    }

    public DomainDAO getDao() {
        return dao;
    }

}
