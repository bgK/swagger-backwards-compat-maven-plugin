package fr.enedis.six.swagger.rules;

import java.util.ArrayList;
import java.util.List;

import fr.enedis.six.swagger.walker.Location;
import fr.enedis.six.swagger.walker.SwaggerDiffVisitor;

/**
 * Base class for the backwards compatibility check rules
 *
 * Two Swagger specifications are deemed compatible when a set
 * of rules are verified when comparing the documents.
 */
public abstract class Rule implements SwaggerDiffVisitor {
    protected Location location;
    private final List<String> errors = new ArrayList<>();

    @Override
    public void setLocation(Location location) {
        this.location = location;
    }

    public void addError(String message) {
        errors.add(location.getFullLocation() + " : " + message);
    }

    public List<String> getErrors() {
        return errors;
    }
}
