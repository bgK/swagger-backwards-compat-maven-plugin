# Swagger Backwards Compatibility Check Maven Plugin

The <code>swagger-backwards-compat</code> plugin is a Maven plugin to
run a backwards compatibility check on a set of Swagger API 2.0 JSON specification
files. The plugin can be integrated into various phases of a maven build to check 
that any changes to a set of Swagger .json files are backwards compatible.
This ensures that these changes do not impact existing users that haven't had a
chance to update to these latest changes. .lock files are created to keep
track of the state of the .json files. This file is updated when a non-breaking change
is made, and should be checked in along with any other changes.

It is also possible to force any breaking changes and reset the current state
by deleting .lock files. It will then reinitialize the next time the
plugin is run.

## Configuration

```xml
<build>
    <plugins>
        <plugin>
            <groupId>fr.enedis.six</groupId>
            <artifactId>swagger-backwards-compat-maven-plugin</artifactId>
            <version>1.0.0-SNAPSHOT</version>
            <configuration>
                <!-- Optional alternate .json Swagger spec location -->
                <swaggerSourceDir>${basedir}/src/main/swagger</swaggerSourceDir>
                <!-- Optional alternate .lock reference Swagger spec location -->
                <swaggerLockDir>${basedir}/src/main/swagger</swaggerLockDir>
            </configuration>
            <executions>
                <execution>
                    <goals>
                        <goal>backwards-compatibility-check</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

## Acknowledgements
Yelp's `swagger-spec-compatibility` Python library and Salesforce's
`proto-backwards-compat-maven-plugin` were large sources of inspiration:
* https://swagger-spec-compatibility.readthedocs.org/
* https://github.com/salesforce/proto-backwards-compat-maven-plugin
