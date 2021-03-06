/*
 * Licensed to Neo4j under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Neo4j licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.neo4j.doc.server.rest;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.core.util.StringKeyObjectValueIgnoreCaseMultivaluedMap;
import org.junit.Test;

import org.neo4j.logging.NullLogProvider;
import org.neo4j.server.database.Database;
import org.neo4j.server.rest.management.AdvertisableService;
import org.neo4j.server.rest.management.console.ConsoleService;
import org.neo4j.server.rest.management.repr.ServerRootRepresentation;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

public class JaxRsResponse extends Response
{

    private final int status;
    private final MultivaluedMap<String,Object> metaData;
    private final MultivaluedMap<String, String> headers;
    private final URI location;
    private String data;
    private MediaType type;

    public JaxRsResponse( ClientResponse response )
    {
        this(response, extractContent(response));
    }

    private static String extractContent(ClientResponse response) {
        if (response.getStatus() == Status.NO_CONTENT.getStatusCode()) return null;
        return response.getEntity(String.class);
    }

    public JaxRsResponse(ClientResponse response, String entity) {
        status = response.getStatus();
        metaData = extractMetaData(response);
        headers = extractHeaders(response);
        location = response.getLocation();
        type = response.getType();
        data = entity;
        response.close();
    }

    @Override
    public String getEntity()
    {
        return data;
    }

    @Override
    public int getStatus()
    {
        return status;
    }

    @Override
    public MultivaluedMap<String, Object> getMetadata()
    {
        return metaData;
    }

    private MultivaluedMap<String, Object> extractMetaData(ClientResponse jettyResponse) {
        MultivaluedMap<String, Object> metadata = new StringKeyObjectValueIgnoreCaseMultivaluedMap();
        for ( Map.Entry<String, List<String>> header : jettyResponse.getHeaders()
                .entrySet() )
        {
            for ( Object value : header.getValue() )
            {
                metadata.putSingle( header.getKey(), value );
            }
        }
        return metadata;
    }

    public MultivaluedMap<String, String> getHeaders()
    {
        return headers;
    }

    private MultivaluedMap<String, String> extractHeaders(ClientResponse jettyResponse) {
        return jettyResponse.getHeaders();
    }

    // new URI( getHeaders().get( HttpHeaders.LOCATION ).get(0));
    public URI getLocation()
    {
        return location;
    }

    public static JaxRsResponse extractFrom(ClientResponse clientResponse) {
        return new JaxRsResponse(clientResponse);
    }

    public MediaType getType() {
        return type;
    }

    public static class ServerRootRepresentationTest
    {
        @Test
        public void shouldProvideAListOfServiceUris() throws Exception
        {
            ConsoleService consoleService = new ConsoleService( null, mock( Database.class ), NullLogProvider.getInstance(), null );
            ServerRootRepresentation srr = new ServerRootRepresentation( new URI( "http://example.org:9999" ),
                    Collections.<AdvertisableService>singletonList( consoleService ) );
            Map<String, Map<String, String>> map = srr.serialize();

            assertNotNull( map.get( "services" ) );

            assertThat( map.get( "services" )
                    .get( consoleService.getName() ), containsString( consoleService.getServerPath() ) );
        }
    }
}
