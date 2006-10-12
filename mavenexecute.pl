$dirname = "C:\\svn\\maven\\trunk\\maven-core-it";
$readme = <<'END';
it0000: The simplest of builds. We have one application class and one test
        class. There are no resources, no source generation, no resource
        generation and a the super model is employed to provide the build
        information.

it0001: Builds upon it0000: we add an application resource that is packaged
        up in the resultant JAR.

it0002: Builds upon it0001: we add the download of a dependency. We delete
        the JAR from the local repository and make sure it is there post build.
       
it0003: Builds upon it0001: we add a jar installation step. We delete the JAR
        from the local repository to make sure it is there post build.
       
it0004: The simplest of pom installation. We have a pom and we install it in
        local repository.
       
it0005: The simplest of pom installation. We have a snapshot pom and we install
        it in local repository.

it0006: Integration test for the verifier plugin.

it0007: We specify a parent in the POM and make sure that it is downloaded as
        part of the process.        

it0008: Simple goal decoration where a plugin binds to a phase and the plugin must
        be downloaded from a remote repository before it can be executed. This
        test also checks to make sure that mojo parameters are aligned to the 
        project basedir when their type is "java.io.File".
        
it0009: Test plugin configuration and goal configuration that overrides what the
        mojo has specified.
        
it0010: Since the artifact resolution does not use the project builder, we must
        ensure that the full hierarchy of all dependencies is resolved. This
        includes the dependencies of the parent-pom's of dependencies. This test
        will check this, by depending on classworlds, which is a dependency of
        maven-component, which is the parent of maven-plugin, which is an
        explicit dependency of this test.
        # TODO: must correct the assumptions of this test
        
it0011: Test specification of dependency versions via <dependencyManagement/>.

it0012: Test simple POM interpolation

it0013: Test plugin-plugin, which tests maven-plugin-tools-api and 
        maven-plugin-tools-java. This will generate a plugin descriptor from 
        java-based mojo sources, install the plugin, and then use it.

it0014: Test POM configuration by settings the -source and -target for the
        compiler to 1.4

it0016: Test a WAR generation

it0017: Test an EJB generation

it0018: Ensure that managed dependencies for dependency POMs are calculated
        correctly when resolved. Removes commons-logging-1.0.3 and checks it is
        redownloaded.

it0019: Test that a version is managed by pluginManagement in the super POM

it0020: Test beanshell mojo support.

it0021: Test pom-level profile inclusion (this one is activated by system
        property).

it0022: Test profile inclusion from profiles.xml (this one is activated by system
        property).

it0023: Test profile inclusion from settings.xml (this one is activated by an id
        in the activeProfiles section).

it0024: Test usage of <executions/> inside a plugin rather than <goals/>
        that are directly inside th plugin.

it0025: Test multiple goal executions with different execution-level configs.

it0026: Test merging of global- and user-level settings.xml files.

it0027: Test @execute with a custom lifecycle, including configuration

it0028: Test that unused configuration parameters from the POM don't cause the
        mojo to fail...they will show up as warnings in the -X output instead.

it0029: Test for pluginManagement injection of plugin configuration.

it0030: Test for injection of dependencyManagement through parents of 
        dependency poms.

it0031: Test usage of plugins.xml mapping file on the repository to resolve
        plugin artifactId from it's prefix using the pluginGroups in
        the provided settings.xml.

it0032: Tests that a specified Maven version requirement that is lower doesn't cause any problems

it0033: Test an EAR generation

it0034: Test version range junit [3.7,) resolves to 3.8.1

it0035: Test artifact relocation.

it0036: Test building from release-pom.xml when it's available

it0037: Test building with alternate pom file using '-f'

it0038: Test building project from outside the project directory using '-f'
        option

it0039: Test reactor for projects that have release-pom.xml in addition to
        pom.xml. The release-pom.xml file should be chosen above pom.xml for
        these projects in the build.

it0040: Test the use of a packaging from a plugin

it0041: Test the use of a new type from a plugin

