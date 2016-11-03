package org.janelia.it.jacs.compute.access;

import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.NonUniqueResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

import com.google.common.collect.ImmutableMap;

public abstract class AbstractBaseDAO {

    private EntityManager entityManager;

    public AbstractBaseDAO(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public <T> T findByNumericId(Number id, Class<T> resultType) {
        return entityManager.find(resultType, id);
    }

    protected <T> List<T> findAll(int offset, int length, Class<T> resultType) {
        return this.findByQueryParamsWithPaging("select a from " + resultType.getSimpleName() + " a",
                ImmutableMap.<String, Object>of(),
                offset, length,
                resultType);
    }

    protected <T> T findFirst(String queryString, Map<String, Object> queryParams, Class<T> resultType) {
        List<T> results = findByQueryParamsWithPaging(queryString, queryParams, 0, 1, resultType);
        if (results.size() > 0) {
            return results.get(0);
        } else {
            return null;
        }
    }

    protected <T> T findFirstUsingNamedQuery(String namedQuery, Map<String, Object> queryParams, Class<T> resultType) {
        TypedQuery<T> query = this.prepareNamedQuery(namedQuery, queryParams, resultType);
        query.setFirstResult(0);
        query.setMaxResults(1);

        List<T> results = query.getResultList();
        if (results.size() > 0) {
            return results.get(0);
        } else {
            return null;
        }
    }

    protected  <T> List<T> findByQueryParams(String queryString, Map<String, Object> queryParams, Class<T> resultType) {
        return this.prepareQuery(queryString, queryParams, resultType).getResultList();
    }

    protected  <T> List<T> findByQueryParamsWithPaging(String queryString, Map<String, Object> queryParams, int offset, int length, Class<T> resultType) {
        TypedQuery<T> query = this.prepareQuery(queryString, queryParams, resultType);
        if (offset > 0) query.setFirstResult(offset);
        if (length > 0) query.setMaxResults(length);
        return query.getResultList();
    }

    protected <T> T getAtMostOneResult(TypedQuery<T> query) {
        query.setMaxResults(2);
        List<T> results = query.getResultList();
        if (results.isEmpty()) {
            return null;
        } else if (results.size() >= 1) {
            return results.get(0);
        } else {
            throw new NonUniqueResultException();
        }
    }

    protected <T> T getSingleResultUsingNamedQuery(String namedQuery, Map<String, Object> queryParams, Class<T> resultType) {
        return prepareNamedQuery(namedQuery, queryParams, resultType).getSingleResult();
    }

    protected <T> TypedQuery<T> prepareNamedQuery(String namedQuery, Map<String, Object> queryParams, Class<T> resultType) {
        TypedQuery<T> query = entityManager.createNamedQuery(namedQuery, resultType);
        setQueryParameters(query, queryParams);
        return query;
    }

    protected <T> TypedQuery<T> prepareQuery(String queryString, Map<String, Object> queryParams, Class<T> resultType) {
        TypedQuery<T> query = entityManager.createQuery(queryString, resultType);
        setQueryParameters(query, queryParams);
        return query;
    }

    protected int executeNativeStmt(String execStmt, Map<String, Object> queryParams) {
        Query query = entityManager.createNativeQuery(execStmt);
        setQueryParameters(query, queryParams);
        return query.executeUpdate();
    }

    protected void setQueryParameters(Query query, Map<String, Object> queryParams) {
        for (Map.Entry<String, Object> paramEntry : queryParams.entrySet()) {
            query.setParameter(paramEntry.getKey(), paramEntry.getValue());
        }
    }

    protected <T> void save(T entity) {
        entityManager.persist(entity);
    }

    protected <T> void update(T entity) {
        entityManager.merge(entity);
    }

    protected <T> void delete(T entity) {
        entityManager.remove(entity);
    }
}
