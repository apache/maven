/* Created on Aug 7, 2004 */
package org.apache.maven.plugin.loader.marmalade.tags;

import org.apache.maven.plugin.descriptor.Parameter;
import org.codehaus.marmalade.model.AbstractMarmaladeTag;
import org.codehaus.marmalade.model.MarmaladeAttribute;
import org.codehaus.marmalade.model.MarmaladeAttributes;
import org.codehaus.marmalade.runtime.MarmaladeExecutionContext;
import org.codehaus.marmalade.runtime.MarmaladeExecutionException;

public class MojoParameterTag extends AbstractMarmaladeTag
    implements DescriptionOwner
{
    public static final String NAME_ATTRIBUTE = "name";
    public static final String TYPE_ATTRIBUTE = "type";
    public static final String REQUIRED_ATTRIBUTE = "required";
    public static final String VALIDATOR_ATTRIBUTE = "validator";
    public static final String EXPRESSION_ATTRIBUTE = "expression";
    private Parameter param = new Parameter(  );

    protected void doExecute( MarmaladeExecutionContext context )
        throws MarmaladeExecutionException
    {
        MarmaladeAttributes attributes = getAttributes(  );

        param.setName( ( String ) requireTagAttribute( NAME_ATTRIBUTE,
                String.class, context ) );
        param.setRequired( ( ( Boolean ) requireTagAttribute( 
                REQUIRED_ATTRIBUTE, Boolean.class, context ) ).booleanValue(  ) );
        param.setType( ( String ) requireTagAttribute( TYPE_ATTRIBUTE,
                String.class, context ) );
        param.setValidator( ( String ) requireTagAttribute( 
                VALIDATOR_ATTRIBUTE, String.class, context ) );
        param.setExpression( String.valueOf(requireTagAttribute( 
                EXPRESSION_ATTRIBUTE, context ) ) );

        processChildren( context );

        if ( param.getDescription(  ) == null )
        {
            throw new MarmaladeExecutionException( 
                "mojo parameters require a description" );
        }

        MojoParametersTag parent = ( MojoParametersTag ) requireParent( MojoParametersTag.class );

        parent.addParameter( param );
    }

    public void setDescription( String description )
    {
        param.setDescription( description );
    }
}