it0042: Test that the reactor can establish the artifact location of known projects for dependencies

it0043: Test for repository inheritence - ensure using the same id overrides the defaults

it0044: Test --settings CLI option

it0045: Test non-reactor behavior when plugin declares "@requiresProject false"

it0046: Test fail-never reactor behavior. Forces an exception to be thrown in
        the first module, but checks that the second modules is built.

it0047: Test the use case for having a compile time dependency be transitive: 
        when you extend a class you need its dependencies at compile time.

it0048: Verify that default values for mojo parameters are working (indirectly, 
        by verifying that the Surefire mojo is functioning correctly).

it0049: Test parameter alias usage.

it0050: Test surefire inclusion/exclusions

it0051: Test source attachment when -DperformRelease=true is specified.

it0052: Test that source attachment doesn't take place when
        -DperformRelease=true is missing.

it0053: Test that attached artifacts have the same buildnumber and timestamp
        as the main artifact. This will not correctly verify until we have
        some way to pattern-match the buildnumber/timestamp...

it0054: Test resource filtering.

it0055: Test that source includes/excludes with in the compiler plugin config.
        This will test excludes and testExcludes...

it0056: Test that multiple executions of the compile goal with different
        includes/excludes will succeed.

it0057: Verify that scope == 'provided' dependencies are available to tests.

it0058: Verify that profiles from settings.xml do not pollute module lists
        across projects in a reactorized build.

it0059: Verify that maven-1 POMs will be ignored but not stop the resolution
        process.

it0060: Test aggregation of list configuration items when using
        'combine.children=append' attribute. Specifically, merge the list of
        excludes for the testCompile mojo.

it0061: Verify that deployment of artifacts to a legacy-layout repository
        results in a groupId directory of 'the.full.group.id' instead of
        'the/full/group/id'.

it0062: Test that a deployment of a snapshot falls back to a non-snapshot repository if no snapshot repository is
        specified.

it0063: Test the use of a system scoped dependency to tools.jar.

it0064: Test the use of a mojo that uses setters instead of private fields
        for the population of configuration values.

it0065: Test that the basedir of the parent is set correctly.

it0066: Test that nonstandard POM files will be installed correctly.

it0067: Test activation of a profile from the command line.

it0068: Test repository accumulation.

it0069: Test offline mode.

it0070: Test a RAR generation.

it0071: Verifies that dotted property references work within plugin
        configurations.

it0072: Verifies that property references with dotted notation work within
        POM interpolation.

it0073: Tests context passing between mojos in the same plugin.

it0074: Test that plugin-level configuration instances are not nullified by
        execution-level configuration instances.

it0075: Verify that direct invocation of a mojo from the command line still
        results in the processing of modules included via profiles.

it0076: Test that plugins in pluginManagement aren't included in the build
        unless they are referenced by groupId/artifactId within the plugins
        section of a pom.

it0077: Test test jar attachment.

it0078: Test that configuration for maven-compiler-plugin is injected from
        PluginManagement section even when it's not explicitly defined in the
        plugins section.

it0079: Test that source attachments have the same build number as the main
        artifact when deployed.

it0080: Test that depending on a WAR doesn't also get its dependencies
        transitively.

it0081: Test per-plugin dependencies.

it0082: Test that the reactor can establish the artifact location of known projects for dependencies
        using process-sources to see that it works even when they aren't compiled

it0083: Verify that overriding a compile time dependency as provided in a WAR ensures it is not included.

it0084: Verify that the collector selecting a particular version gets the correct subtree

it0085: Verify that system-scoped dependencies get resolved with system scope
        when they are resolved transitively via another (non-system)
        dependency. Inherited scope should not apply in the case of
        system-scoped dependencies, no matter where they are.

it0086: Verify that a plugin dependency class can be loaded from both the plugin classloader and the
        context classloader available to the plugin.

it0087: Verify that a project-level plugin dependency class can be loaded from both the plugin classloader
        and the context classloader available to the plugin.

it0088: Test path translation.

