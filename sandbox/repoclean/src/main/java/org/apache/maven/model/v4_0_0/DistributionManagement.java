/*
 * $Id$
 */

package org.apache.maven.model.v4_0_0;

//---------------------------------/
//- Imported classes and packages -/
//---------------------------------/


/**
 * Class DistributionManagement.
 * 
 * @version $Revision$ $Date$
 */
public class DistributionManagement
    implements java.io.Serializable
{

    //--------------------------/
    //- Class/Member Variables -/
    //--------------------------/

    /**
     * Field repository
     */
    private Repository repository;

    /**
     * Field site
     */
    private Site site;

    //-----------/
    //- Methods -/
    //-----------/

    /**
     * Method getRepository
     */
    public Repository getRepository()
    {
        return this.repository;
    } //-- Repository getRepository() 

    /**
     * Method getSite
     */
    public Site getSite()
    {
        return this.site;
    } //-- Site getSite() 

    /**
     * Method setRepository
     * 
     * @param repository
     */
    public void setRepository( Repository repository )
    {
        this.repository = repository;
    } //-- void setRepository(Repository) 

    /**
     * Method setSite
     * 
     * @param site
     */
    public void setSite( Site site )
    {
        this.site = site;
    } //-- void setSite(Site) 

}