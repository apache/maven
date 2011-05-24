package org.apache.maven.router.repository;

/*******************************************************************************
 * Copyright (c) 2010-2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Apache License v2.0 which accompanies this distribution.
 * The Eclipse Public License is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 * The Apache License v2.0 is available at
 *   http://www.apache.org/licenses/LICENSE-2.0.html
 * You may elect to redistribute this code under either of these licenses.
 *******************************************************************************/

import java.util.Map;

import org.apache.maven.router.repository.WagonProvider;
import org.apache.maven.router.repository.WagonRepositoryConnector;
import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.providers.http.LightweightHttpWagon;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.spi.connector.RepositoryConnector;
import org.sonatype.aether.spi.connector.RepositoryConnectorFactory;
import org.sonatype.aether.spi.log.NullLogger;
import org.sonatype.aether.test.impl.TestFileProcessor;
import org.sonatype.aether.test.util.connector.suite.ConnectorTestSetup.AbstractConnectorTestSetup;
import org.sonatype.aether.test.util.connector.suite.ConnectorTestSuite;
import org.sonatype.aether.transfer.NoRepositoryConnectorException;
import org.sonatype.tests.http.server.jetty.behaviour.ResourceServer;
import org.sonatype.tests.http.server.jetty.impl.JettyServerProvider;

/**
 * @author Benjamin Hanzelmann
 */
public class TestSuiteHttpWagon
    extends ConnectorTestSuite
{

    private static class JettyConnectorTestSetup
        extends AbstractConnectorTestSetup
    {

        private JettyServerProvider provider;

        public RemoteRepository before( RepositorySystemSession session, Map<String, Object> context )
            throws Exception
        {
            provider = new JettyServerProvider();
            provider.initServer();
            provider.addBehaviour( "/*", new ResourceServer() );
            provider.start();
            return new RemoteRepository( "jetty-repo", "default", provider.getUrl().toString() + "/repo" );
        }

        public RepositoryConnectorFactory factory()
        {
            return new RepositoryConnectorFactory()
            {

                public RepositoryConnector newInstance( RepositorySystemSession session, RemoteRepository repository )
                    throws NoRepositoryConnectorException
                {
                    return new WagonRepositoryConnector( new WagonProvider()
                    {

                        public void release( Wagon wagon )
                        {
                            try
                            {
                                wagon.disconnect();
                            }
                            catch ( ConnectionException e )
                            {
                                throw new RuntimeException( e.getMessage(), e );
                            }
                        }

                        public Wagon lookup( String roleHint )
                            throws Exception
                        {
                            return new LightweightHttpWagon();
                        }

                    }, null, repository, session, TestFileProcessor.INSTANCE, NullLogger.INSTANCE );
                }

                public int getPriority()
                {
                    return 0;
                }
            };
        }

        @Override
        public void after( RepositorySystemSession session, RemoteRepository repository, Map<String, Object> context )
            throws Exception
        {
            if ( provider != null )
            {
                provider.stop();
                provider = null;
            }
        }

    }

    public TestSuiteHttpWagon()
    {
        super( new JettyConnectorTestSetup() );
    }

}
