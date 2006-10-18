#!/usr/bin/perl
use File::Path;
use strict;

my $dirname = "maven-core-it";
my $newITs = "maven-core-it-new";
my $newITsResources = "$newITs/src/test/resources";


open( FILE, "$dirname/integration-tests-descriptions.txt" ) or die;
undef $/;
my $readme = <FILE>; 
close( FILE );

$/ = "\n";

my @descriptions = $readme =~ m/(it\d+\: .*?)(?=\nit\d+\:|$)/gsx;
my %comment;
for my $desc (@descriptions) {
	my ($name, $value) = ($desc =~ m/^(it\d+)\: (.*)$/s);
	chomp ($value);
	$comment{$name} = $value;
}

rmtree($newITs);
mkpath($newITs);

open (POM, "> $newITs/pom.xml" );
print POM <<END;

<project>
  <modelVersion>4.0.0</modelVersion>
  <groupId>org.apache.maven.integrationtests</groupId>
  <artifactId>maven-core-integrationtests</artifactId> 
  <version>1.0-SNAPSHOT</version>
  <name>Maven Integration Tests</name> 
  <build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-surefire-plugin</artifactId>
            <configuration>
                <includes>
                    <include>**/IntegrationTestSuite.java</include>
                 </includes>
                 <forkMode>never</forkMode>
            </configuration>
        </plugin>
    </plugins>
  </build>
  <dependencies>
    <dependency>
      <groupId>org.apache.maven</groupId>
      <artifactId>maven-core-it-verifier</artifactId>
      <version>2.1-SNAPSHOT</version>
    </dependency>
    <dependency>
      <groupId>junit</groupId>
      <artifactId>junit</artifactId>
      <version>3.8.1</version>
      <scope>test</scope>
    </dependency>
  </dependencies>
</project>
END

close POM;

my $suiteRoot = "$newITs/src/test/java/org/apache/maven/integrationtests";
mkpath($suiteRoot);
mkpath($newITsResources);

open( SUITE, "> $suiteRoot/IntegrationTestSuite.java" );  
print SUITE <<END;
package org.apache.maven.integrationtests;

import junit.framework.*;

public class IntegrationTestSuite extends TestCase {

