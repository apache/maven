/*
 * $Id$
 */

package org.apache.maven.model.v3_0_0;

//---------------------------------/
//- Imported classes and packages -/
//---------------------------------/


/**
 * Class License.
 * 
 * @version $Revision$ $Date$
 */
public class License
    implements java.io.Serializable
{

    //--------------------------/
    //- Class/Member Variables -/
    //--------------------------/

    /**
     * Field name
     */
    private String name;

    /**
     * Field url
     */
    private String url;

    /**
     * Field distribution
     */
    private String distribution;

    /**
     * Field comments
     */
    private String comments;

    //-----------/
    //- Methods -/
    //-----------/

    /**
     * Method getComments
     */
    public String getComments()
    {
        return this.comments;
    } //-- String getComments() 

    /**
     * Method getDistribution
     */
    public String getDistribution()
    {
        return this.distribution;
    } //-- String getDistribution() 

    /**
     * Method getName
     */
    public String getName()
    {
        return this.name;
    } //-- String getName() 

    /**
     * Method getUrl
     */
    public String getUrl()
    {
        return this.url;
    } //-- String getUrl() 

    /**
     * Method setComments
     * 
     * @param comments
     */
    public void setComments( String comments )
    {
        this.comments = comments;
    } //-- void setComments(String) 

    /**
     * Method setDistribution
     * 
     * @param distribution
     */
    public void setDistribution( String distribution )
    {
        this.distribution = distribution;
    } //-- void setDistribution(String) 

    /**
     * Method setName
     * 
     * @param name
     */
    public void setName( String name )
    {
        this.name = name;
    } //-- void setName(String) 

    /**
     * Method setUrl
     * 
     * @param url
     */
    public void setUrl( String url )
    {
        this.url = url;
    } //-- void setUrl(String) 

}