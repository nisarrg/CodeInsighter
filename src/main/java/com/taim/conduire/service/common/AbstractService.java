package com.taim.conduire.service.common;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.List;

public interface AbstractService<T extends Serializable, ID extends Serializable> {

    JpaRepository<T, ID> getRepository();

    @Transactional(readOnly = true)
    default T getOne(ID entityId) {
        return getRepository().getOne(entityId);
    }

    @Transactional(readOnly = true)
    default List<T> findAll() {
        return getRepository().findAll();
    }

    @Transactional
    default T create(T entity) {
        return getRepository().save(entity);
    }

    @Transactional
    default T update(T entity) {
        return getRepository().save(entity);
    }

    @Transactional
    default void delete(T entity) {
        getRepository().delete(entity);
    }

    @Transactional
    default void deleteById(ID entityId) {
        getRepository().delete((T) entityId);
    }
}
