package fr.enedis.six.swagger.rules;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import fr.enedis.six.swagger.walker.Location;
import io.swagger.models.HttpMethod;
import io.swagger.models.Model;
import io.swagger.models.Operation;
import io.swagger.models.Path;
import io.swagger.models.Response;
import io.swagger.models.parameters.Parameter;
import io.swagger.models.properties.Property;

/**
 * Composite {@link Rule}
 *
 * Delegates all operations to a set of specified Rules. Using this class
 * allows to process multiple rules while walking the Swagger documents
 * only once.
 */
public class CompositeRule extends Rule {
    private final List<Rule> rules;

    public CompositeRule(Rule... rules) {
        this.rules = Arrays.asList(rules);
    }

    @Override
    public List<String> getErrors() {
        List<String> errors = new ArrayList<>();

        rules.forEach(v -> errors.addAll(v.getErrors()));

        return errors;
    }

    @Override
    public void setLocation(Location location) {
        rules.forEach(v -> v.setLocation(location));
    }

    @Override
    public void acceptPath(String key, Path left, Path right) {
        rules.forEach(v -> v.acceptPath(key, left, right));
    }

    @Override
    public void acceptOperation(HttpMethod operationKey, Operation left, Operation right) {
        rules.forEach(v -> v.acceptOperation(operationKey, left, right));
    }

    @Override
    public void acceptParameter(Parameter left, Parameter right) {
        rules.forEach(v -> v.acceptParameter(left, right));
    }

    @Override
    public void acceptResponse(String key, Response left, Response right) {
        rules.forEach(v -> v.acceptResponse(key, left, right));
    }

    @Override
    public void acceptModel(Model left, Model right) {
        rules.forEach(v -> v.acceptModel(left, right));
    }

    @Override
    public void acceptProperty(String key, Property left, Property right) {
        rules.forEach(v -> v.acceptProperty(key, left, right));
    }

    @Override
    public void acceptEnumValue(String left, String right) {
        rules.forEach(v -> v.acceptEnumValue(left, right));
    }
}
