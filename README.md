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
            <groupId>io.kemtoa.swagger</groupId>
            <artifactId>swagger-backwards-compat-maven-plugin</artifactId>
            <version>1.0.0</version>
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

## Rules
The following rules are enforced when checking for backwards incompatible changes:
* **PropertyRemovedInResponseRule**: A property was removed from a response.

    Clients using the old spec may rely on it being present and fail with
    the new implementation.

* **PropertyTypeChangedRule**: The type of a property was changed.

    Clients using the old spec may fail to interpret the new type. This rule triggers
    on all type changes. There is no such thing as backwards compatible types at the
    moment.

* **AddedRequiredRequestParameterRule**: A new required parameter was added to a request.

    Clients using the old spec don't provide the new parameter resulting in the request
    being rejected by the server.

* **ParameterLocationChangedRule**: The way a parameter is transmitted in the request changed.

    For example a parameter that was passed in the query string is now passed as a header.
    Clients using the old spec provide the parameter at the old location resulting in the
    request being rejected by the server.

* **RemovedOperationRule**: An endpoint path or a verb was removed.

    Clients using the old spec will keep calling the removed endpoint, resulting in the
    request being rejected by the server.

* **AddedEnumValueInResponseRule**: An enumeration value was added in an object used in a response.

    Clients using the old spec may fail to interpret the new value.

* **RemovedEnumValueInRequestRule**: An enumeration value was removed in an object used in a request.

    Client using the old spec may keep sending the old value, resulting in the request
    being rejected by the server.

This plugin not detecting backwards incompatible changes does not mean there are not.
However, it does covers the most usual cases. Only the API specification is verified,
this plugin cannot detect backwards incompatible changes in the service implementation.

## Acknowledgements
Yelp's `swagger-spec-compatibility` Python library and Salesforce's
`proto-backwards-compat-maven-plugin` were large sources of inspiration:
* https://swagger-spec-compatibility.readthedocs.org/
* https://github.com/salesforce/proto-backwards-compat-maven-plugin