it0089: Test that Checkstyle PackageNamesLoader.loadModuleFactory(..) method will complete as-is with
        the context classloader available to the plugin.
       
it0090: Test that ensures that envars are interpolated correctly into plugin
        configurations.

it0091: Test that currently demonstrates that properties are not correctly
        interpolated into other areas in the POM. This may strictly be a boolean
        problem: I captured the problem as it was reported.

it0092: Test that legacy repositories with legacy snapshots download correctly.

it0094: Test classloading issues with mojos after 2.0 (MNG-1898).

it0095: Test URL calculation when modules are in sibling dirs of parent. (MNG-2006)

it0096: Test that plugin executions from >1 step of inheritance don't run multiple times. 

it0097: Test that the implied relative path for the parent POM works, even two
        levels deep.

it0098: Test that quoted system properties are processed correctly. [MNG-1415]

it0099: Test that parent-POMs cached during a build are available as parents
        to other POMs in the multimodule build. [MNG-2130]

it0100: Test that ${parent.artifactId} resolves correctly. [MNG-2124]

it0101: Test that properties defined in an active profile in the user's
        settings are available for interpolation of systemPath in a dependency.
        [MNG-2052]

it0102: Test that <activeByDefault/> calculations for profile activation only
        use profiles defined in the POM. [MNG-2136]

it0103: Verify that multimodule builds where one project references another as
        a parent can build, even if that parent is not correctly referenced by
        <relativePath/> and is not in the local repository. [MNG-2196]

it0104: Verify that plugin configurations are resolved correctly, particularly
        when they contain ${project.build.directory} in the string value of a 
        Map.Entry.

it0105: MRESOURCES-18

it0106: When a project has modules and its parent is not preinstalled [MNG-2318]

it0107: Verify that default implementation of an implementation for a complex object works as 
        expected [MNG-2293]
END

@descriptions = $readme =~ m/(it\d+\: .*?)(?=\nit\d+\:|$)/gsx;
for $desc (@descriptions) {
	($name, $value) = ($desc =~ m/^(it\d+)\: (.*)$/s);
	# ($value) = ($result =~ m/^it\d+\: (.*)$/s);
	# $value =~ s/\s+/ /g;
	chomp ($value);
	$comment{$name} = $value;
}

