This test needs to build a JAR and then a plugin to use it. The root pom does that. The verifier then goes into test-project 
and executes mvn verify to execute the plugin. I did this to make sure the verifier picked up the correct 
artifacts by making a separate execution.

This test also shows that any resources that are required by plugins should be specified as dependencies and not extensions.