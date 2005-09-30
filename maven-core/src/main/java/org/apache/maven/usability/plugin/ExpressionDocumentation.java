package org.apache.maven.usability.plugin;

public final class ExpressionDocumentation
{
    private String origin;

    private String usage;

    private String bannedMessage;

    private String deprecationMessage;

    private String expression;

    private String addendum;

    ExpressionDocumentation()
    {
    }

    void setExpression( String expression )
    {
        this.expression = expression;
    }

    public String getExpression()
    {
        return expression;
    }

    void setOrigin( String origin )
    {
        this.origin = origin;
    }

    public String getOrigin()
    {
        return origin;
    }

    void setUsage( String usage )
    {
        this.usage = usage;
    }

    public String getUsage()
    {
        return usage;
    }

    void setBanMessage( String bannedMessage )
    {
        this.bannedMessage = bannedMessage;
    }

    public String getBanMessage()
    {
        return bannedMessage;
    }

    void setDeprecationMessage( String deprecationMessage )
    {
        this.deprecationMessage = deprecationMessage;
    }

    public String getDeprecationMessage()
    {
        return deprecationMessage;
    }

    void setAddendum( String addendum )
    {
        this.addendum = addendum;
    }
    
    public String getAddendum()
    {
        return addendum;
    }
}