opendir(DIR, $dirname) or die "can't opendir $dirname: $!";
while (defined($filename = readdir(DIR))) {
    next unless (-d "$dirname/$filename");
    next if ($filename eq ".svn");
    next unless ($filename =~ m/^it0\d+$/);
    $filePrebuildHook = "$dirname/$filename/prebuild-hook.txt";
    $fileCliOptions = "$dirname/$filename/cli-options.txt";
    $fileSystemProperties = "$dirname/$filename/system.properties";
    $fileVerifierProperties = "$dirname/$filename/verifier.properties";
    $fileGoals = "$dirname/$filename/goals.txt";
    $fileExpectedResults = "$dirname/$filename/expected-results.txt";
    $failOnErrorOutput = 1;
    if (!exists($comment{$filename})) {
    	die "no comment: $filename\n";
    }
    print "/** $comment{$filename} */\n";
    print "public void test_$filename() throws Exception {\n";
    print "File basedir = new File(rootdir, \"$filename\");\n";
    print "verifier = new Verifier(basedir.getAbsolutePath());\n";
    
    if (-e "$filePrebuildHook") {
    	open (FILE, "$filePrebuildHook");
	    while ($line = <FILE>) {
	    	# print ("OLDLINE: $line");
	    	if ($line =~ /^(rm|rmdir) (.*)/) {
	    		($cmd, $path) = ($1, $2);
	    		
	    		if ($cmd eq "rm") {
	    			if ($path =~ m/^\$\{artifact:([^:]*?):([^:]*?):([^:]*?):([^:]*?)\}$/) {
	    				print "verifier.deleteArtifact(\"$1\", \"$2\", \"$3\", \"$4\");\n";
	    			} else {
	    				print "FileUtils.deleteFile(new File(basedir, \"$path\"));\n";
	    			}
	    		} elsif ($cmd eq "rmdir") {
	    			print "FileUtils.deleteDirectory(new File(basedir, \"$path\"));\n";
	    		} else {
	    			die ("wtf? $line\n");
	    		}
			}
			else {
				die ("unexpected command: $line\n");
			}
	    }
	    close FILE;
	}
    
    if (-e "$fileCliOptions") {
    	open(FILE, $fileCliOptions);
    	$cliOptions = <FILE>;
    	chomp ($cliOptions);
    	$cliOptions =~ s/"/\\"/g;
    	print "List cliOptions = new ArrayList();\n";
    	print "cliOptions.add(\"$cliOptions\");\n";
    	print "verifier.setCliOptions(cliOptions);\n";
    	close FILE;
    }
    if (-e "$fileSystemProperties") {
    	open(FILE, $fileSystemProperties);
    	print "Properties systemProperties = new Properties();\n";
    	while ($line = <FILE>) {
    		next if ($line =~ m/^\s*\#/);
    		($name, $value) = ($line =~ m/^([^=]*)=(.*)/);
    		print "systemProperties.put(\"$name\", \"$value\");\n";
    	}
    	print "verifier.setSystemProperties(systemProperties);\n";
    	close FILE;
    }
    if (-e "$fileVerifierProperties") {
    	open(FILE, $fileVerifierProperties);
    	print "Properties verifierProperties = new Properties();\n";
    	while ($line = <FILE>) {
    		next if ($line =~ m/^\s*\#/);
    		($name, $value) = ($line =~ m/^([^=]*)=(.*)/);
    		if ($name eq "failOnErrorOutput" and $value eq "false") {
    			$failOnErrorOutput = 0;
    		}
    		print "verifierProperties.put(\"$name\", \"$value\");\n";
    	}
    	print "verifier.setVerifierProperties(verifierProperties);\n";
    	close FILE;
    }
    
    open (FILE, $fileGoals) or die "Couldn't open $fileGoals: $!\n";
    
    @goals = ();
    while ($line = <FILE>) {
    	next if ($line =~ m/^\s*$/);
    	chomp ($line);
    	push (@goals, $line);
    }
    if (scalar(@goals) == 1) {
    	print "verifier.executeGoal(\"$goals[0]\");\n";
    } else {
	    print "List goals = Arrays.asList(new String[] {";
	    for ($i = 0; $i < @goals; $i++) {
	    	print "\"$goals[$i]\"";
	    	print ", " if ($i != scalar(@goals) -1);
		}
	    print "});\n";
	    print "verifier.executeGoals(goals);\n";
	}
	
	close FILE;
	
	if (-e $fileExpectedResults) {
	
		open (FILE, $fileExpectedResults) or die "Couldn't open $fileExpectedResults: $!\n";
		
		while ($line = <FILE>) {
	    	chomp ($line);
	    	#print ("OLDLINE: $line\n");
	    	if ($line =~ /^\#(.*)/) {
	    		print "//$1\n";
	    		next;
	    	}
	    	if ($line =~ m/^\!\$\{artifact:([^:]*?):([^:]*?):([^:]*?):([^:]*?)\}$/) {
	    		print "verifier.assertArtifactNotPresent(\"$1\", \"$2\", \"$3\", \"$4\");\n";
	    	} elsif ($line =~ m/^\$\{artifact:([^:]*?):([^:]*?):([^:]*?):([^:]*?)\}$/) {
	    		print "verifier.assertArtifactPresent(\"$1\", \"$2\", \"$3\", \"$4\");\n";
	    	} elsif ($line =~ m/^\!(.*)/) {
	    		print "verifier.assertFileNotPresent(\"$1\");\n";
	    	} else {
	    		print "verifier.assertFilePresent(\"$line\");\n";
	    	}
	    }
	    close FILE;
	}
	
	if ($failOnErrorOutput) {
		print "verifier.verifyErrorFreeLog();\n";
	} else {
		print "// don't verify error free log\n";
	}
	print "}\n\n";
	
}
closedir(DIR);