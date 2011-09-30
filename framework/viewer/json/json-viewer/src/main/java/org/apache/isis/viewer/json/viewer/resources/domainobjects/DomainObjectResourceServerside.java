/**
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.isis.viewer.json.viewer.resources.domainobjects;

import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.apache.isis.core.metamodel.adapter.ObjectAdapter;
import org.apache.isis.core.metamodel.adapter.version.Version;
import org.apache.isis.core.metamodel.consent.Consent;
import org.apache.isis.core.metamodel.spec.ObjectSpecification;
import org.apache.isis.core.metamodel.spec.feature.OneToManyAssociation;
import org.apache.isis.core.metamodel.spec.feature.OneToOneAssociation;
import org.apache.isis.viewer.json.applib.RepresentationType;
import org.apache.isis.viewer.json.applib.RestfulMediaType;
import org.apache.isis.viewer.json.applib.RestfulResponse.HttpStatusCode;
import org.apache.isis.viewer.json.applib.domainobjects.DomainObjectResource;
import org.apache.isis.viewer.json.viewer.JsonApplicationException;
import org.apache.isis.viewer.json.viewer.ResourceContext;
import org.apache.isis.viewer.json.viewer.representations.ReprBuilderAbstract;
import org.apache.isis.viewer.json.viewer.representations.TypedReprBuilder;
import org.apache.isis.viewer.json.viewer.representations.TypedReprBuilderFactory;

@Path("/objects")
public class DomainObjectResourceServerside extends DomainResourceAbstract implements
        DomainObjectResource {

    private static final DateFormat ETAG_FORMAT = 
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    ////////////////////////////////////////////////////////////
    // domain object
    ////////////////////////////////////////////////////////////
    


    @GET
    @Path("/{oid}")
    @Produces({ MediaType.APPLICATION_JSON, RestfulMediaType.APPLICATION_JSON_DOMAIN_OBJECT, RestfulMediaType.APPLICATION_JSON_ERROR })
    public Response object(
            @PathParam("oid") final String oidStr) {
        init();

        final ObjectAdapter objectAdapter = getObjectAdapter(oidStr);
        
        final TypedReprBuilderFactory reprBuilderBuilder = 
                builderFactoryRegistry.find(RepresentationType.DOMAIN_OBJECT);
        
        final DomainObjectReprBuilder repBuilder = 
                (DomainObjectReprBuilder) reprBuilderBuilder.newBuilder(getResourceContext());
        repBuilder.with(objectAdapter);
        
        ResponseBuilder respBuilder = 
                responseOfOk(RepresentationType.DOMAIN_OBJECT, Caching.NONE, repBuilder);
        
        Version version = objectAdapter.getVersion();
        if (version != null && version.getTime() != null) {
            respBuilder.tag(ETAG_FORMAT.format(version.getTime()));
        }
        return respBuilder.build();
    }

    @PUT
    @Path("/{oid}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON, RestfulMediaType.APPLICATION_JSON_DOMAIN_OBJECT, RestfulMediaType.APPLICATION_JSON_ERROR })
    public Response object(
        @PathParam("oid") final String oidStr, 
        final InputStream arguments) {

        init();

        // TODO
        throw new UnsupportedOperationException();
    }

    ////////////////////////////////////////////////////////////
    // domain object property
    ////////////////////////////////////////////////////////////

    @GET
    @Path("/{oid}/properties/{propertyId}")
    @Produces({ MediaType.APPLICATION_JSON, RestfulMediaType.APPLICATION_JSON_OBJECT_PROPERTY, RestfulMediaType.APPLICATION_JSON_ERROR })
    public Response propertyDetails(
            @PathParam("oid") final String oidStr,
            @PathParam("propertyId") final String propertyId) {
        init();
        
        final ObjectAdapter objectAdapter = getObjectAdapter(oidStr);
        final OneToOneAssociation property = getPropertyThatIsVisibleAndUsable(
                objectAdapter, propertyId, Intent.ACCESS);

        ResourceContext resourceContext = getResourceContext();
        final ObjectPropertyReprBuilder builder = ObjectPropertyReprBuilder.newBuilder(
                resourceContext, objectAdapter, property);
        
        return responseOfOk(RepresentationType.OBJECT_PROPERTY, Caching.NONE, builder).build();
    }

    @PUT
    @Path("/{oid}/properties/{propertyId}")
    @Produces({ MediaType.APPLICATION_JSON, RestfulMediaType.APPLICATION_JSON_ERROR })
    public Response modifyProperty(
            @PathParam("oid") final String oidStr,
            @PathParam("propertyId") final String propertyId,
            final InputStream body) {
        init();

        final ObjectAdapter objectAdapter = getObjectAdapter(oidStr);
        final OneToOneAssociation property = getPropertyThatIsVisibleAndUsable(
                objectAdapter, propertyId, Intent.MUTATE);

        ObjectSpecification propertySpec = property.getSpecification();
        String bodyAsString = asStringUtf8(body);

        ObjectAdapter argAdapter = parseBodyAsMapWithSingleValue(propertySpec, bodyAsString);

        Consent consent = property.isAssociationValid(objectAdapter, argAdapter);
        if (consent.isVetoed()) {
            throw JsonApplicationException.create(
                    HttpStatusCode.UNAUTHORIZED, 
                    consent.getReason());
        }

        property.set(objectAdapter, argAdapter);

        return Response.status(HttpStatusCode.NO_CONTENT.getJaxrsStatusType())
                .build();
    }

    @DELETE
    @Path("/{oid}/properties/{propertyId}")
    @Produces({ MediaType.APPLICATION_JSON, RestfulMediaType.APPLICATION_JSON_ERROR })
    public Response clearProperty(
            @PathParam("oid") final String oidStr,
            @PathParam("propertyId") final String propertyId) {
        init();

        final ObjectAdapter objectAdapter = getObjectAdapter(oidStr);
        final OneToOneAssociation property = getPropertyThatIsVisibleAndUsable(
                objectAdapter, propertyId, Intent.MUTATE);

        Consent consent = property.isAssociationValid(objectAdapter, null);
        if (consent.isVetoed()) {
            throw JsonApplicationException.create(
                    HttpStatusCode.UNAUTHORIZED, consent.getReason());
        }

        property.set(objectAdapter, null);

        return responseOfNoContent(objectAdapter.getVersion()).build();
    }


    ////////////////////////////////////////////////////////////
    // domain object collection
    ////////////////////////////////////////////////////////////

    @GET
    @Path("/{oid}/collections/{collectionId}")
    @Produces({ MediaType.APPLICATION_JSON, RestfulMediaType.APPLICATION_JSON_OBJECT_COLLECTION, RestfulMediaType.APPLICATION_JSON_ERROR })
    public Response accessCollection(
        @PathParam("oid") final String oidStr,
        @PathParam("collectionId") final String collectionId) {

        final ObjectAdapter objectAdapter = getObjectAdapter(oidStr);
        final OneToManyAssociation collection = getCollectionThatIsVisibleAndUsable(
                objectAdapter, collectionId, Intent.ACCESS);

        final ObjectCollectionReprBuilder builder = ObjectCollectionReprBuilder.newBuilder(
                getResourceContext(), objectAdapter, collection);
        
        return Response.status(HttpStatusCode.OK.getJaxrsStatusType())
                .entity(jsonFor(builder))
                .type(MediaType.APPLICATION_JSON_TYPE)
                .build();
    }

    @PUT
    @Path("/{oid}/collections/{collectionId}")
    @Produces({ MediaType.APPLICATION_JSON, RestfulMediaType.APPLICATION_JSON_ERROR })
    public Response addToSet(
            @PathParam("oid") final String oidStr,
            @PathParam("collectionId") final String collectionId,
            final InputStream body) {
        init();

        final ObjectAdapter objectAdapter = getObjectAdapter(oidStr);
        final OneToManyAssociation collection = getCollectionThatIsVisibleAndUsable(
                objectAdapter, collectionId, Intent.MUTATE);

        if (!collection.getCollectionSemantics().isSet()) {
            throw JsonApplicationException.create(
                    HttpStatusCode.BAD_REQUEST, 
                    "Collection '%s' does not have set semantics", collectionId);
        }

        ObjectSpecification collectionSpec = collection.getSpecification();
        String bodyAsString = asStringUtf8(body);
        ObjectAdapter argAdapter = parseBodyAsMapWithSingleValue(collectionSpec, bodyAsString);

        Consent consent = collection.isValidToAdd(objectAdapter, argAdapter);
        if (consent.isVetoed()) {
            throw JsonApplicationException.create(
                    HttpStatusCode.UNAUTHORIZED, 
                    consent.getReason());
        }

        collection.addElement(objectAdapter, argAdapter);
        
        return Response.status(HttpStatusCode.NO_CONTENT.getJaxrsStatusType()).build();
    }

    @POST
    @Path("/{oid}/collections/{collectionId}")
    @Produces({ MediaType.APPLICATION_JSON, RestfulMediaType.APPLICATION_JSON_ERROR })
    public Response addToList(
            @PathParam("oid") final String oidStr,
            @PathParam("collectionId") final String collectionId,
            final InputStream body) {
        init();

        final ObjectAdapter objectAdapter = getObjectAdapter(oidStr);
        final OneToManyAssociation collection = getCollectionThatIsVisibleAndUsable(
                objectAdapter, collectionId, Intent.MUTATE);

        if (!collection.getCollectionSemantics().isListOrArray()) {
            throw JsonApplicationException.create(
                    HttpStatusCode.METHOD_NOT_ALLOWED, 
                    "Collection '%s' does not have list or array semantics", collectionId);
        }

        ObjectSpecification collectionSpec = collection.getSpecification();
        String bodyAsString = asStringUtf8(body);
        ObjectAdapter argAdapter = parseBodyAsMapWithSingleValue(collectionSpec, bodyAsString);

        Consent consent = collection.isValidToAdd(objectAdapter, argAdapter);
        if (consent.isVetoed()) {
            throw JsonApplicationException.create(
                    HttpStatusCode.UNAUTHORIZED, 
                    consent.getReason());
        }

        collection.addElement(objectAdapter, argAdapter);
        
        return responseOfNoContent(objectAdapter.getVersion()).build();
    }

    @DELETE
    @Path("/{oid}/collections/{collectionId}")
    @Produces({ MediaType.APPLICATION_JSON, RestfulMediaType.APPLICATION_JSON_ERROR })
    public Response removeFromCollection(
        @PathParam("oid") final String oidStr,
        @PathParam("collectionId") final String collectionId,
        final InputStream body) {

        final ObjectAdapter objectAdapter = getObjectAdapter(oidStr);
        final OneToManyAssociation collection = getCollectionThatIsVisibleAndUsable(
                objectAdapter, collectionId, Intent.MUTATE);

        ObjectSpecification collectionSpec = collection.getSpecification();
        String bodyAsString = asStringUtf8(body);
        ObjectAdapter argAdapter = parseBodyAsMapWithSingleValue(collectionSpec, bodyAsString);

        Consent consent = collection.isValidToRemove(objectAdapter, argAdapter);
        if (consent.isVetoed()) {
            throw JsonApplicationException.create(
                    HttpStatusCode.UNAUTHORIZED, consent.getReason());
        }

        collection.removeElement(objectAdapter, argAdapter);
        
        return responseOfNoContent(objectAdapter.getVersion()).build();
    }


    ////////////////////////////////////////////////////////////
    // domain object action
    ////////////////////////////////////////////////////////////

    @GET
    @Path("/{oid}/actions/{actionId}")
    @Produces({ MediaType.APPLICATION_JSON, RestfulMediaType.APPLICATION_JSON_OBJECT_ACTION, RestfulMediaType.APPLICATION_JSON_ERROR })
    public Response actionPrompt(
            @PathParam("oid") final String oidStr,
            @PathParam("actionId") final String actionId) {
        init();

        final ObjectAdapter objectAdapter = getObjectAdapter(oidStr);
        return actionPrompt(actionId, objectAdapter);
    }


    
    ////////////////////////////////////////////////////////////
    // domain object action invoke
    ////////////////////////////////////////////////////////////

    @GET
    @Path("/{oid}/actions/{actionId}/invoke")
    @Produces({ MediaType.APPLICATION_JSON, RestfulMediaType.APPLICATION_JSON_DOMAIN_OBJECT, RestfulMediaType.APPLICATION_JSON_LIST, RestfulMediaType.APPLICATION_JSON_SCALAR_VALUE, RestfulMediaType.APPLICATION_JSON_ERROR })
    public Response invokeActionQueryOnly(
            @PathParam("oid") final String oidStr,
            @PathParam("actionId") final String actionId,
            @QueryParam("args") final String arguments) {
        init();

        final ObjectAdapter objectAdapter = getObjectAdapter(oidStr);

        return invokeActionQueryOnly(objectAdapter, actionId, arguments);
    }

    @PUT
    @Path("/{oid}/actions/{actionId}/invoke")
    @Produces({ MediaType.APPLICATION_JSON, RestfulMediaType.APPLICATION_JSON_DOMAIN_OBJECT, RestfulMediaType.APPLICATION_JSON_LIST, RestfulMediaType.APPLICATION_JSON_SCALAR_VALUE, RestfulMediaType.APPLICATION_JSON_ERROR })
    public Response invokeActionIdempotent(
            @PathParam("oid") final String oidStr,
            @PathParam("actionId") final String actionId,
            final InputStream arguments) {
        init();

        final ObjectAdapter objectAdapter = getObjectAdapter(oidStr);
        return invokeActionIdempotent(objectAdapter, actionId, arguments);
    }


    @POST
    @Path("/{oid}/actions/{actionId}/invoke")
    @Produces({ MediaType.APPLICATION_JSON, RestfulMediaType.APPLICATION_JSON_DOMAIN_OBJECT, RestfulMediaType.APPLICATION_JSON_LIST, RestfulMediaType.APPLICATION_JSON_SCALAR_VALUE, RestfulMediaType.APPLICATION_JSON_ERROR })
    public Response invokeAction(
            @PathParam("oid") final String oidStr,
            @PathParam("actionId") final String actionId,
            final InputStream body) {
        init();

        final ObjectAdapter objectAdapter = getObjectAdapter(oidStr);
        return invokeAction(objectAdapter, actionId, body);
    }



}