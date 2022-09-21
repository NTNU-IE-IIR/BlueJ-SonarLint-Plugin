# Some pointers in order to run release using the Maven Release Plugin

# maven-release-plugin-example


### Cleaning a Release

1. Delete the release descriptor (release.properties)

2. Delete any backup POM files

```
mvn release:clean
```



### Preparing the Release

1. Perform some checks - there should be no uncommitted changes and the project should depend on no SNAPSHOT dependencies

2. Change the version of the project in the pom file to a full release number (remove SNAPSHOT suffix) - in our example - 0.1

3. Run the project test suites

4. Commit and push the changes

5. Create the tag out of this non-SNAPSHOT versioned code

6. Increase the version of the project in the pom - in our example - 0.2-SNAPSHOT

7. Commit and push the changes


### Dry Run

Allows you to run all operations in `release:prepare` goal except for actual commits into SCM.

```
mvn release:prepare -DdryRun=true
```


### Performing the Release

1. Checkout release tag from SCM

2. Build and deploy released code

3. Relies on the output of the Prepare step - the `release.properties`.

To avoid the deploy-step to be executed (which would try to deploy the artifact to a server for distribution), add `-Darguments="-Dmaven.deploy.skip=true"`
```
mvn release:perform -Darguments="-Dmaven.deploy.skip=true"
```

### Cleaning, Preparing and Performing the release

You can perform all 3 steps in one go:
```
mvn release:clean release:prepare release:perform -DreleaseVersion=0.1 -DdevelopmentVersion=0.2-SNAPSHOT -Darguments="-Dmaven.deploy.skip=true"
```