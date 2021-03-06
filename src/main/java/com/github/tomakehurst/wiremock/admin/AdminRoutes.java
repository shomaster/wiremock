/*
 * Copyright (C) 2011 Thomas Akehurst
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.tomakehurst.wiremock.admin;

import com.github.tomakehurst.wiremock.admin.tasks.*;
import com.github.tomakehurst.wiremock.extension.AdminApiExtension;
import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.github.tomakehurst.wiremock.verification.notmatched.PlainTextStubNotMatchedRenderer;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableBiMap;

import java.util.Collections;
import java.util.Map;

import static com.github.tomakehurst.wiremock.admin.RequestSpec.requestSpec;
import static com.github.tomakehurst.wiremock.common.Exceptions.throwUnchecked;
import static com.github.tomakehurst.wiremock.http.RequestMethod.*;
import static com.google.common.collect.Iterables.tryFind;

public class AdminRoutes {
	
	public static final String CONTEXT_PATHPARAM = "context";

    private final ImmutableBiMap<RequestSpec, AdminTask> routes;
    private final Iterable<AdminApiExtension> apiExtensions;

    public static AdminRoutes defaults() {
        return new AdminRoutes(Collections.<AdminApiExtension>emptyList(), new PlainTextStubNotMatchedRenderer());
    }

    public static AdminRoutes defaultsPlus(Iterable<AdminApiExtension> apiExtensions, AdminTask notMatchedTask) {
        return new AdminRoutes(apiExtensions, notMatchedTask);
    }

    protected AdminRoutes(Iterable<AdminApiExtension> apiExtensions, AdminTask notMatchedTask) {
        this.apiExtensions = apiExtensions;
        RouteBuilder routeBuilder = new RouteBuilder();
        initDefaultRoutes(routeBuilder);
        initAdditionalRoutes(routeBuilder);
        routeBuilder.add(ANY, "/not-matched", notMatchedTask);
        routes = routeBuilder.build();
    }

    private void initDefaultRoutes(Router router) {
    	
    		router.add(POST, "/context", CreateContextTask.class);
    	
        router.add(GET,  "/{" + CONTEXT_PATHPARAM + "}/", RootTask.class);
        router.add(GET,  "/{" + CONTEXT_PATHPARAM + "}", RootRedirectTask.class);
        router.add(POST, "/{" + CONTEXT_PATHPARAM + "}/reset", ResetTask.class);

        router.add(GET,  "/{" + CONTEXT_PATHPARAM + "}/mappings", GetAllStubMappingsTask.class);
        router.add(POST, "/{" + CONTEXT_PATHPARAM + "}/mappings", CreateStubMappingTask.class);
        router.add(DELETE, "/{" + CONTEXT_PATHPARAM + "}/mappings", ResetStubMappingsTask.class);

        router.add(POST, "/{" + CONTEXT_PATHPARAM + "}/mappings/new", OldCreateStubMappingTask.class); // Deprecated
        router.add(POST, "/{" + CONTEXT_PATHPARAM + "}/mappings/remove", OldRemoveStubMappingTask.class);  // Deprecated
        router.add(POST, "/{" + CONTEXT_PATHPARAM + "}/mappings/edit", OldEditStubMappingTask.class);  // Deprecated
        router.add(POST, "/{" + CONTEXT_PATHPARAM + "}/mappings/save", SaveMappingsTask.class);
        router.add(POST, "/{" + CONTEXT_PATHPARAM + "}/mappings/reset", ResetToDefaultMappingsTask.class);
        router.add(GET,  "/{" + CONTEXT_PATHPARAM + "}/mappings/{id}", GetStubMappingTask.class);
        router.add(PUT,  "/{" + CONTEXT_PATHPARAM + "}/mappings/{id}", EditStubMappingTask.class);
        router.add(DELETE, "/{" + CONTEXT_PATHPARAM + "}/mappings/{id}", RemoveStubMappingTask.class);

        router.add(GET, "/{" + CONTEXT_PATHPARAM + "}/scenarios", GetAllScenariosTask.class);
        router.add(POST, "/{" + CONTEXT_PATHPARAM + "}/scenarios/reset", ResetScenariosTask.class);

        router.add(GET,  "/{" + CONTEXT_PATHPARAM + "}/requests", GetAllRequestsTask.class);
        router.add(DELETE,  "/{" + CONTEXT_PATHPARAM + "}/requests", ResetRequestsTask.class);
        router.add(POST, "/{" + CONTEXT_PATHPARAM + "}/requests/reset", OldResetRequestsTask.class);  // Deprecated
        router.add(POST, "/{" + CONTEXT_PATHPARAM + "}/requests/count", GetRequestCountTask.class);
        router.add(POST, "/{" + CONTEXT_PATHPARAM + "}/requests/find", FindRequestsTask.class);
        router.add(GET,  "/{" + CONTEXT_PATHPARAM + "}/requests/unmatched", FindUnmatchedRequestsTask.class);
        router.add(GET,  "/{" + CONTEXT_PATHPARAM + "}/requests/unmatched/near-misses", FindNearMissesForUnmatchedTask.class);
        router.add(GET,  "/{" + CONTEXT_PATHPARAM + "}/requests/{id}", GetServedStubTask.class);

        router.add(POST, "/{" + CONTEXT_PATHPARAM + "}/recordings/snapshot", SnapshotTask.class);
        router.add(POST, "/{" + CONTEXT_PATHPARAM + "}/recordings/start", StartRecordingTask.class);
        router.add(POST, "/{" + CONTEXT_PATHPARAM + "}/recordings/stop", StopRecordingTask.class);
        router.add(GET,  "/{" + CONTEXT_PATHPARAM + "}/recordings/status", GetRecordingStatusTask.class);
        router.add(GET,  "/{" + CONTEXT_PATHPARAM + "}/recorder", GetRecordingsIndexTask.class);

        router.add(POST, "/{" + CONTEXT_PATHPARAM + "}/near-misses/request", FindNearMissesForRequestTask.class);
        router.add(POST, "/{" + CONTEXT_PATHPARAM + "}/near-misses/request-pattern", FindNearMissesForRequestPatternTask.class);

        router.add(POST, "/{" + CONTEXT_PATHPARAM + "}/settings", GlobalSettingsUpdateTask.class);
        router.add(POST, "/{" + CONTEXT_PATHPARAM + "}/shutdown", ShutdownServerTask.class);

        router.add(GET, "/{" + CONTEXT_PATHPARAM + "}/docs/raml", GetRamlSpecTask.class);
        router.add(GET, "/{" + CONTEXT_PATHPARAM + "}/docs/swagger", GetSwaggerSpecTask.class);
        router.add(GET, "/{" + CONTEXT_PATHPARAM + "}/docs", GetDocIndexTask.class);
    }

    protected void initAdditionalRoutes(Router routeBuilder) {
        for (AdminApiExtension apiExtension: apiExtensions) {
            apiExtension.contributeAdminApiRoutes(routeBuilder);
        }
    }

    public AdminTask taskFor(final RequestMethod method, final String path) {
        return tryFind(routes.entrySet(), new Predicate<Map.Entry<RequestSpec, AdminTask>>() {
            @Override
            public boolean apply(Map.Entry<RequestSpec, AdminTask> entry) {
                return entry.getKey().matches(method, path);
            }
        }).transform(new Function<Map.Entry<RequestSpec, AdminTask>, AdminTask>() {
            @Override
            public AdminTask apply(Map.Entry<RequestSpec, AdminTask> input) {
                return input.getValue();
            }
        }).or(new NotFoundAdminTask());
    }

    public RequestSpec requestSpecForTask(final Class<? extends AdminTask> taskClass) {
        RequestSpec requestSpec = tryFind(routes.entrySet(), new Predicate<Map.Entry<RequestSpec, AdminTask>>() {
            @Override
            public boolean apply(Map.Entry<RequestSpec, AdminTask> input) {
                return input.getValue().getClass().equals(taskClass);
            }
        }).transform(new Function<Map.Entry<RequestSpec,AdminTask>, RequestSpec>() {
            @Override
            public RequestSpec apply(Map.Entry<RequestSpec, AdminTask> input) {
                return input.getKey();
            }
        }).orNull();

        if (requestSpec == null) {
            throw new NotFoundException("No route could be found for " + taskClass.getSimpleName());
        }

        return requestSpec;
    }

    protected static class RouteBuilder implements Router {
        private final ImmutableBiMap.Builder<RequestSpec, AdminTask> builder;

        public RouteBuilder() {
            builder = ImmutableBiMap.builder();
        }

        @Override
        public void add(RequestMethod method, String urlTemplate, Class<? extends AdminTask> taskClass) {
            try {
                AdminTask task = taskClass.newInstance();
                add(requestSpec(method, urlTemplate), task);
            } catch (Exception e) {
                throwUnchecked(e);
            }
        }

        @Override
        public void add(RequestMethod method, String urlTemplate, AdminTask task) {
            add(requestSpec(method, urlTemplate), task);
        }

        public void add(RequestSpec requestSpec, AdminTask task) {
            builder.put(requestSpec, task);
        }

        ImmutableBiMap<RequestSpec, AdminTask> build() {
            return builder.build();
        }
    }
}
