This test checks the order of plugin searching. In <2.0.7, Maven searched codehaus before apache.

This is to fix MNG-2926. This test will fail with 2.0.6 and less.