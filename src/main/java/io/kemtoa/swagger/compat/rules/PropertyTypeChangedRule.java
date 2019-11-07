package io.kemtoa.swagger.compat.rules;

import org.apache.commons.lang3.StringUtils;

import io.swagger.models.ArrayModel;
import io.swagger.models.Model;
import io.swagger.models.ModelImpl;
import io.swagger.models.properties.Property;

/**
 * Changing the type of a field is not backward compatible as a client using
 * 'old' Swagger specs will send the field with a different type leading the
 * service to fail to validate the request. On the other end, if the object
 * containing the updated field is used in the response, it will lead to
 * unexpected client errors when parsing the response and/or using the
 * updated property.
 */
public class PropertyTypeChangedRule extends Rule {

    @Override
    public void acceptModel(Model left, Model right) {
        if (left == null || right == null) {
            return; // Handled in other rules
        }

        String leftType = getModelType(left);
        String rightType = getModelType(right);

        if (!StringUtils.equals(leftType, rightType)) {
            addError("The type changed in the new spec: '"
                    + rightType + "' was previously '" + leftType + "'.");
        }

        if (left instanceof ModelImpl && right instanceof ModelImpl) {
            ModelImpl leftModel = (ModelImpl) left;
            ModelImpl rightModel = (ModelImpl) right;

            if (!StringUtils.equals(leftModel.getFormat(), rightModel.getFormat())) {
                addError("The format changed in the new spec: '"
                        + rightModel.getFormat() + "' was previously '" + leftModel.getFormat() + "'.");
            }
        }
    }

    private String getModelType(Model model) {
        String type = null;
        if (model instanceof ModelImpl) {
            type = ((ModelImpl) model).getType();
        } else if (model instanceof ArrayModel) {
            type = ((ArrayModel) model).getType();
        }
        return type;
    }

    @Override
    public void acceptProperty(String key, Property left, Property right) {
        if (left == null || right == null) {
            return; // Handled in other rules
        }

        if (!StringUtils.equals(left.getType(), right.getType())) {
            addError("The type of property '" + key + "' has changed in the new spec: '"
                    + right.getType() + "' was previously '" + left.getType() + "'.");
        }

        if (!StringUtils.equals(left.getFormat(), right.getFormat())) {
            addError("The format of property '" + key + "' has changed in the new spec: '"
                    + right.getFormat() + "' was previously '" + left.getFormat() + "'.");
        }
    }
}
