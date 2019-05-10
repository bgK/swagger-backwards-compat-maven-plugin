package fr.enedis.six.swagger;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import fr.enedis.six.swagger.rules.AddedEnumValueInResponseRule;
import fr.enedis.six.swagger.rules.AddedRequiredRequestParameterRule;
import fr.enedis.six.swagger.rules.CompositeRule;
import fr.enedis.six.swagger.rules.ParameterLocationChangedRule;
import fr.enedis.six.swagger.rules.PropertyRemovedInResponseRule;
import fr.enedis.six.swagger.rules.PropertyTypeChangedRule;
import fr.enedis.six.swagger.rules.RemovedEnumValueInRequestRule;
import fr.enedis.six.swagger.rules.RemovedOperationRule;
import fr.enedis.six.swagger.walker.SwaggerDiffWalker;
import io.swagger.models.Swagger;
import io.swagger.parser.SwaggerParser;

/**
 * Swagger spec backwards compatibility check
 *
 * This Mojo enforces only backwards compatible change are made to the Swagger
 * API specs in the {@link #swaggerSourceDir} directory.
 * Only Swagger API 2.0 specifications in the json format are supported.
 *
 * It works by keeping a copy of the last 'validated' specs document
 * in the {@link #swaggerLockDir} directory. Each time the Mojo is executed,
 * the 'old' and the 'new' specs are compared for backwards incompatible
 * changes. If incompatible changes are found, the execution is failed.
 * Otherwise the execution succeeds and the 'new' spec is copied to
 * the {@link #swaggerLockDir} directory and thus becomes the 'old' spec
 * for the next Mojo execution.
 */
@Mojo(
    name = "backwards-compatibility-check",
    defaultPhase = LifecyclePhase.VERIFY,
    requiresDependencyResolution = ResolutionScope.COMPILE,
    threadSafe = true
)
public class BackwardsCompatibilityCheckMojo extends AbstractMojo {

    @Parameter(defaultValue = "${basedir}/src/main/swagger")
    private String swaggerSourceDir;

    @Parameter(defaultValue = "${basedir}/src/main/swagger")
    private String swaggerLockDir;

    @Parameter(property = "skipSwaggerCheck")
    private boolean skipSwaggerCheck;

    private static class SwaggerGroup {
        private String name;
        private Path jsonPath;
        private Path lockPath;
    }

    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            Path swaggerSourcePath = Paths.get(swaggerSourceDir);
            Path swaggerLockPath = Paths.get(swaggerLockDir);

            if (skipSwaggerCheck) {
                getLog().info("The Swagger backwards compatibility check is skipped.");
                return;
            }

            if (!Files.exists(swaggerSourcePath)) {
                getLog().info("The Swagger source directory does not exist '" + swaggerSourceDir + "', skipping.");
                return;
            }

            Map<String, SwaggerGroup> swaggerGroups = loadSwaggerGroups(swaggerSourcePath, swaggerLockPath);

            for (SwaggerGroup group : swaggerGroups.values()) {
                checkSwaggerGroupBackwardsCompatibility(group);
            }
        } catch (IOException e) {
            throw new MojoExecutionException("An error occurred while running the Swagger compatibility check", e);
        }
    }

    private Map<String, SwaggerGroup> loadSwaggerGroups(Path swaggerSourcePath, Path swaggerLockPath) throws IOException {
        Map<String, SwaggerGroup> swaggerGroups = new HashMap<>();
        DirectoryStream<Path> jsonFilesStream = Files.newDirectoryStream(swaggerSourcePath, "*.json");

        for (Path jsonPath : jsonFilesStream) {
            SwaggerGroup group = new SwaggerGroup();
            group.name = FilenameUtils.removeExtension(jsonPath.getFileName().toString());
            group.jsonPath = jsonPath;

            swaggerGroups.put(group.name, group);
        }

        DirectoryStream<Path> lockFilesStream = Files.newDirectoryStream(swaggerLockPath, "*.lock");

        for (Path lockPath : lockFilesStream) {
            String name = FilenameUtils.removeExtension(lockPath.getFileName().toString());

            swaggerGroups.computeIfAbsent(name, (n) -> new SwaggerGroup());

            SwaggerGroup group = swaggerGroups.get(name);
            group.name = name;
            group.lockPath = lockPath;
        }
        return swaggerGroups;
    }

    private void checkSwaggerGroupBackwardsCompatibility(SwaggerGroup group) throws IOException, MojoFailureException {
        if (group.jsonPath == null) {
            getLog().warn("Found a .lock file without a corresponding .json file: " + group.lockPath.toString());

            throw new MojoFailureException("Backwards compatibility check failed for group " + group.name);
        }

        if (group.lockPath == null) {
            group.lockPath = Paths.get(swaggerLockDir, group.name + ".lock");
            Files.createDirectories(Paths.get(swaggerLockDir));
            Files.copy(group.jsonPath, group.lockPath);
            getLog().info("Initialized compatibility check for group '" + group.name + "'.");
            return;
        }

        Swagger swaggerOld = new SwaggerParser().read(group.lockPath.toAbsolutePath().toString());
        if (swaggerOld == null) {
            throw new MojoFailureException("Unable to parse Swagger lock file: " + group.lockPath);
        }

        Swagger swaggerNew = new SwaggerParser().read(group.jsonPath.toAbsolutePath().toString());
        if (swaggerNew == null) {
            throw new MojoFailureException("Unable to parse Swagger spec: " + group.jsonPath);
        }

        CompositeRule rules = new CompositeRule(
                new AddedEnumValueInResponseRule(),
                new AddedRequiredRequestParameterRule(),
                new ParameterLocationChangedRule(),
                new PropertyRemovedInResponseRule(),
                new PropertyTypeChangedRule(),
                new RemovedEnumValueInRequestRule(),
                new RemovedOperationRule()
        );

        SwaggerDiffWalker walker = new SwaggerDiffWalker();
        walker.walk(rules, swaggerOld, swaggerNew);

        if (!rules.getErrors().isEmpty()) {

            getLog().error("Backwards incompatible changes were found for group '" + group.name + "':");

            for (String error : rules.getErrors()) {
                getLog().error(error);
            }

            throw new MojoFailureException("Backwards compatibility check failed for group " + group.name);
        } else {
            Files.createDirectories(Paths.get(swaggerLockDir));
            Files.copy(group.jsonPath, group.lockPath, StandardCopyOption.REPLACE_EXISTING);
            getLog().info("Backwards compatibility check passed for group '" + group.name + "'.");
        }
    }
}
