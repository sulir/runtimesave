# RuntimeSave

RuntimeSave is an IntelliJ IDEA extension to save the runtime values of variables in a graph database and load them for various purposes.

## Setup

Make sure you have JDK 21+, CMake and a C++ compiler installed.

1. Install [Neo4j](https://neo4j.com/docs/operations-manual/current/installation/) and [APOC Core](https://neo4j.com/docs/apoc/current/installation/).
2. Change `initial.dbms.default_database` in [neo4j.conf](https://neo4j.com/docs/operations-manual/current/database-administration/standard-databases/configuration-parameters/) to `runtimesave`.
3. Set the [password](https://neo4j.com/docs/operations-manual/current/configuration/set-initial-password/) for the `neo4j` user to any value.
4. Open this project in IntelliJ IDEA.
5. Execute the `runIde` Gradle task with the environment variable `NEO4J_PASSWORD` set to the given password.

## Usage

In IntelliJ IDEA with the plugin installed (e.g., via `runIde` Gradle task), you can perform various actions.

To add a savepoint, right-click on the given line number in the left gutter and select Add Savepoint. In the Run/Debug Configuration dialog, check "Enable savepoints" under RuntimeSave savepoints settings. Then you can run code (e.g., a test) that includes the execution of the given line, in debugging mode. This will save values of variables each time the savepoint is hit.

For saving sample values from all executed lines, choose the menu Run / Sample Values from '(configuration name)'. You can adjust sampling options in the given Run/Debug Configuration dialog under RuntimeSave sampling settings.

To view the saved objects in the graph database, open http://localhost:7474/.

To start a program from any line, place the caret on the given line and select Run / Start Program at Current Line from the main menu. In the opened dialog, click Debug.

## Tests

Unit tests, including the tests of the hashing algorithms, are located in the `graph/src/test/java` directory. They can be run using: `./gradlew test`.
