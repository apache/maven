import java.util.Locale;

/**
 * Condition that tests the OS type.
 *
 * @author Stefan Bodewig
 * @author Magesh Umasankar
 * @since Ant 1.4
 * @version $Revision$
 */
public class Os
{
    private static final String OS_NAME = System.getProperty( "os.name" ).toLowerCase( Locale.US );

    private static final String OS_ARCH = System.getProperty( "os.arch" ).toLowerCase( Locale.US );

    private static final String OS_VERSION = System.getProperty( "os.version" ).toLowerCase( Locale.US );

    private static final String PATH_SEP = System.getProperty( "path.separator" );

    private String family;
    private String name;
    private String version;
    private String arch;

    /**
     * Default constructor
     *
     */
    public Os()
    {
    }

    /**
     * Constructor that sets the family attribute
     *
     * @param family a String value
     */
    public Os( String family )
    {
        setFamily( family );
    }

    /**
     * Sets the desired OS family type
     *
     * @param f      The OS family type desired<br />
     *               Possible values:<br />
     *               <ul>
     *               <li>dos</li>
     *               <li>mac</li>
     *               <li>netware</li>
     *               <li>os/2</li>
     *               <li>tandem</li>
     *               <li>unix</li>
     *               <li>windows</li>
     *               <li>win9x</li>
     *               <li>z/os</li>
     *               <li>os/400</li>
     *               </ul>
     */
    public void setFamily( String f )
    {
        family = f.toLowerCase( Locale.US );
    }

    /**
     * Sets the desired OS name
     *
     * @param name   The OS name
     */
    public void setName( String name )
    {
        this.name = name.toLowerCase( Locale.US );
    }

    /**
     * Sets the desired OS architecture
     *
     * @param arch   The OS architecture
     */
    public void setArch( String arch )
    {
        this.arch = arch.toLowerCase( Locale.US );
    }

    /**
     * Sets the desired OS version
     *
     * @param version   The OS version
     */
    public void setVersion( String version )
    {
        this.version = version.toLowerCase( Locale.US );
    }

    /**
     * Determines if the OS on which Ant is executing matches the type of
     * that set in setFamily.
     * @see Os#setFamily(String)
     */
    public boolean eval() throws Exception
    {
        return isOs( family, name, arch, version );
    }

    /**
     * Determines if the OS on which Ant is executing matches the
     * given OS family.
     * @param family the family to check for
     * @return true if the OS matches
     * @since 1.5
     */
    public static boolean isFamily( String family )
    {
        return isOs( family, null, null, null );
    }

    /**
     * Determines if the OS on which Ant is executing matches the
     * given OS name.
     *
     * @param name the OS name to check for
     * @return true if the OS matches
     * @since 1.7
     */
    public static boolean isName( String name )
    {
        return isOs( null, name, null, null );
    }

    /**
     * Determines if the OS on which Ant is executing matches the
     * given OS architecture.
     *
     * @param arch the OS architecture to check for
     * @return true if the OS matches
     * @since 1.7
     */
    public static boolean isArch( String arch )
    {
        return isOs( null, null, arch, null );
    }

    /**
     * Determines if the OS on which Ant is executing matches the
     * given OS version.
     *
     * @param version the OS version to check for
     * @return true if the OS matches
     * @since 1.7
     */
    public static boolean isVersion( String version )
    {
        return isOs( null, null, null, version );
    }

    /**
     * Determines if the OS on which Ant is executing matches the
     * given OS family, name, architecture and version
     *
     * @param family   The OS family
     * @param name   The OS name
     * @param arch   The OS architecture
     * @param version   The OS version
     * @return true if the OS matches
     * @since 1.7
     */
    public static boolean isOs( String family, String name, String arch,
                                String version )
    {
        boolean retValue = false;

        if ( family != null || name != null || arch != null
            || version != null )
        {

            boolean isFamily = true;
            boolean isName = true;
            boolean isArch = true;
            boolean isVersion = true;

            if ( family != null )
            {
                if ( family.equals( "windows" ) )
                {
                    isFamily = OS_NAME.indexOf( "windows" ) > -1;
                }
                else if ( family.equals( "os/2" ) )
                {
                    isFamily = OS_NAME.indexOf( "os/2" ) > -1;
                }
                else if ( family.equals( "netware" ) )
                {
                    isFamily = OS_NAME.indexOf( "netware" ) > -1;
                }
                else if ( family.equals( "dos" ) )
                {
                    isFamily = PATH_SEP.equals( ";" ) && !isFamily( "netware" );
                }
                else if ( family.equals( "mac" ) )
                {
                    isFamily = OS_NAME.indexOf( "mac" ) > -1;
                }
                else if ( family.equals( "tandem" ) )
                {
                    isFamily = OS_NAME.indexOf( "nonstop_kernel" ) > -1;
                }
                else if ( family.equals( "unix" ) )
                {
                    isFamily = PATH_SEP.equals( ":" )
                        && !isFamily( "openvms" )
                        && ( !isFamily( "mac" ) || OS_NAME.endsWith( "x" ) );
                }
                else if ( family.equals( "win9x" ) )
                {
                    isFamily = isFamily( "windows" )
                        && ( OS_NAME.indexOf( "95" ) >= 0
                        || OS_NAME.indexOf( "98" ) >= 0
                        || OS_NAME.indexOf( "me" ) >= 0
                        || OS_NAME.indexOf( "ce" ) >= 0 );
                }
                else if ( family.equals( "z/os" ) )
                {
                    isFamily = OS_NAME.indexOf( "z/os" ) > -1
                        || OS_NAME.indexOf( "os/390" ) > -1;
                }
                else if ( family.equals( "os/400" ) )
                {
                    isFamily = OS_NAME.indexOf( "os/400" ) > -1;
                }
                else if ( family.equals( "openvms" ) )
                {
                    isFamily = OS_NAME.indexOf( "openvms" ) > -1;
                }
            }
            if ( name != null )
            {
                isName = name.equals( OS_NAME );
            }
            if ( arch != null )
            {
                isArch = arch.equals( OS_ARCH );
            }
            if ( version != null )
            {
                isVersion = version.equals( OS_VERSION );
            }
            retValue = isFamily && isName && isArch && isVersion;
        }
        return retValue;
    }
}
