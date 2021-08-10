package org.apache.maven.caching.xml;

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

import org.apache.maven.caching.jaxb.BuildDiffType;
import org.apache.maven.caching.jaxb.BuildInfoType;
import org.apache.maven.caching.jaxb.CacheReportType;
import org.apache.maven.caching.jaxb.ObjectFactory;
import org.codehaus.plexus.component.annotations.Component;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;

/**
 * XmlService
 */
@Component( role = XmlService.class )
public class XmlService
{

    private final ObjectFactory objectFactory;
    private final JAXBContext jaxbContext;
    private final Schema schema;

    public XmlService() throws JAXBException, SAXException
    {
        objectFactory = new ObjectFactory();
        jaxbContext = JAXBContext.newInstance( "org.apache.maven.caching.jaxb", XmlService.class.getClassLoader() );

        SchemaFactory sf = SchemaFactory.newInstance( XMLConstants.W3C_XML_SCHEMA_NS_URI );
        final InputStream domainSchemaStream = getResourceAsStream( "cache-domain.xsd" );
        final Source domainSchema = new StreamSource( domainSchemaStream );
        final InputStream configSchemaStream = getResourceAsStream( "cache-config.xsd" );
        final Source configSchema = new StreamSource( configSchemaStream );
        schema = sf.newSchema( new Source[] {domainSchema, configSchema} );
    }

    public byte[] toBytes( BuildInfoType buildInfo )
    {
        return serializeXml( objectFactory.createBuild( buildInfo ) );
    }

    public byte[] toBytes( BuildDiffType diff )
    {
        return serializeXml( objectFactory.createDiff( diff ) );
    }

    public byte[] toBytes( CacheReportType cacheReportType )
    {

        return serializeXml( objectFactory.createReport( cacheReportType ) );
    }

    private byte[] serializeXml( JAXBElement<?> element )
    {
        try
        {
            Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
            jaxbMarshaller.setSchema( schema );
            // output pretty printed
            jaxbMarshaller.setProperty( Marshaller.JAXB_FORMATTED_OUTPUT, true );
            jaxbMarshaller.setProperty( Marshaller.JAXB_ENCODING, "UTF-8" );
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            jaxbMarshaller.marshal( element, baos );
            return baos.toByteArray();
        }
        catch ( Exception e )
        {
            throw new RuntimeException( "Errors in jaxb serialization: " + e.toString(), e );
        }
    }

    public <T> T fromFile( Class<T> clazz, File file )
    {

        try
        {
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            unmarshaller.setSchema( schema );
            JAXBElement<T> result = (JAXBElement<T>) unmarshaller.unmarshal( file );
            return result.getValue();
        }
        catch ( Exception e )
        {
            throw new RuntimeException( "Errors in jaxb serialization: " + e.toString(), e );
        }
    }

    public static InputStream getResourceAsStream( String name )
    {
        ClassLoader cl = XmlService.class.getClassLoader();
        if ( cl == null )
        {
            // A system class.
            return ClassLoader.getSystemResourceAsStream( name );
        }
        return cl.getResourceAsStream( name );
    }

    public <T> T fromBytes( Class<T> clazz, byte[] bytes )
    {
        try
        {
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            unmarshaller.setSchema( schema );
            JAXBElement<T> result = (JAXBElement<T>) unmarshaller.unmarshal( new ByteArrayInputStream( bytes ) );
            return result.getValue();

        }
        catch ( Exception e )
        {
            throw new RuntimeException( "Errors in jaxb serialization: " + e.toString(), e );
        }
    }
}
