package io.kemtoa.swagger.compat.rules;

/**
 * Adding an enum value to a request response is backward incompatible
 * as clients using the 'old' version of the Swagger specs, will not be
 * able to properly validate the response.
 */
public class AddedEnumValueInResponseRule extends Rule {

    @Override
    public void acceptEnumValue(String left, String right) {
        if (left == null && location.isResponse()) {
            addError("The enum value '" + right + "' has been added in the new spec.");
        }
    }
}
