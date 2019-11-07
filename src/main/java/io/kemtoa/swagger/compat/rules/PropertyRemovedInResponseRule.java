package io.kemtoa.swagger.compat.rules;

import io.swagger.models.properties.Property;

/**
 * Removing a required property from an object leads to false expectation
 * on the client receiving the object. If the client is using 'old' service's
 * spec it will expect the property to be present and so it could throw
 * errors. It could be valid to assume that the client won't perform response
 * validation and this could lead to unexpected errors while parsing the
 * response and/or using the missing property.
 */
public class PropertyRemovedInResponseRule extends Rule {

    @Override
    public void acceptProperty(String key, Property left, Property right) {
        if (right == null && location.isResponse()) {
            addError("The property '" + key + "' has been removed in the new spec.");
        }
    }
}