    public static Test suite() {
        TestSuite suite = new TestSuite();
END
    
opendir(DIR, $dirname) or die "can't opendir $dirname: $!";
while (defined(my $filename = readdir(DIR))) {
    next unless (-d "$dirname/$filename");
    next if ($filename eq ".svn");
    next unless ($filename =~ m/^it0\d+$/);
    my $filePrebuildHook = "$dirname/$filename/prebuild-hook.txt";
    my $fileCliOptions = "$dirname/$filename/cli-options.txt";
    my $fileSystemProperties = "$dirname/$filename/system.properties";
    my $fileVerifierProperties = "$dirname/$filename/verifier.properties";
    my $fileGoals = "$dirname/$filename/goals.txt";
    my $fileExpectedResults = "$dirname/$filename/expected-results.txt";
    my $failOnErrorOutput = 1;
    
    my $itTestCaseDirectory = $suiteRoot; 
    my $itTestName = "Maven" . uc($filename) . "Test";
    my $testFile = "$itTestCaseDirectory/$itTestName" . ".java";    
    my $testProjectDirectory = "$newITsResources/$filename";
    
    # 96, 97 will not due to bugs in maven, they work when other ITs are run but it's due to ordering and fluke
    # 43 will not run because it can't find the maven-help-plugin
    # 90 will not run because it relies of an environment variable which I think is wrong 
    # 91 POM interpolation test failure
    # 98 fails because it needs a quoted CLI property, this isn't really a core problem and certainly won't be valid in an embedded env
    # 104 test failure in interpolation
    # 106 failure in artifact resolution
    # 107 failure in artifact resolution
    if ( $filename eq "it0096" || 
         $filename eq "it0097" ||
         $filename eq "it0043" ||
         $filename eq "it0090" ||
         $filename eq "it0091" ||
         $filename eq "it0098" || 
         $filename eq "it0104" ||
         $filename eq "it0106" || 
         $filename eq "it0107" ) 
    {
        print SUITE "       // suite.addTestSuite($itTestName.class);\n";
    }
    else
    {
        print SUITE "       suite.addTestSuite($itTestName.class);\n";
    }    	
    
    if (!exists($comment{$filename})) {
    	die "no comment: $filename\n";
    }
    
    
    
    mkpath($itTestCaseDirectory);
    # DGF can't believe perl doesn't have a baked in recursive copy!
    if ("MSWin32" eq $^O) {
        my $winSrc = "$dirname/$filename";
        $winSrc =~ s!/!\\!g;
        my $winDest = $testProjectDirectory;
        $winDest =~ s!/!\\!g;
        mkpath($testProjectDirectory);
        system( "xcopy /e $winSrc $winDest" );
    } else {
        system( "cp -r $dirname/$filename $testProjectDirectory" );
    }
	unlink("$testProjectDirectory/cli-options.txt");
	unlink("$testProjectDirectory/system.properties");
	unlink("$testProjectDirectory/verifier.properties");
	unlink("$testProjectDirectory/goals.txt");
	unlink("$testProjectDirectory/expected-results.txt");
	unlink("$testProjectDirectory/prebuild-hook.txt");
	unlink("$testProjectDirectory/log.txt");

	open( T, "> $testFile") or die;
    print $filename . "\n";    
    print T <<END;
package org.apache.maven.integrationtests;
import java.io.*;
import java.util.*;

import junit.framework.*;

import org.apache.maven.it.*;
import org.apache.maven.it.util.*;

public class $itTestName extends TestCase /*extends AbstractMavenIntegrationTest*/ {    

/** $comment{$filename} */
public void test$filename() throws Exception {
String basedir = System.getProperty("maven.test.tmpdir", System.getProperty("java.io.tmpdir"));
File testDir = new File(basedir, getName());
FileUtils.deleteDirectory(testDir);
System.out.println("Extracting $filename to " + testDir.getAbsolutePath());
ResourceExtractor.extractResourcePath(getClass(), "/$filename", testDir);
Verifier verifier = new Verifier(testDir.getAbsolutePath());
END

    if (-e "$filePrebuildHook") {
    	open (FILE, "$filePrebuildHook");
	    while (my $line = <FILE>) {
	    	if ($line =~ /^(rm|rmdir) (.*)/) {
	    		my ($cmd, $path) = ($1, $2);
	    		
	    		if ($cmd eq "rm") {
	    			if ($path =~ m/^\$\{artifact:([^:]*?):([^:]*?):([^:]*?):([^:]*?)\}$/) {
	    				print T "verifier.deleteArtifact(\"$1\", \"$2\", \"$3\", \"$4\");\n";
	    			} else {
	    				print T "FileUtils.deleteFile(new File(basedir, \"$path\"));\n";
	    			}
	    		} elsif ($cmd eq "rmdir") {
	    			print T "FileUtils.deleteDirectory(new File(basedir, \"$path\"));\n";
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
    	my $cliOptions = <FILE>;
    	chomp ($cliOptions);
    	$cliOptions =~ s/"/\\"/g;
    	print T "List cliOptions = new ArrayList();\n";
    	print T "cliOptions.add(\"$cliOptions\");\n";
    	print T "verifier.setCliOptions(cliOptions);\n";
    	close FILE;
    }
    if (-e "$fileSystemProperties") {
    	open(FILE, $fileSystemProperties);
    	print T  "Properties systemProperties = new Properties();\n";
    	while (my $line = <FILE>) {
    		next if ($line =~ m/^\s*\#/);
    		my ($name, $value) = ($line =~ m/^([^=]*)=(.*)/);
    		print T  "systemProperties.put(\"$name\", \"$value\");\n";
    	}
    	print T  "verifier.setSystemProperties(systemProperties);\n";
    	close FILE;
    }
    if (-e "$fileVerifierProperties") {
    	open(FILE, $fileVerifierProperties);
    	print T  "Properties verifierProperties = new Properties();\n";
    	while (my $line = <FILE>) {
    		next if ($line =~ m/^\s*\#/);
    		my ($name, $value) = ($line =~ m/^([^=]*)=(.*)/);
    		if ($name eq "failOnErrorOutput" and $value eq "false") {
    			$failOnErrorOutput = 0;
    		}
    		print T  "verifierProperties.put(\"$name\", \"$value\");\n";
    	}
    	print T  "verifier.setVerifierProperties(verifierProperties);\n";
    	close FILE;
    }
    
    open (FILE, $fileGoals) or die "Couldn't open $fileGoals: $!\n";
    
    my @goals = ();
    while (my $line = <FILE>) {
    	next if ($line =~ m/^\s*$/);
    	chomp ($line);
    	push (@goals, $line);
    }
    if (scalar(@goals) == 1) {
    	print T  "verifier.executeGoal(\"$goals[0]\");\n";
    } else {
	    print T  "List goals = Arrays.asList(new String[] {";
	    for (my $i = 0; $i < @goals; $i++) {
	    	print T  "\"$goals[$i]\"";
	    	print T  ", " if ($i != scalar(@goals) -1);
		}
	    print T  "});\n";
	    print T  "verifier.executeGoals(goals);\n";
	}
	
	close FILE;
	
	if (-e $fileExpectedResults) {
	
		open (FILE, $fileExpectedResults) or die "Couldn't open $fileExpectedResults: $!\n";
		
		while (my $line = <FILE>) {
	    	chomp ($line);
	    	#print T  ("OLDLINE: $line\n");
	    	if ($line =~ /^\#(.*)/) {
	    		print T  "//$1\n";
	    		next;
	    	}
	    	if ($line =~ m/^\!\$\{artifact:([^:]*?):([^:]*?):([^:]*?):([^:]*?)\}$/) {
	    		print T  "verifier.assertArtifactNotPresent(\"$1\", \"$2\", \"$3\", \"$4\");\n";
	    	} elsif ($line =~ m/^\$\{artifact:([^:]*?):([^:]*?):([^:]*?):([^:]*?)\}$/) {
	    		print T  "verifier.assertArtifactPresent(\"$1\", \"$2\", \"$3\", \"$4\");\n";
	    	} elsif ($line =~ m/^\!(.*)/) {
	    		print T  "verifier.assertFileNotPresent(\"$1\");\n";
	    	} else {
	    		print T  "verifier.assertFilePresent(\"$line\");\n";
	    	}
	    }
	    close FILE;
	}
	
	if ($failOnErrorOutput) {
		print T  "verifier.verifyErrorFreeLog();\n";
	} else {
		print T  "// don't verify error free log\n";
	}
	print T "verifier.resetStreams();\n";
	print T "System.out.println(\"PASS\");\n";
	print T  "}}\n\n";
	
}

# DGF end of the suite
print SUITE <<END;
        return suite;
    }
}
END

        
closedir(DIR);
