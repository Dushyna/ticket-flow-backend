package io.github.dushyna.ticketflow.common;

import java.util.Collection;


/**
 * Utility class for handling operations related to entities, such as generating string
 * representations for the IDs of entities or collections of entities. Provides utility
 * methods for use in toString implementations or logging/debugging purposes.
 */
public abstract class EntityUtil {

    private static final String EMPTY_COLLECTION = "[]";
    private static final String ENTITY_NULL = "null";

    private EntityUtil() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Generates a string representation of the IDs of all entities in the given collection.
     * If the collection is null or empty, an empty collection representation ("[]") is returned.
     *
     * @param entities a collection of BaseEntity objects from which IDs are extracted
     * @return a string representation of the list of IDs of the entities in the collection,
     * or "[]" if the collection is null or empty
     */
    public static String getIdsForToString(Collection<? extends BaseEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return EMPTY_COLLECTION;
        }
        return entities.stream()
                .map(BaseEntity::getId)
                .map(Object::toString)
                .toList()
                .toString();
    }

    /**
     * Retrieves a string representation of the ID of the given entity.
     * If the entity is null, a predefined "null" string is returned.
     * Otherwise, the string representation of the entity's ID is returned.
     *
     * @param <E>    the type of the entity that extends {@code BaseEntity}
     * @param entity the entity whose ID is to be retrieved; can be null
     * @return the ID of the entity as a string if the entity is not null,
     * or the string "null" if the entity is null
     */
    public static <E extends BaseEntity> String getIdForToString(E entity) {
        return entity == null ? ENTITY_NULL : entity.getId().toString();
    }
}
