#!/usr/bin/perl

$dirname = "maven-core-it";

open( FILE, "maven-core-it/integration-tests-descriptions.txt" ) or die;
undef $/;
$readme = <FILE>; 

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
