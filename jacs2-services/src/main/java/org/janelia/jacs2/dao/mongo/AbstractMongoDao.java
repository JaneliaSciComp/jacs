package org.janelia.jacs2.dao.mongo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.UpdateResult;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.janelia.jacs2.cdi.ObjectMapperFactory;
import org.janelia.jacs2.dao.AbstractDao;
import org.janelia.it.jacs.model.domain.interfaces.HasIdentifier;
import org.janelia.it.jacs.model.domain.support.MongoMapping;
import org.janelia.jacs2.dao.ReadWriteDao;
import org.janelia.jacs2.model.page.PageRequest;
import org.janelia.jacs2.model.page.PageResult;
import org.janelia.jacs2.model.page.SortCriteria;
import org.janelia.jacs2.model.page.SortDirection;
import org.janelia.jacs2.model.DomainModelUtils;
import org.janelia.jacs2.dao.mongo.utils.TimebasedIdentifierGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.eq;

/**
 * Abstract Mongo DAO.
 *
 * @param <T> type of the element
 */
public abstract class AbstractMongoDao<T extends HasIdentifier> extends AbstractDao<T, Number> implements ReadWriteDao<T, Number> {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractMongoDao.class);

    protected final TimebasedIdentifierGenerator idGenerator;
    protected final MongoCollection<T> mongoCollection;
    protected final MongoCollection<T> archiveMongoCollection;

    protected AbstractMongoDao(MongoDatabase mongoDatabase,
                               TimebasedIdentifierGenerator idGenerator) {
        Pair<String, String> entityCollectionNames = getDomainObjectCollectionNames();
        mongoCollection = mongoDatabase.getCollection(entityCollectionNames.getLeft(), getEntityType());
        if (StringUtils.isNotEmpty(entityCollectionNames.getRight())) {
            archiveMongoCollection = mongoDatabase.getCollection(entityCollectionNames.getRight(), getEntityType());
        } else {
            archiveMongoCollection = null;
        }
        this.idGenerator = idGenerator;
    }

    private Pair<String, String> getDomainObjectCollectionNames() {
        Class<T> entityClass = getEntityType();
        MongoMapping mongoMapping = DomainModelUtils.getMapping(entityClass);
        Preconditions.checkArgument(mongoMapping != null, "Entity class " + entityClass.getName() + " is not annotated with MongoMapping");
        return ImmutablePair.of(mongoMapping.collectionName(), mongoMapping.archiveCollectionName());
    }

    @Override
    public T findById(Number id) {
        List<T> entityDocs = find(eq("_id", id), null, 0, 2, getEntityType());
        return CollectionUtils.isEmpty(entityDocs) ? null : entityDocs.get(0);
    }

    @Override
    public List<T> findByIds(Collection<Number> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return Collections.emptyList();
        } else {
            return find(Filters.in("_id", ids), null, 0, 0, getEntityType());
        }
    }

    @Override
    public PageResult<T> findAll(PageRequest pageRequest) {
        List<T> results = find(null,
                createBsonSortCriteria(pageRequest.getSortCriteria()),
                pageRequest.getOffset(),
                pageRequest.getPageSize(),
                getEntityType());
        return new PageResult<>(pageRequest, results);
    }

    protected Bson createBsonSortCriteria(List<SortCriteria> sortCriteria) {
        Bson bsonSortCriteria = null;
        if (CollectionUtils.isNotEmpty(sortCriteria)) {
            Map<String, Object> sortCriteriaAsMap = sortCriteria.stream()
                .filter(sc -> StringUtils.isNotBlank(sc.getField()))
                .collect(Collectors.toMap(
                        SortCriteria::getField,
                        sc -> sc.getDirection() == SortDirection.DESC ? -1 : 1,
                        (sc1, sc2) -> sc2,
                        LinkedHashMap::new));
            bsonSortCriteria = new Document(sortCriteriaAsMap);
        }
        return bsonSortCriteria;
    }

    @Override
    public long countAll() {
        return mongoCollection.count();
    }

    protected <R> List<R> find(Bson queryFilter, Bson sortCriteria, long offset, int length, Class<R> resultType) {
        List<R> entityDocs = new ArrayList<>();
        FindIterable<R> results = mongoCollection.find(resultType);
        if (queryFilter != null) {
            results = results.filter(queryFilter);
        }
        if (offset > 0) {
            results = results.skip((int) offset);
        }
        if (length > 0) {
            results = results.limit(length);
        }
        return results
                .sort(sortCriteria)
                .into(entityDocs);
    }

    @Override
    public void save(T entity) {
        if (entity.getId() == null) {
            entity.setId(idGenerator.generateId());
            mongoCollection.insertOne(entity);
        }
    }

    public void saveAll(List<T> entities) {
        Iterator<Number> idIterator = idGenerator.generateIdList(entities.size()).iterator();
        List<T> toInsert = new ArrayList<>();
        entities.forEach(e -> {
            if (e.getId() == null) {
                e.setId(idIterator.next());
                toInsert.add(e);
            }
        });
        if (!toInsert.isEmpty()) {
            mongoCollection.insertMany(toInsert);
        }
    }

    @Override
    public void update(T entity, Map<String, Object> fieldsToUpdate) {
        UpdateOptions updateOptions = new UpdateOptions();
        updateOptions.upsert(false);
        update(entity, fieldsToUpdate, updateOptions);

    }

    private long update(T entity, Map<String, Object> fieldsToUpdate, UpdateOptions updateOptions) {
        return update(getUpdateMatchCriteria(entity), getUpdates(fieldsToUpdate), updateOptions);
    }

    protected long update(Bson query, Bson toUpdate, UpdateOptions updateOptions) {
        LOG.trace("Update: {} -> {}", query, toUpdate);
        UpdateResult result = mongoCollection.updateOne(query, toUpdate, updateOptions);
        return result.getMatchedCount();
    }

    protected Bson getUpdates(Map<String, Object> fieldsToUpdate) {
        List<Bson> fieldUpdates = fieldsToUpdate.entrySet().stream()
                .map(e -> getFieldUpdate(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
        return Updates.combine(fieldUpdates);
    }

    protected Bson getUpdateMatchCriteria(T entity) {
        return eq("_id", entity.getId());
    }

    @Override
    public void delete(T entity) {
        mongoCollection.deleteOne(eq("_id", entity.getId()));
    }

    @Override
    public void archive(T entity) {
        if (archiveMongoCollection == null) {
            throw new UnsupportedOperationException("Archive is not supported for " + getEntityType());
        }
        archiveMongoCollection.insertOne(entity);
        delete(entity);
    }

    private Bson getFieldUpdate(String fieldName, Object value) {
        if (value == null) {
            return Updates.unset(fieldName);
        } else if (value instanceof Iterable) {
            if (Set.class.isAssignableFrom(value.getClass())) {
                return Updates.addEachToSet(fieldName, ImmutableList.copyOf((Iterable) value));
            } else {
                return Updates.pushEach(fieldName, ImmutableList.copyOf((Iterable) value));
            }
        } else {
            return Updates.set(fieldName, value);
        }
    }
}
