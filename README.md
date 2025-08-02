# RuntimeSave

RuntimeSave is an IntelliJ IDEA extension to save the runtime values of variables in a graph database and load them for various purposes.

## Setup

Make sure you have JDK 21+ installed.

1. Install [Neo4j](https://neo4j.com/docs/operations-manual/current/installation/) and [APOC Core](https://neo4j.com/docs/apoc/current/installation/).
2. Change `initial.dbms.default_database` in [neo4j.conf](https://neo4j.com/docs/operations-manual/current/database-administration/standard-databases/configuration-parameters/) to `runtimesave`.
3. Set the [password](https://neo4j.com/docs/operations-manual/current/configuration/set-initial-password/) for the `neo4j` user to any value.
4. Open this project in IntelliJ IDEA.
5. Execute the `runIde` Gradle task with the environment variable `NEO4J_PASSWORD` set to the given password.  

In the case you are viewing this repository through the Anonymous GitHub service, you may need to download [a ZIP file from OSF.io](https://osf.io/8bduk?view_only=cb39523a5f29406fa6fa8f80ed5eea69) for a properly compilable (but possibly less up-to-date) version.

## Usage

In IntelliJ IDEA with the plugin installed (e.g., via `runIde` Gradle task), you can perform various actions.

To add a savepoint, right-click on the given line number in the left gutter and select Add Savepoint. Then you can run code (e.g., a test) that includes the execution of the given line, to save sample values of variables.

For saving sample values from all executed lines, choose the menu Run / Collect All Lines in '(configuration name)'.

To view the saved objects in the graph database, open http://localhost:7474/.

To start a program from any line, place the caret on the given line and select Run / Start Program at Current Line from the main menu. In the opened dialog, click Debug.

## Tests and the Comparison of Approaches

Unit tests, including the tests of the hashing algorithms, are located in the `plugin/src/test/java` directory. They can be run using: `./gradlew test`.

The comparison of approaches (plain, packed, hashed, and pack+hash) is implemented in `Comparison.java` inside the `plugin/src/test/java/com/github/sulir/runtimesave/comparison` directory. It can be executed using its `main` method (the `NEO4J_PASSWORD` environment variable has to be set).
