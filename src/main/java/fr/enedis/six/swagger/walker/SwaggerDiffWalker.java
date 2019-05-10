package fr.enedis.six.swagger.walker;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import io.swagger.models.ArrayModel;
import io.swagger.models.HttpMethod;
import io.swagger.models.Model;
import io.swagger.models.ModelImpl;
import io.swagger.models.Operation;
import io.swagger.models.Path;
import io.swagger.models.RefModel;
import io.swagger.models.Response;
import io.swagger.models.Swagger;
import io.swagger.models.parameters.BodyParameter;
import io.swagger.models.parameters.Parameter;
import io.swagger.models.properties.ArrayProperty;
import io.swagger.models.properties.ObjectProperty;
import io.swagger.models.properties.Property;
import io.swagger.models.properties.RefProperty;
import io.swagger.models.properties.StringProperty;

/**
 * Swagger API tool for comparing two specification documents.
 *
 * Traverses a pair of Swagger 2.0 json documents, calling the specified instance
 * of {@link SwaggerDiffVisitor} for each encountered node present in at least
 * one of the documents.
 *
 * Keeps track of the position in the tree using {@link Location}.
 */
public class SwaggerDiffWalker {

    private Swagger swaggerLeft;
    private Swagger swaggerRight;
    private Location location = new Location();

    private Set<Model> visitedModels = new HashSet<>(); // Used to prevent infinite recursion

    public void walk(SwaggerDiffVisitor visitor, Swagger swaggerLeft, Swagger swaggerRight) {
        this.swaggerLeft = swaggerLeft;
        this.swaggerRight = swaggerRight;

        visitor.setLocation(location);

        Set<String> leftKeys  = swaggerLeft.getPaths() != null ? swaggerLeft.getPaths().keySet() : new HashSet<>();
        Set<String> rightKeys = swaggerRight.getPaths() != null ? swaggerRight.getPaths().keySet() : new HashSet<>();
        Stream.concat(
                leftKeys.stream(),
                rightKeys.stream()
        ).distinct().forEach(key -> doVisitAndRecurse(
                visitor, key,
                swaggerLeft.getPath(key),
                swaggerRight.getPath(key))
        );
    }

    private void doVisitAndRecurse(SwaggerDiffVisitor visitor, String pathKey, Path left, Path right) {
        location.pushPath("Path " + pathKey);

        try {
            visitor.acceptPath(pathKey, left, right);

            if (left == null || right == null) {
                return;
            }

            Set<HttpMethod> leftKeys = left.getOperationMap().keySet();
            Set<HttpMethod> rightKeys = right.getOperationMap().keySet();
            Stream.concat(
                    leftKeys.stream(),
                    rightKeys.stream()
            ).distinct().forEach(key -> doVisitAndRecurse(visitor, key,
                    left.getOperationMap().get(key),
                    right.getOperationMap().get(key))
            );
        } finally {
            location.popPath();
        }
    }

    private void doVisitAndRecurse(SwaggerDiffVisitor visitor, HttpMethod operationKey, Operation left, Operation right) {
        location.pushPath("Operation " + operationKey);

        try {
            visitor.acceptOperation(operationKey, left, right);

            if (left == null || right == null) {
                return;
            }

            Stream.concat(
                    left.getParameters().stream().map(Parameter::getName),
                    right.getParameters().stream().map(Parameter::getName)
            ).distinct().forEach(name -> {
                Parameter leftParam = left.getParameters().stream().filter(param -> param.getName().equals(name)).findAny().orElse(null);
                Parameter rightParam = right.getParameters().stream().filter(param -> param.getName().equals(name)).findAny().orElse(null);
                doVisitAndRecurse(visitor, leftParam, rightParam);
            });

            Set<String> leftKeys  = left.getResponses() != null ? left.getResponses().keySet() : new HashSet<>();
            Set<String> rightKeys = right.getResponses() != null ? right.getResponses().keySet() : new HashSet<>();
            Stream.concat(
                    leftKeys.stream(),
                    rightKeys.stream()
            ).distinct().forEach(key -> doVisitAndRecurse(visitor, key,
                    left.getResponses().get(key),
                    right.getResponses().get(key))
            );
        } finally {
            location.popPath();
        }
    }

    private void doVisitAndRecurse(SwaggerDiffVisitor visitor, Parameter left, Parameter right) {
        location.pushPath("Parameter " + (left != null ? left.getName() : right.getName()));
        location.setRequest(true);

        try {
            visitor.acceptParameter(left, right);

            if (left == null || right == null) {
                return;
            }

            if (left instanceof BodyParameter || right instanceof BodyParameter) {
                doVisitAndRecurse(visitor,
                        Optional.of(left).map(BodyParameter.class::cast).map(BodyParameter::getSchema).orElse(null),
                        Optional.of(right).map(BodyParameter.class::cast).map(BodyParameter::getSchema).orElse(null)
                );
            }
        } finally {
            visitedModels.clear();
            location.setRequest(false);
            location.popPath();
        }
    }

