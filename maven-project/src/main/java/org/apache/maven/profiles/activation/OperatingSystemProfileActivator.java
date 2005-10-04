package org.apache.maven.profiles.activation;

import org.apache.maven.model.Activation;
import org.apache.maven.model.ActivationOS;
import org.apache.maven.model.Profile;
import org.codehaus.plexus.util.Os;

public class OperatingSystemProfileActivator
    implements ProfileActivator
{

    public boolean canDetermineActivation( Profile profile )
    {
        Activation activation = profile.getActivation();
        return activation != null && activation.getOs() != null;
    }

    public boolean isActive( Profile profile )
    {
        Activation activation = profile.getActivation();
        ActivationOS os = activation.getOs();
        
        boolean hasNonNull = ensureAtLeastOneNonNull( os );
        
        boolean isFamily = determineFamilyMatch( os.getFamily() );
        boolean isName = determineNameMatch( os.getName() );
        boolean isArch = determineArchMatch( os.getArch() );
        boolean isVersion = determineVersionMatch( os.getVersion() );
        
        return hasNonNull && isFamily && isName && isArch && isVersion;
    }
    
    private boolean ensureAtLeastOneNonNull( ActivationOS os )
    {
        return os.getArch() != null || os.getFamily() != null || os.getName() != null || os.getVersion() != null;
    }

    private boolean determineVersionMatch( String version )
    {
        String test = version;
        boolean reverse = false;
        
        if ( test.startsWith( "!" ) )
        {
            reverse = true;
            test = test.substring( 1 );
        }
        
        boolean result = Os.isVersion( test );
        
        if ( reverse )
        {
            return !result;
        }
        else
        {
            return result;
        }
    }

    private boolean determineArchMatch( String arch )
    {
        String test = arch;
        boolean reverse = false;
        
        if ( test.startsWith( "!" ) )
        {
            reverse = true;
            test = test.substring( 1 );
        }
        
        boolean result = Os.isArch( test );
        
        if ( reverse )
        {
            return !result;
        }
        else
        {
            return result;
        }
    }

    private boolean determineNameMatch( String name )
    {
        String test = name;
        boolean reverse = false;
        
        if ( test.startsWith( "!" ) )
        {
            reverse = true;
            test = test.substring( 1 );
        }
        
        boolean result = Os.isName( test );
        
        if ( reverse )
        {
            return !result;
        }
        else
        {
            return result;
        }
    }

    private boolean determineFamilyMatch( String family )
    {
        String test = family;
        boolean reverse = false;
        
        if ( test.startsWith( "!" ) )
        {
            reverse = true;
            test = test.substring( 1 );
        }
        
        boolean result = Os.isFamily( test );
        
        if ( reverse )
        {
            return !result;
        }
        else
        {
            return result;
        }
    }

}
