package org.apache.maven.errors;

import org.apache.maven.ProjectCycleException;
import org.apache.maven.lifecycle.TaskValidationResult;
import org.apache.maven.lifecycle.model.MojoBinding;

import java.util.Arrays;
import java.util.List;

// NOTE: The strange String[] syntax is a backward adaptation from java5 stuff, where
// I was using varargs in listOf(..). I'm not moving them to constants because I'd like
// to go back to this someday...

// TODO: Optimize the String[] instances in here to List constants, and remove listOf(..)
public final class CoreErrorTips
{

    private static final List NO_GOALS_TIPS = Arrays.asList( new String[] {
        "Maven in 5 Minutes guide (http://maven.apache.org/guides/getting-started/maven-in-five-minutes.html)",
        "Maven User's documentation (http://maven.apache.org/users/)",
        "Maven Plugins page (http://maven.apache.org/plugins/)",
        "CodeHaus Mojos Project page (http://mojo.codehaus.org/plugins.html)"
    } );

    private CoreErrorTips()
    {
    }

    public static List getNoGoalsTips()
    {
        return NO_GOALS_TIPS;
    }

    public static List getMojoFailureTips( MojoBinding binding )
    {
        // TODO Auto-generated method stub
        return null;
    }

    public static List getProjectCycleTips( ProjectCycleException error )
    {
        // TODO Auto-generated method stub
        return null;
    }

    public static List getTaskValidationTips( TaskValidationResult result, Exception cause )
    {
        // TODO Auto-generated method stub
        return null;
    }

}
