Verifies that references to ${project.build.directory} are interpolated with absolute path values, even when referenced in places that the pathTranslator doesn't touch, like properties. Simply run:

mvn validate

to see this test work. The maven-it-plugin-project-interpolation does the rest, verifying that the <myDirectory/> property has been interpolated to the result of ${project.build.directory}/foo.