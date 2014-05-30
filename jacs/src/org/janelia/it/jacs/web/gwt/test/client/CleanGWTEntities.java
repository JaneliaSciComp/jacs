
package org.janelia.it.jacs.web.gwt.test.client;

import com.google.gwt.core.client.EntryPoint;
import org.janelia.it.jacs.model.collections.ImmutableHashMap;
import org.janelia.it.jacs.model.collections.ImmutableHashSet;
import org.janelia.it.jacs.model.common.BlastableNodeVO;
import org.janelia.it.jacs.model.download.Author;
import org.janelia.it.jacs.model.download.DataFile;
import org.janelia.it.jacs.model.download.HierarchyNode;
import org.janelia.it.jacs.model.download.Project;
import org.janelia.it.jacs.model.download.Publication;
import org.janelia.it.jacs.model.genomics.AASequence;
import org.janelia.it.jacs.model.genomics.Assembly;
import org.janelia.it.jacs.model.genomics.BaseSequenceEntity;
import org.janelia.it.jacs.model.genomics.BioSequence;
import org.janelia.it.jacs.model.genomics.Chromosome;
import org.janelia.it.jacs.model.genomics.EntityTypeGenomic;
import org.janelia.it.jacs.model.genomics.NASequence;
import org.janelia.it.jacs.model.genomics.Nucleotide;
import org.janelia.it.jacs.model.genomics.ORF;
import org.janelia.it.jacs.model.genomics.Peptide;
import org.janelia.it.jacs.model.genomics.Read;
import org.janelia.it.jacs.model.genomics.Scaffold;
import org.janelia.it.jacs.model.genomics.SeqUtil;
import org.janelia.it.jacs.model.genomics.SequenceException;
import org.janelia.it.jacs.model.genomics.SequenceType;
import org.janelia.it.jacs.model.metadata.BioMaterial;
import org.janelia.it.jacs.model.metadata.GeoPath;
import org.janelia.it.jacs.model.metadata.GeoPoint;
import org.janelia.it.jacs.model.metadata.Library;
import org.janelia.it.jacs.model.metadata.Sample;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.blast.*;
import org.janelia.it.jacs.model.user_data.DataSource;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.user_data.User;
import org.janelia.it.jacs.model.user_data.blast.Blastable;
import org.janelia.it.jacs.model.vo.BooleanParameterVO;
import org.janelia.it.jacs.model.vo.DoubleParameterVO;
import org.janelia.it.jacs.model.vo.LongParameterVO;
import org.janelia.it.jacs.model.vo.MultiSelectVO;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.model.vo.ParameterVO;
import org.janelia.it.jacs.model.vo.SingleSelectVO;
import org.janelia.it.jacs.model.vo.TextParameterVO;

/**
 * This dummy page is used by the build to make sure that model classes referenced in it pass GWT compiler.
 * That in turn increases the likelihood that these model classes can be used in GWT client
 * pages in the future if they're not being used already.
 * <p/>
 * Note: To make sure that these model classes can be used at runtime by GWT, you'd have to call
 * cleanGWTEnity in the service impl layer before passing the object to GWT page.
 * <p/>
 * see org.janelia.it.jacs.web.gwt.detail.server.bse.BSEntityServiceImpl for example:
 * Read read = featureDAO.getReadByBseEntityId(Long.valueOf(bseEntityId));
 * cleanForGWT(read);
 * return read;
 */
public class CleanGWTEntities implements EntryPoint {

    /**
     * Classes below that have been commented out do not pass GWT compiler and as such cannot be referenced
     * in GWT pages directly.  In order to access any data contained by these classes we'd have to either
     * clean up GWT errors in these classes or create value objects that do pass the GWT compiler
     */

//    private org.janelia.it.jacs.model.TimebasedIdentifierGenerator idGenerator;
//    private org.janelia.it.jacs.server.utils.BaseProperties baseProperties;
//    private org.janelia.it.jacs.model.common.SystemConfigurationProperties camProps;
//    private org.janelia.it.jacs.model.user_data.blast.BlastDatabaseFileNode blastDBFileNode;
//    private org.janelia.it.jacs.model.user_data.blast.BlastResultFileNode blastResFileNode;
//    private org.janelia.it.jacs.model.user_data.FastaFileNode fastaFileNode;
//    private org.janelia.it.jacs.model.user_data.FileNode fileNode;
//    private org.janelia.it.jacs.model.common.ParameterVOMapUserType paramVoMapUserType;
//    private org.janelia.it.jacs.model.user_data.blast.BlastResultNode blastResNode;
//    private org.janelia.it.jacs.model.genomics.BlastHit blastHit;
    private ImmutableHashMap imutHashmap;
    private ImmutableHashSet imutHashset;
    private BlastableNodeVO bnVo;
    private Author author;
    private DataFile datafile;
    private HierarchyNode hierarchyNode;
    private Project project;
    private Publication publication;
    private Assembly assemble;
    private BaseSequenceEntity bseEntity;
    private NASequence naSequence;
    private AASequence aaSequence;
    private BioSequence biosequence;
    private Chromosome chromosome;
    private EntityTypeGenomic entitytype;
    private Peptide peptide;
    private Nucleotide nucleotide;
    private ORF orf;
    private Read read;
    private Scaffold scaffold;
    private SequenceException seqException;
    private SequenceType seqType;
    private SeqUtil seqUtil;
    private Library library;
    private GeoPath geoPath;
    private GeoPoint geoPoint;
    private Sample sample;
    private BioMaterial bioMaterial;
    private BlastNTask blastNTask;
    private BlastPTask blastPTask;
    private BlastTask blastTask;
    private BlastXTask blastXTask;
    private MegablastTask megaBlastTask;
    private Task task;
    private TBlastNTask tBlastNTask;
    private TBlastXTask tBlastXTask;
    private Blastable blastable;
    private DataSource dataSource;
    private Event event;
    private Node node;
    private User user;
    private ParameterException parameterException;
    private ParameterVO parameterVO;
    private BooleanParameterVO boolParamVo;
    private DoubleParameterVO doubleParamVo;
    private LongParameterVO longParamVo;
    private MultiSelectVO multiSelVo;
    private SingleSelectVO singleSelectVo;
    private TextParameterVO textParamVo;

    public void onModuleLoad() {
    }
}
