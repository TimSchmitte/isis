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
package org.apache.isis.viewer.json.viewer;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

import org.apache.isis.applib.profiles.Localization;
import org.apache.isis.core.commons.authentication.AuthenticationSession;
import org.apache.isis.core.metamodel.adapter.ObjectAdapterLookup;
import org.apache.isis.core.metamodel.adapter.oid.stringable.OidStringifier;
import org.apache.isis.runtimes.dflt.runtime.system.persistence.PersistenceSession;
import org.apache.isis.viewer.json.applib.RestfulRequest.QueryParameter;

public class ResourceContext {

    private final HttpHeaders httpHeaders;
    private final UriInfo uriInfo;
    private final Request request;
    private final HttpServletRequest httpServletRequest;
    private final HttpServletResponse httpServletResponse;
    private final SecurityContext securityContext;
    private final OidStringifier oidStringifier;
    private final Localization localization;
    private final AuthenticationSession authenticationSession;
    private final PersistenceSession persistenceSession;
    private final ObjectAdapterLookup objectAdapterLookup;

    public ResourceContext(
            final HttpHeaders httpHeaders, final UriInfo uriInfo, final Request request,
            final HttpServletRequest httpServletRequest, final HttpServletResponse httpServletResponse,
            final SecurityContext securityContext, 
            final OidStringifier oidStringifier, 
            final Localization localization, 
            final AuthenticationSession authenticationSession, 
            final PersistenceSession persistenceSession, 
            final ObjectAdapterLookup objectAdapterLookup) {

        this.httpHeaders = httpHeaders;
        this.uriInfo = uriInfo;
        this.request = request;
        this.httpServletRequest = httpServletRequest;
        this.httpServletResponse = httpServletResponse;
        this.securityContext = securityContext;
        this.oidStringifier = oidStringifier;
        this.localization = localization;
        this.authenticationSession = authenticationSession;
        this.persistenceSession = persistenceSession;
        this.objectAdapterLookup = objectAdapterLookup;
    }

    public HttpHeaders getHttpHeaders() {
        return httpHeaders;
    }

    public <Q> Q getArg(QueryParameter<Q> queryParameter) {
        return queryParameter.valueOf(getHttpServletRequest().getParameterMap());
    }


    public UriInfo getUriInfo() {
        return uriInfo;
    }

    public Request getRequest() {
        return request;
    }

    public HttpServletRequest getHttpServletRequest() {
        return httpServletRequest;
    }

    public HttpServletResponse getServletResponse() {
        return httpServletResponse;
    }

    public SecurityContext getSecurityContext() {
        return securityContext;
    }

    public String urlFor(String url) {
        return getUriInfo().getBaseUri().toString() + url;
    }

    
    public OidStringifier getOidStringifier() {
        return oidStringifier;
    }

    public Localization getLocalization() {
        return localization;
    }

    public AuthenticationSession getAuthenticationSession() {
        return authenticationSession;
    }

    public ObjectAdapterLookup getAdapterManager() {
        return objectAdapterLookup;
    }

    public PersistenceSession getPersistenceSession() {
        return persistenceSession;
    }



}