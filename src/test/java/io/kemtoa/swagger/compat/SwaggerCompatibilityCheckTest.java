package io.kemtoa.swagger.compat;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import io.kemtoa.swagger.compat.rules.AddedEnumValueInResponseRule;
import io.kemtoa.swagger.compat.rules.AddedRequiredRequestParameterRule;
import io.kemtoa.swagger.compat.rules.CompositeRule;
import io.kemtoa.swagger.compat.rules.ParameterLocationChangedRule;
import io.kemtoa.swagger.compat.rules.PropertyRemovedInResponseRule;
import io.kemtoa.swagger.compat.rules.PropertyTypeChangedRule;
import io.kemtoa.swagger.compat.rules.RemovedEnumValueInRequestRule;
import io.kemtoa.swagger.compat.rules.RemovedOperationRule;
import io.kemtoa.swagger.compat.walker.SwaggerDiffWalker;
import io.swagger.models.Swagger;
import io.swagger.parser.SwaggerParser;

@RunWith(Parameterized.class)
public class SwaggerCompatibilityCheckTest {

    private static class TestCase {
        public String oldPath;
        public String newPath;
        public List<String> errors;

        public TestCase(String oldPath, String newPath, String... errors) {
            this.oldPath = oldPath;
            this.newPath = newPath;
            this.errors = Arrays.asList(errors);
        }

        @Override
        public String toString() {
            return "TestCase{" +
                    "oldPath='" + oldPath + '\'' +
                    ", newPath='" + newPath + '\'' +
                    '}';
        }
    }

    @Parameters
    public static Collection<TestCase> data() {
        return Arrays.asList(
                new TestCase("specs/petstore.json", "specs/petstore.json"),
                new TestCase("specs/petstore.json", "specs/petstore-added-parameter.json",
                        "Path /store/order/{orderId}, Operation GET, Parameter newParam : The required parameter 'newParam' has been added in the new spec."
                ),
                new TestCase("specs/petstore.json", "specs/petstore-removed-enum-value.json",
                        "Path /user/createWithArray, Operation POST, Parameter body, Property sex : The enum value 'UNKNOWN' has been removed in the new spec.",
                        "Path /user/{username}, Operation PUT, Parameter body, Property sex : The enum value 'UNKNOWN' has been removed in the new spec.",
                        "Path /user, Operation POST, Parameter body, Property sex : The enum value 'UNKNOWN' has been removed in the new spec."
                ),
                new TestCase("specs/petstore.json", "specs/petstore-removed-operation.json",
                        "Path /pet, Operation POST : The operation was removed in the new spec.",
                        "Path /user/createWithList : The path was removed in the new spec."
                ),
                new TestCase("specs/recursive.json", "specs/recursive.json"),
                new TestCase("specs/uber.json", "specs/uber.json"),
                new TestCase("specs/uber.json", "specs/uber-removed-property.json",
                        "Path /products, Operation GET, Response 200, Property items, Property image : The property 'image' has been removed in the new spec.",
                        "Path /estimates/time, Operation GET, Response 200, Property items, Property image : The property 'image' has been removed in the new spec."
                ),
                new TestCase("specs/uber.json", "specs/uber-type-changed.json",
                        "Path /me, Operation GET, Response 200, Property promo_code : The type of property 'promo_code' has changed in the new spec: 'integer' was previously 'string'."
                ),
                new TestCase("specs/uber.json", "specs/uber-parameter-location-changed.json",
                        "Path /products, Operation GET, Parameter latitude : The location of parameter 'latitude' has changed in the new spec: 'header' previously was 'query'."
                ),
                new TestCase("specs/uber.json", "specs/uber-added-enum-value.json",
                        "Path /me, Operation GET, Response 200, Property status : The enum value 'IN_BETWEEN' has been added in the new spec."
                )
        );
    }

    @Parameter
    public TestCase testCase;

    @Test
    public void test() {
        Swagger swaggerLeft = new SwaggerParser().read(testCase.oldPath);
        Swagger swaggerRight = new SwaggerParser().read(testCase.newPath);

        CompositeRule rules = new CompositeRule(
                new PropertyRemovedInResponseRule(),
                new PropertyTypeChangedRule(),
                new RemovedEnumValueInRequestRule(),
                new AddedEnumValueInResponseRule(),
                new AddedRequiredRequestParameterRule(),
                new ParameterLocationChangedRule(),
                new RemovedOperationRule()
        );

        SwaggerDiffWalker walker = new SwaggerDiffWalker();
        walker.walk(rules, swaggerLeft, swaggerRight);

        assertEquals(testCase.errors.size(), rules.getErrors().size());

        for (String error : testCase.errors) {
            assertThat(rules.getErrors(), hasItem(error));
        }
    }
}
