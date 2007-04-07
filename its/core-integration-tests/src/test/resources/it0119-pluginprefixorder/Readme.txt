This test checks the order of plugin searching. In <2.0.7, Maven searched codehaus before apache.

This test also verifies that prefixes set by a user in the settings are searched first before the standard ones.

This is to fix MNG-2926. This test will fail with Maven <=2.0.6.