    private void doVisitAndRecurse(SwaggerDiffVisitor visitor, String key, Response left, Response right) {
        location.pushPath("Response " + key);
        location.setResponse(true);

        try {
            visitor.acceptResponse(key, left, right);

            if (left == null || right == null) {
                return;
            }

            if (left.getResponseSchema() == null && right.getResponseSchema() == null) {
                return;
            }

            doVisitAndRecurse(visitor, left.getResponseSchema(), right.getResponseSchema());
        } finally {
            visitedModels.clear();
            location.setResponse(false);
            location.popPath();
        }
    }

    private void doVisitAndRecurse(SwaggerDiffVisitor visitor, Model left, Model right) {
        if (visitedModels.contains(left) && visitedModels.contains(right)) {
            // Prevent infinite recursion
            return;
        }

        visitor.acceptModel(left, right);

        if (left != null) {
            visitedModels.add(left);
        }
        if (right != null) {
            visitedModels.add(right);
        }

        if (left == null || right == null) {
            return;
        }

        if (left instanceof ArrayModel && right instanceof ArrayModel) {
            doVisitAndRecurse(visitor, "items", ((ArrayModel) left).getItems(), ((ArrayModel) right).getItems());
        } else if (left instanceof ModelImpl && right instanceof ModelImpl) {
            doVisitEnumValues(visitor, ((ModelImpl) left).getEnum(), ((ModelImpl) right).getEnum());
            doVisitAndRecurse(visitor, left.getProperties(), right.getProperties());
        } else if (left instanceof RefModel && right instanceof RefModel) {
            RefModel leftRef  = (RefModel) left;
            RefModel rightRef = (RefModel) right;

            Model leftModel = resolveModel(swaggerLeft, leftRef.getSimpleRef());
            Model rightModel = resolveModel(swaggerRight, rightRef.getSimpleRef());

            doVisitAndRecurse(visitor, leftModel, rightModel);
        }
    }

    private Model resolveModel(Swagger swagger, String simpleRef) {
        Map<String, Model> definitions = swagger.getDefinitions();
        if (definitions == null) {
            return null;
        }

        return definitions.get(simpleRef);
    }

    private void doVisitAndRecurse(SwaggerDiffVisitor visitor, Map<String, Property> left, Map<String, Property> right) {
        if (left == null || right == null) {
            return;
        }

        Set<String> leftKeys  = left.keySet();
        Set<String> rightKeys = right.keySet();
        Stream.concat(
                leftKeys.stream(),
                rightKeys.stream()
        ).distinct().forEach(key -> doVisitAndRecurse(visitor, key,
                left.get(key),
                right.get(key))
        );
    }

    private void doVisitAndRecurse(SwaggerDiffVisitor visitor, String name, Property left, Property right) {
        location.pushPath("Property " + name);

        try {
            visitor.acceptProperty(name, left, right);

            if (left == null || right == null) {
                return;
            }

            if (left instanceof RefProperty && right instanceof RefProperty) {
                RefProperty leftRef  = (RefProperty) left;
                RefProperty rightRef = (RefProperty) right;

                Model leftModel = resolveModel(swaggerLeft, leftRef.getSimpleRef());
                Model rightModel = resolveModel(swaggerRight, rightRef.getSimpleRef());

                doVisitAndRecurse(visitor, leftModel, rightModel);
            } else if (left instanceof ObjectProperty && right instanceof ObjectProperty) {
                ObjectProperty leftObject  = (ObjectProperty) left;
                ObjectProperty rightObject = (ObjectProperty) right;

                doVisitAndRecurse(visitor, leftObject.getProperties(), rightObject.getProperties());
            } else if (left instanceof ArrayProperty && right instanceof ArrayProperty) {
                ArrayProperty leftArray = (ArrayProperty) left;
                ArrayProperty rightArray = (ArrayProperty) right;

                doVisitAndRecurse(visitor, "items", leftArray.getItems(), rightArray.getItems());
            } else if (left instanceof StringProperty && right instanceof StringProperty) {
                StringProperty leftString = (StringProperty) left;
                StringProperty rightString = (StringProperty) right;

                doVisitEnumValues(visitor, leftString.getEnum(), rightString.getEnum());
            }
        } finally {
            location.popPath();
        }
    }

    private void doVisitEnumValues(SwaggerDiffVisitor visitor, List<String> leftValues, List<String> rightValues) {
        Set<String> enumValues = new HashSet<>();
        if (leftValues != null) {
            enumValues.addAll(leftValues);
        }
        if (rightValues != null) {
            enumValues.addAll(rightValues);
        }

        for (String value : enumValues) {
            visitor.acceptEnumValue(
                    leftValues != null && leftValues.contains(value) ? value : null,
                    rightValues != null && rightValues.contains(value) ? value : null
            );
        }
    }
}
