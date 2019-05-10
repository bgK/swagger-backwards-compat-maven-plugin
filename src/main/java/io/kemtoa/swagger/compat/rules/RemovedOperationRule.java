package io.kemtoa.swagger.compat.rules;

import io.swagger.models.HttpMethod;
import io.swagger.models.Operation;
import io.swagger.models.Path;

/**
 * Removing endpoints is a backwards incompatible change as existing clients
 * could keep calling now missing endpoints.
 */
public class RemovedOperationRule extends Rule {

    @Override
    public void acceptPath(String key, Path left, Path right) {
        if (right == null) {
            addError("The path was removed in the new spec.");
        }
    }

    @Override
    public void acceptOperation(HttpMethod operationKey, Operation left, Operation right) {
        if (right == null) {
            addError("The operation was removed in the new spec.");
        }
    }
}
