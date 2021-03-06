/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.isis.viewer.restfulobjects.tck.domainobjectorservice.id.action.invoke;

import java.io.IOException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.apache.isis.viewer.restfulobjects.applib.JsonRepresentation;
import org.apache.isis.viewer.restfulobjects.applib.LinkRepresentation;
import org.apache.isis.viewer.restfulobjects.applib.Rel;
import org.apache.isis.viewer.restfulobjects.applib.RestfulHttpMethod;
import org.apache.isis.viewer.restfulobjects.applib.client.RestfulClient;
import org.apache.isis.viewer.restfulobjects.applib.client.RestfulResponse;
import org.apache.isis.viewer.restfulobjects.applib.client.RestfulResponse.Header;
import org.apache.isis.viewer.restfulobjects.applib.client.RestfulResponse.HttpStatusCode;
import org.apache.isis.viewer.restfulobjects.applib.domainobjects.ActionResultRepresentation;
import org.apache.isis.viewer.restfulobjects.applib.domainobjects.DomainServiceResource;
import org.apache.isis.viewer.restfulobjects.applib.domainobjects.ObjectActionRepresentation;
import org.apache.isis.viewer.restfulobjects.applib.util.UrlEncodingUtils;
import org.apache.isis.viewer.restfulobjects.tck.IsisWebServerRule;
import org.apache.isis.viewer.restfulobjects.tck.RestfulMatchers;
import org.apache.isis.viewer.restfulobjects.tck.Util;

import static org.apache.isis.viewer.restfulobjects.tck.RestfulMatchers.hasMediaTypeProfile;
import static org.apache.isis.viewer.restfulobjects.tck.RestfulMatchers.hasStatus;
import static org.apache.isis.viewer.restfulobjects.tck.RestfulMatchers.isLink;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class Get_givenMandatorySimpleArg_whenArgNullValue_bad {

    @Rule
    public IsisWebServerRule webServerRule = new IsisWebServerRule();

    private RestfulClient client;

    private DomainServiceResource serviceResource;

    @Before
    public void setUp() throws Exception {
        client = webServerRule.getClient();

        serviceResource = client.getDomainServiceResource();
    }
    
    @Test
    public void usingClientFollow() throws Exception {

        // given
        final JsonRepresentation givenAction = Util.givenAction(client, "ActionsEntities", "subList");
        final ObjectActionRepresentation actionRepr = givenAction.as(ObjectActionRepresentation.class);

        final LinkRepresentation invokeLink = actionRepr.getInvoke();

        assertThat(invokeLink, isLink(client)
                                    .rel(Rel.INVOKE)
                                    .httpMethod(RestfulHttpMethod.GET)
                                    .href(Matchers.endsWith(":39393/services/ActionsEntities/actions/subList/invoke"))
                                    .build());
        
        JsonRepresentation args =invokeLink.getArguments();
        assertThat(args.size(), is(2));
        assertThat(args, RestfulMatchers.mapHas("from"));
        assertThat(args, RestfulMatchers.mapHas("to"));
        
        // when
        args.mapPut("from.value", (Integer)null);
        args.mapPut("to.value", 0);

        final RestfulResponse<ActionResultRepresentation> restfulResponse = client.followT(invokeLink, args);
        
        // then
        thenResponseIsErrorWithInvalidReason(restfulResponse);
    }


    @Test
    public void usingResourceProxy() throws Exception {

        // given, when
        JsonRepresentation args = JsonRepresentation.newMap();
        args.mapPut("from.value", (Integer)null);
        args.mapPut("to.value", 0);
        Response response = serviceResource.invokeActionQueryOnly("ActionsEntities", "subList", UrlEncodingUtils.urlEncode(args));
        RestfulResponse<ActionResultRepresentation> restfulResponse = RestfulResponse.ofT(response);
        
        // then
        thenResponseIsErrorWithInvalidReason(restfulResponse);
    }

    private static void thenResponseIsErrorWithInvalidReason(final RestfulResponse<ActionResultRepresentation> restfulResponse) throws IOException {
        assertThat(restfulResponse, hasStatus(HttpStatusCode.VALIDATION_FAILED));
        assertThat(restfulResponse.getHeader(Header.WARNING), is("Validation failed, see body for details"));

        // hmmm... what is the media type, though?  the spec doesn't say.  testing for a generic one.
        assertThat(restfulResponse.getHeader(Header.CONTENT_TYPE), hasMediaTypeProfile(MediaType.APPLICATION_JSON));

        RestfulResponse<JsonRepresentation> restfulResponseOfError = restfulResponse.wraps(JsonRepresentation.class);
        JsonRepresentation repr = restfulResponseOfError.getEntity();
        
        assertThat(repr.getString("from.invalidReason"), is("'from' is mandatory"));
        // TODO: really ought to be null, but ObjectActionImpl.isProposedArgumentSetValidResultSet also checks that each argument is valid
        assertThat(repr.getString("x-ro-invalidReason"), is("'from' is mandatory"));
    }



}
