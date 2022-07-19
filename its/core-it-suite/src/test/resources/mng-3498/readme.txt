Tests mojos that fork to another mojo inside the same plugin.

To run, execute the following steps:

1. Build the plugin:

    cd plugin
    mvn clean install
    
2. Build the test project:

    cd project
    mvn validate
