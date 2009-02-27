package org.apache.maven.profiles;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.model.Activation;
import org.apache.maven.model.Model;
import org.apache.maven.model.Profile;
import org.apache.maven.model.Parent;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.profiles.activation.ProfileActivationContext;
import org.apache.maven.profiles.activation.ProfileActivationException;
import org.apache.maven.shared.model.ModelContainer;
import org.apache.maven.shared.model.ModelProperty;
import org.apache.maven.shared.model.ModelMarshaller;
import org.apache.maven.shared.model.InterpolatorProperty;
import org.apache.maven.project.builder.factories.IdModelContainerFactory;
import org.apache.maven.project.builder.ProjectUri;
import org.apache.maven.project.builder.PomTransformer;
import org.apache.maven.project.builder.PomInterpolatorTag;
import org.apache.maven.project.builder.profile.*;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.MutablePlexusContainer;
import org.codehaus.plexus.util.xml.pull.XmlSerializer;
import org.codehaus.plexus.util.xml.pull.MXSerializer;

import java.util.*;
import java.util.Map.Entry;
import java.io.*;
import java.lang.reflect.Method;


public class DefaultProfileManager
    implements ProfileManager
{
    private MutablePlexusContainer container;

    private Map profilesById = new LinkedHashMap();

    private ProfileActivationContext profileActivationContext;

    /**
     * the properties passed to the profile manager are the props that
     * are passed to maven, possibly containing profile activator properties
     */
    public DefaultProfileManager( PlexusContainer container, ProfileActivationContext profileActivationContext )
    {
        this.container = (MutablePlexusContainer) container;
        if ( profileActivationContext == null )
        {
            this.profileActivationContext = createDefaultActivationContext();
        }
        else
        {
            this.profileActivationContext = profileActivationContext;
        }
    }

    // TODO: Remove this, if possible. It uses system properties, which are not safe for IDE and other embedded environments.
    /**
     * @deprecated Using this is dangerous when extensions or non-global system properties are in play.
     */
    public DefaultProfileManager( PlexusContainer container )
    {
        this.container = (MutablePlexusContainer) container;

        profileActivationContext = createDefaultActivationContext();
    }

    private ProfileActivationContext createDefaultActivationContext()
    {

        return new ProfileActivationContext(System.getProperties(), false );
    }

    public ProfileActivationContext getProfileActivationContext()
    {
        return profileActivationContext;
    }

    public void setProfileActivationContext( ProfileActivationContext profileActivationContext )
    {
        this.profileActivationContext = profileActivationContext;
    }

    public Map getProfilesById()
    {
        return profilesById;
    }

    /* (non-Javadoc)
    * @see org.apache.maven.profiles.ProfileManager#addProfile(org.apache.maven.model.Profile)
    */
    public void addProfile( Profile profile )
    {
        String profileId = profile.getId();

        Profile existing = (Profile) profilesById.get( profileId );
        if ( existing != null )
        {
            container.getLogger().warn( "Overriding profile: \'" + profileId + "\' (source: " + existing.getSource() +
                ") with new instance from source: " + profile.getSource() );
        }

        profilesById.put( profile.getId(), profile );

        Activation activation = profile.getActivation();

        if ( ( activation != null ) && activation.isActiveByDefault() )
        {
            activateAsDefault( profileId );
        }
    }

    /* (non-Javadoc)
    * @see org.apache.maven.profiles.ProfileManager#explicitlyActivate(java.lang.String)
    */
    public void explicitlyActivate( String profileId )
    {
        List activatedIds = profileActivationContext.getExplicitlyActiveProfileIds();
        if ( !activatedIds.contains( profileId ) )
        {
            container.getLogger().debug( "Profile with id: \'" + profileId + "\' has been explicitly activated." );

            profileActivationContext.setActive( profileId );
        }
    }

    /* (non-Javadoc)
    * @see org.apache.maven.profiles.ProfileManager#explicitlyActivate(java.util.List)
    */
    public void explicitlyActivate( List profileIds )
    {
        for ( Iterator it = profileIds.iterator(); it.hasNext(); )
        {
            String profileId = (String) it.next();

            explicitlyActivate( profileId );
        }
    }

    /* (non-Javadoc)
    * @see org.apache.maven.profiles.ProfileManager#explicitlyDeactivate(java.lang.String)
    */
    public void explicitlyDeactivate( String profileId )
    {
        List deactivatedIds = profileActivationContext.getExplicitlyInactiveProfileIds();
        if ( !deactivatedIds.contains( profileId ) )
        {
            container.getLogger().debug( "Profile with id: \'" + profileId + "\' has been explicitly deactivated." );

            profileActivationContext.setInactive( profileId );
        }
    }

    /* (non-Javadoc)
    * @see org.apache.maven.profiles.ProfileManager#explicitlyDeactivate(java.util.List)
    */
    public void explicitlyDeactivate( List profileIds )
    {
        for ( Iterator it = profileIds.iterator(); it.hasNext(); )
        {
            String profileId = (String) it.next();

            explicitlyDeactivate( profileId );
        }
    }

    public List getActiveProfiles()
        throws ProfileActivationException
    {
        return getActiveProfiles( null );
    }

    // TODO: Portions of this logic are duplicated in o.a.m.p.b.p.ProfileContext, something is wrong here
    public List getActiveProfiles( Model model )
        throws ProfileActivationException
    {

        try
        {
            List activeFromPom = new ArrayList();
            List activeExternal = new ArrayList();

            for ( Iterator it = profilesById.entrySet().iterator(); it.hasNext(); )
            {
                Map.Entry entry = (Entry) it.next();

                String profileId = (String) entry.getKey();
                Profile profile = (Profile) entry.getValue();

                boolean shouldAdd = false;
                if ( profileActivationContext.isExplicitlyActive( profileId ) )
                {
                    shouldAdd = true;
                }
                else if ( isActive( profile, profileActivationContext ) )
                {
                    shouldAdd = true;
                }

                if ( !profileActivationContext.isExplicitlyInactive( profileId ) && shouldAdd )
                {
                    if ( "pom".equals( profile.getSource() ) )
                    {
                        activeFromPom.add( profile );
                    }
                    else
                    {
                        activeExternal.add( profile );
                    }
                }
            }

            if ( activeFromPom.isEmpty() )
            {
                List defaultIds = profileActivationContext.getActiveByDefaultProfileIds();

                List deactivatedIds = profileActivationContext.getExplicitlyInactiveProfileIds();

                for ( Iterator it = defaultIds.iterator(); it.hasNext(); )
                {
                    String profileId = (String) it.next();

                    // If this profile was excluded, don't add it back in
                    // Fixes MNG-3545
                    if ( deactivatedIds.contains( profileId ) )
                    {
                        continue;
                    }
                    Profile profile = (Profile) profilesById.get( profileId );

                    if ( profile != null )
                    {
                        activeFromPom.add( profile );
                    }
                }
            }

            List allActive = new ArrayList( activeFromPom.size() + activeExternal.size() );

            allActive.addAll( activeExternal );
            allActive.addAll( activeFromPom );

            return allActive;
        }
        finally
        {
        }
    }

    private static List<ActiveProfileMatcher> matchers = Arrays.asList(new FileMatcher(),
        new JdkMatcher(), new OperatingSystemMatcher(), new PropertyMatcher());

    private boolean isActive( Profile profile, ProfileActivationContext context )
        throws ProfileActivationException
    {
        //TODO: Using reflection now. Need to replace with custom mapper
        StringWriter writer = new StringWriter();
        XmlSerializer serializer = new MXSerializer();
        serializer.setProperty( "http://xmlpull.org/v1/doc/properties.html#serializer-indentation", "  " );
        serializer.setProperty( "http://xmlpull.org/v1/doc/properties.html#serializer-line-separator", "\n" );
        try
        {
            serializer.setOutput( writer );
            serializer.startDocument("UTF-8", null );
        } catch (IOException e) {
            
        }

        try {
            MavenXpp3Writer w = new MavenXpp3Writer();
            Class c = Class.forName("org.apache.maven.model.io.xpp3.MavenXpp3Writer");

            Class partypes[] = new Class[3];
            partypes[0] = Profile.class;
            partypes[1] = String.class;
            partypes[2] = XmlSerializer.class;

            Method meth = c.getDeclaredMethod(
                         "writeProfile", partypes);
            meth.setAccessible(true);
            
            Object arglist[] = new Object[3];
            arglist[0] = profile;
            arglist[1] = "profile";
            arglist[2] = serializer;

            meth.invoke(w, arglist);
            serializer.endDocument();
        } catch (Exception e)
        {
            throw new ProfileActivationException(e.getMessage(), e);
        }

        List<InterpolatorProperty> interpolatorProperties = new ArrayList<InterpolatorProperty>();
        interpolatorProperties.addAll(InterpolatorProperty.toInterpolatorProperties(
                context.getExecutionProperties(),
                PomInterpolatorTag.EXECUTION_PROPERTIES.name()));

        List<ModelProperty> p;
        try
        {                                                                   
            p = ModelMarshaller.marshallXmlToModelProperties(new ByteArrayInputStream(writer.getBuffer().toString().getBytes()),
                    ProjectUri.Profiles.Profile.xUri, PomTransformer.URIS);
        } catch (IOException e) {
            throw new ProfileActivationException(e.getMessage());
        }
        //Serializer adds in extra node, strip it out
        List<ModelProperty> p2 = new ArrayList<ModelProperty>();
        for(ModelProperty mp : p)
        {
            p2.add(new ModelProperty(mp.getUri().replaceFirst("profile/", ""), mp.getResolvedValue()));
        }
        
        ModelContainer mc = new IdModelContainerFactory(ProjectUri.Profiles.Profile.xUri).create(p2);
        for(ActiveProfileMatcher matcher : matchers)
        {
            if(matcher.isMatch(mc, interpolatorProperties))
            {
                return true;
            }
        }
        return false;
    }

    /* (non-Javadoc)
     * @see org.apache.maven.profiles.ProfileManager#addProfiles(java.util.List)
     */
    public void addProfiles( List profiles )
    {
        for ( Iterator it = profiles.iterator(); it.hasNext(); )
        {
            Profile profile = (Profile) it.next();

            addProfile( profile );
        }
    }

    public void activateAsDefault( String profileId )
    {
        List defaultIds = profileActivationContext.getActiveByDefaultProfileIds();

        if ( !defaultIds.contains( profileId ) )
        {
            profileActivationContext.setActiveByDefault( profileId );
        }
    }

    public List getExplicitlyActivatedIds()
    {
        return profileActivationContext.getExplicitlyActiveProfileIds();
    }

    public List getExplicitlyDeactivatedIds()
    {
        return profileActivationContext.getExplicitlyInactiveProfileIds();
    }

    public List getIdsActivatedByDefault()
    {
        return profileActivationContext.getActiveByDefaultProfileIds();
    }


    public static String getGroupId( Model model )
    {
        Parent parent = model.getParent();

        String groupId = model.getGroupId();
        if ( ( parent != null ) && ( groupId == null ) )
        {
            groupId = parent.getGroupId();
        }

        return groupId;
    }
}
