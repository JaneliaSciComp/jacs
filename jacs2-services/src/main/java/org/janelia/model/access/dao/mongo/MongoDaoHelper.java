package org.janelia.model.access.dao.mongo;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.janelia.model.jacs2.page.SortCriteria;
import org.janelia.model.jacs2.page.SortDirection;

import java.util.*;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.eq;

/**
 * Mongo DAO helper.
 */
public class MongoDaoHelper {

    public static <T, R> R findById(Number id, MongoCollection<T> mongoCollection, Class<R> documentType) {
        List<R> entityDocs = find(eq("_id", id), null, 0, 2, mongoCollection, documentType);
        return CollectionUtils.isEmpty(entityDocs) ? null : entityDocs.get(0);
    }

    public static <T, R> List<R> findByIds(Collection<Number> ids, MongoCollection<T> mongoCollection, Class<R> documentType) {
        if (CollectionUtils.isEmpty(ids)) {
            return Collections.<R>emptyList();
        } else {
            return find(Filters.in("_id", ids), null, 0, 0, mongoCollection, documentType);
        }
    }

    public static Bson createBsonSortCriteria(List<SortCriteria> sortCriteria) {
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

    public static <T, R> List<R> find(Bson queryFilter, Bson sortCriteria, long offset, int length, MongoCollection<T> mongoCollection, Class<R> resultType) {
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

    public static <T> long count(Bson queryFilter, MongoCollection<T> mongoCollection) {
        if (queryFilter == null) {
            return mongoCollection.count();
        } else {
            return mongoCollection.count(queryFilter);
        }
    }

    public static <T, I> void delete(MongoCollection<T> mongoCollection, I entityId) {
        mongoCollection.deleteOne(eq("_id", entityId));
    }

}
