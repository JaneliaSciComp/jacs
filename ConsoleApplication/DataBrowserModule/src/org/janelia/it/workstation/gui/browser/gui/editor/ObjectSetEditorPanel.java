package org.janelia.it.workstation.gui.browser.gui.editor;

import java.awt.BorderLayout;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.JPanel;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.ontology.Annotation;
import org.janelia.it.jacs.model.domain.support.DomainUtils;
import org.janelia.it.jacs.model.domain.workspace.ObjectSet;
import org.janelia.it.jacs.shared.utils.ReflectionUtils;
import org.janelia.it.workstation.gui.browser.api.DomainMgr;
import org.janelia.it.workstation.gui.browser.api.DomainModel;
import org.janelia.it.workstation.gui.browser.events.selection.DomainObjectSelectionModel;
import org.janelia.it.workstation.gui.browser.gui.listview.PaginatedResultsPanel;
import org.janelia.it.workstation.gui.browser.gui.support.SearchProvider;
import org.janelia.it.workstation.gui.browser.model.search.ResultPage;
import org.janelia.it.workstation.gui.browser.model.search.SearchResults;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;

/**
 * Simple editor panel for viewing object sets. In the future it may support drag and drop editing of object sets. 
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ObjectSetEditorPanel extends JPanel implements DomainObjectSelectionEditor<ObjectSet>, SearchProvider {

    private final static Logger log = LoggerFactory.getLogger(ObjectSetEditorPanel.class);
    
    private final PaginatedResultsPanel resultsPanel;
    
    private final DomainObjectSelectionModel selectionModel = new DomainObjectSelectionModel();

    private List<DomainObject> domainObjects;
    private List<Annotation> annotations;
    
    public ObjectSetEditorPanel() {
        
        setLayout(new BorderLayout());
        
        resultsPanel = new PaginatedResultsPanel(selectionModel, this) {
            @Override
            protected ResultPage getPage(SearchResults searchResults, int page) throws Exception {
                return searchResults.getPage(page);
            }
        };
        add(resultsPanel, BorderLayout.CENTER);
    }
    
    @Override
    public void loadDomainObject(final ObjectSet objectSet) {

        log.debug("loadDomainObject(ObjectSet:{})",objectSet.getName());
        selectionModel.setParentObject(objectSet);
        
        resultsPanel.showLoadingIndicator();
        
        SimpleWorker childLoadingWorker = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {
                DomainModel model = DomainMgr.getDomainMgr().getModel();
                domainObjects = model.getDomainObjects(objectSet.getClassName(), objectSet.getMembers());
                annotations = model.getAnnotations(DomainUtils.getReferences(domainObjects));
                log.info("Showing "+domainObjects.size()+" items");
            }

            @Override
            protected void hadSuccess() {
        		showResults();
            }

            @Override
            protected void hadError(Throwable error) {
                resultsPanel.showNothing();
                SessionMgr.getSessionMgr().handleException(error);
            }
        };

        childLoadingWorker.execute();
    }

	public void setSortField(final String sortCriteria) {

        resultsPanel.showLoadingIndicator();

        SimpleWorker worker = new SimpleWorker() {
        
            @Override
            protected void doStuff() throws Exception {
                final String sortField = (sortCriteria.startsWith("-")||sortCriteria.startsWith("+")) ? sortCriteria.substring(1) : sortCriteria;
                final boolean ascending = !sortCriteria.startsWith("-");
        		Collections.sort(domainObjects, new Comparator<DomainObject>() {
					@Override
					@SuppressWarnings({ "rawtypes", "unchecked" })
        			public int compare(DomainObject o1, DomainObject o2) {
        				try {
        	                // TODO: speed could be improved by moving the reflection calls outside of the sort
        					Comparable v1 = (Comparable)ReflectionUtils.get(o1, sortField);
        					Comparable v2 = (Comparable)ReflectionUtils.get(o2, sortField);
        					Ordering ordering = Ordering.natural().nullsLast();
        					if (!ascending) {
        						ordering = ordering.reverse();
        					}
    		                return ComparisonChain.start().compare(v1, v2, ordering).result();
        				}
        				catch (Exception e) {
        					log.error("Problem encountered when sorting DomainObjects",e);
        					return 0;
        				}
        			}
        		});	
            }

            @Override
            protected void hadSuccess() {
        		showResults();
            }

            @Override
            protected void hadError(Throwable error) {
                resultsPanel.showNothing();
                SessionMgr.getSessionMgr().handleException(error);
            }
        };
        
        worker.execute();
        
	}
	
	public void showResults() {
        SearchResults searchResults = SearchResults.paginate(domainObjects, annotations);
        resultsPanel.showSearchResults(searchResults);
	}
	
	public void search() {
		// Nothing needs to be done here, because results were updated by setSortField()
	}
	
    @Override
    public String getName() {
        return "Object Set Editor";
    }
    
    @Override
    public DomainObjectSelectionModel getSelectionModel() {
        return selectionModel;
    }
    
    @Override
    public Object getEventBusListener() {
        return resultsPanel;
    }
}
