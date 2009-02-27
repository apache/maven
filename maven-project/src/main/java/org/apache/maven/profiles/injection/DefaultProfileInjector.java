package org.apache.maven.profiles.injection;

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

import org.apache.maven.model.*;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.project.builder.*;
import org.apache.maven.shared.model.ModelProperty;
import org.apache.maven.shared.model.ModelMarshaller;
import org.apache.maven.shared.model.ModelTransformerContext;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.util.xml.pull.XmlSerializer;
import org.codehaus.plexus.util.xml.pull.MXSerializer;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.codehaus.plexus.util.WriterFactory;
import org.codehaus.plexus.util.ReaderFactory;

import java.util.*;
import java.io.*;
import java.lang.reflect.Method;

/**
 * Inject profile data into a Model, using the profile as the dominant data source, and
 * persisting results of the injection in the Model.
 */
@Component(role = ProfileInjector.class)
public class DefaultProfileInjector
    implements ProfileInjector
{
    public Model inject( Profile profile, Model model )
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
        }
        catch (Exception e)
        {
            return null;
        }
        Set<String> uris = new HashSet(PomTransformer.URIS);
        uris.add(ProjectUri.Profiles.Profile.Build.Plugins.Plugin.configuration);

        List<ModelProperty> p;
        try
        {
            p = ModelMarshaller.marshallXmlToModelProperties(new ByteArrayInputStream(writer.getBuffer().toString().getBytes()),
                    ProjectUri.Profiles.xUri, uris);
        } catch (IOException e) {
            return null;
        }

            List<ModelProperty> transformed = new ArrayList<ModelProperty>();
            for(ModelProperty mp : p)
            {
                if(mp.getUri().startsWith(ProjectUri.Profiles.Profile.xUri) && !mp.getUri().equals(ProjectUri.Profiles.Profile.id)
                        && !mp.getUri().startsWith(ProjectUri.Profiles.Profile.Activation.xUri) )
                {
                    transformed.add(new ModelProperty(mp.getUri().replace(ProjectUri.Profiles.Profile.xUri, ProjectUri.xUri),
                            mp.getResolvedValue()));
                }
            }

        PomTransformer transformer = new PomTransformer( new PomClassicDomainModelFactory() );
        ModelTransformerContext ctx = new ModelTransformerContext(PomTransformer.MODEL_CONTAINER_INFOS );

        PomClassicDomainModel transformedDomainModel;
        try {
            transformedDomainModel = ( (PomClassicDomainModel) ctx.transform( Arrays.asList(  new PomClassicDomainModel(transformed), convertToDomainModel(model)), 
                                                                                                    transformer,
                                                                                                    transformer,
                                                                                                    Collections.EMPTY_LIST,
                                                                                                    null,
                                                                                                    null ) );
            return convertFromInputStreamToModel(transformedDomainModel.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }


    }

    private PomClassicDomainModel convertToDomainModel(Model model) throws IOException
    {
        if ( model == null )
        {
            throw new IllegalArgumentException( "model: null" );
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Writer out = null;
        MavenXpp3Writer writer = new MavenXpp3Writer();
        try
        {
            out = WriterFactory.newXmlWriter( baos );
            writer.write( out, model );
        }
        finally
        {
            if ( out != null )
            {
                out.close();
            }
        }
        return new PomClassicDomainModel(new ByteArrayInputStream(baos.toByteArray()));
    }

    private static Model convertFromInputStreamToModel(InputStream inputStream) throws IOException
    {

        try
        {
            return new MavenXpp3Reader().read( ReaderFactory.newXmlReader( inputStream ) );
        }
        catch ( XmlPullParserException e )
        {
            throw new IOException( e.getMessage() );
        }

    }

}
