package org.apache.maven.usability;

public class SystemWarnings
{

    public static String getOfflineWarning()
    {
        return "\nNOTE: Maven is executing in offline mode. Any artifacts not already in your local\n" +
                "repository will be inaccessible.\n";
    }
    
}
