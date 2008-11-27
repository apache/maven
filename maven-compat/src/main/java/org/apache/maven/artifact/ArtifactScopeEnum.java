package org.apache.maven.artifact;

/**
 * Type safe reincarnation of Artifact scope. Also supplies the <code>DEFAULT_SCOPE<code> as well
 * as convenience method to deal with scope relationships.
 * 
 * @author <a href="oleg@codehaus.org">Oleg Gusakov</a>
 *
 */

public enum ArtifactScopeEnum
{
    compile( 1 ), test( 2 ), runtime( 3 ), provided( 4 ), system( 5 );

    public static final ArtifactScopeEnum DEFAULT_SCOPE = compile;

    private int id;

    // Constructor
    ArtifactScopeEnum( int id )
    {
        this.id = id;
    }

    int getId()
    {
        return id;
    }


    /**
     * Helper method to simplify null processing
     * 
     * @return 
     */
    public static final ArtifactScopeEnum checkScope( ArtifactScopeEnum scope )
    {
    	return scope == null ? DEFAULT_SCOPE : scope;
    }
    
    /**
     * 
     * @return unsafe String representation of this scope.
     */
    public String getScope()
    {
        if ( id == 1 )
        {
            return Artifact.SCOPE_COMPILE;
        }
        else if ( id == 2 )
        {
            return Artifact.SCOPE_TEST;

        }
        else if ( id == 3 )
        {
            return Artifact.SCOPE_RUNTIME;

        }
        else if ( id == 4 )
        {
            return Artifact.SCOPE_PROVIDED;
        }
        else
        {
            return Artifact.SCOPE_SYSTEM;
        }
    }
    
    private static final ArtifactScopeEnum [][][] _compliancySets = {
    	  { { compile  }, { compile,                provided, system } }
      	, { { test     }, { compile, test,          provided, system } }
    	, { { runtime  }, { compile,       runtime,           system } }
    	, { { provided }, { compile, test,          provided         } }
    };
    
    /**
     * scope relationship function. Used by the graph conflict resolution policies
     * 
     * @param scope
     * @return true is supplied scope is an inclusive sub-scope of current one.
     */
    public boolean encloses( ArtifactScopeEnum scope )
    {
    	final ArtifactScopeEnum s = checkScope(scope);
    	
    	// system scope is historic only - and simple
    	if( id == system.id )
    		return scope.id == system.id;

    	for( ArtifactScopeEnum[][] set : _compliancySets  )
    	{
    		if( id == set[0][0].id )
    		{
    			for( ArtifactScopeEnum ase : set[1] )
    			{
    				if( s.id == ase.id )
    					return true;
    			}
    			break;
    		}
    	}
    	return false;
    }
}
