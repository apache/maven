This is a sample IT based on a real one. This test first installs a jar containing some checkstyle rules. The second invocation uses that jar as an extension
so that checkstyle finds the resources. If Maven behaves properly, the build succeeds, if not, the build fails. This is the simplest way to structure an IT when
possible as it's cut and dry and is very easy to check with the verifier.