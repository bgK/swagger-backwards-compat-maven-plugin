package io.kemtoa.swagger.compat.walker;

import io.swagger.models.HttpMethod;
import io.swagger.models.Model;
import io.swagger.models.Operation;
import io.swagger.models.Path;
import io.swagger.models.Response;
import io.swagger.models.parameters.Parameter;
import io.swagger.models.properties.Property;

/**
 * Diff visitor interface for the element types found in Swagger API specifications
 *
 * To be used in conjunction with {@link SwaggerDiffWalker}.
 *
 * Methods are called by the walker for each node found in at least one of the
 * documents to compare. When a node is found only in one of the documents,
 * the corresponding parameter is null.
 */
public interface SwaggerDiffVisitor {
    default void acceptPath(String key, Path left, Path right) {
    }

    default void acceptOperation(HttpMethod operationKey, Operation left, Operation right) {
    }

    default void acceptParameter(Parameter left, Parameter right) {
    }

    default void acceptResponse(String key, Response left, Response right) {
    }

    default void acceptModel(Model left, Model right) {
    }

    default void acceptProperty(String key, Property left, Property right) {
    }

    default void acceptEnumValue(String left, String right) {
    }

    default void setLocation(Location location) {
    }
}
