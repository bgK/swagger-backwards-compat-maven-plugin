package fr.enedis.six.swagger.rules;

import io.swagger.models.parameters.Parameter;
import io.swagger.models.properties.Property;

/**
 * Adding a required property to a request parameter causes client requests
 * to fail if the property is not present.
 */
public class AddedRequiredRequestParameterRule extends Rule {

    @Override
    public void acceptParameter(Parameter left, Parameter right) {
        if (left == null && right.getRequired()) {
            addError("The required parameter '" + right.getName() + "' has been added in the new spec.");
        }
    }

    @Override
    public void acceptProperty(String key, Property left, Property right) {
        if (left == null && right.getRequired() && location.isRequest()) {
            addError("The required property '" + key + "' has been added in the new spec.");
        }
    }
}
