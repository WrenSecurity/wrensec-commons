/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2016 ForgeRock AS.
 * Portions Copyright 2018-2026 Wren Security.
 */
package org.forgerock.api.transform;

import static java.lang.Boolean.TRUE;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableList;
import static org.forgerock.api.markup.asciidoc.AsciiDoc.normalizeName;
import static org.forgerock.api.util.PathUtil.buildPath;
import static org.forgerock.api.util.PathUtil.buildPathParameters;
import static org.forgerock.api.util.PathUtil.mergeParameters;
import static org.forgerock.api.util.ValidationUtil.isEmpty;
import static org.forgerock.json.JsonValue.array;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.fieldIfNotNull;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.JsonValueFunctions.listOf;
import static org.forgerock.json.schema.validator.Constants.DEFAULT;
import static org.forgerock.json.schema.validator.Constants.DESCRIPTION;
import static org.forgerock.json.schema.validator.Constants.ENUM;
import static org.forgerock.json.schema.validator.Constants.ID;
import static org.forgerock.json.schema.validator.Constants.ITEMS;
import static org.forgerock.json.schema.validator.Constants.PROPERTIES;
import static org.forgerock.json.schema.validator.Constants.REQUIRED;
import static org.forgerock.json.schema.validator.Constants.TITLE;
import static org.forgerock.json.schema.validator.Constants.TYPE;
import static org.forgerock.json.schema.validator.Constants.TYPE_ARRAY;
import static org.forgerock.json.schema.validator.Constants.TYPE_INTEGER;
import static org.forgerock.json.schema.validator.Constants.TYPE_NULL;
import static org.forgerock.json.schema.validator.Constants.TYPE_OBJECT;
import static org.forgerock.json.schema.validator.Constants.TYPE_STRING;
import static org.forgerock.util.Reject.checkNotNull;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.servers.Server;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.forgerock.api.enums.CountPolicy;
import org.forgerock.api.enums.PagingMode;
import org.forgerock.api.enums.PatchOperation;
import org.forgerock.api.enums.QueryType;
import org.forgerock.api.enums.Stability;
import org.forgerock.api.markup.asciidoc.AsciiDoc;
import org.forgerock.api.models.Action;
import org.forgerock.api.models.ApiDescription;
import org.forgerock.api.models.ApiError;
import org.forgerock.api.models.Create;
import org.forgerock.api.models.Definitions;
import org.forgerock.api.models.Delete;
import org.forgerock.api.models.Items;
import org.forgerock.api.models.Parameter;
import org.forgerock.api.models.Patch;
import org.forgerock.api.models.Paths;
import org.forgerock.api.models.Query;
import org.forgerock.api.models.Read;
import org.forgerock.api.models.Reference;
import org.forgerock.api.models.Resource;
import org.forgerock.api.models.Schema;
import org.forgerock.api.models.SubResources;
import org.forgerock.api.models.Update;
import org.forgerock.api.models.VersionedPath;
import org.forgerock.api.util.BigDecimalUtil;
import org.forgerock.api.util.PathUtil;
import org.forgerock.api.util.ReferenceResolver;
import org.forgerock.api.util.ValidationUtil;
import org.forgerock.http.header.AcceptApiVersionHeader;
import org.forgerock.http.routing.Version;
import org.forgerock.http.swagger.SwaggerExtended;
import org.forgerock.json.JsonValue;
import org.forgerock.json.JsonValueException;
import org.forgerock.util.Function;
import org.forgerock.util.annotations.VisibleForTesting;
import org.forgerock.util.i18n.LocalizableString;
import org.forgerock.util.i18n.PreferredLocales;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wrensecurity.guava.common.base.Joiner;
import org.wrensecurity.guava.common.hash.Hashing;
import org.wrensecurity.guava.common.io.BaseEncoding;

/**
 * Transforms an {@link ApiDescription} into an OpenAPI 3.0 model.
 *
 * @see <a href="https://github.com/OAI/OpenAPI-Specification/blob/main/versions/3.0.3.md">OpenAPI 3.0</a> spec
 */
public class OpenApiTransformer {

    private static final Logger logger = LoggerFactory.getLogger(OpenApiTransformer.class);

    private static final String EMPTY_STRING = "";

    private static final String PARAMETER_FIELDS = "_fields";
    private static final String PARAMETER_PRETTY_PRINT = "_prettyPrint";
    private static final String PARAMETER_MIME_TYPE = "_mimeType";
    private static final String PARAMETER_IF_MATCH = "If-Match";
    private static final String PARAMETER_IF_NONE_MATCH = "If-None-Match";
    private static final String PARAMETER_IF_NONE_MATCH_ANY_ONLY = "If-None-Match: *";
    private static final String PARAMETER_IF_NONE_MATCH_REV_ONLY = "If-None-Match: <rev>";
    private static final String PARAMETER_LOCATION = "Location";

    /** JSON Reference prefix for component schemas (replaces v2 #/definitions/). */
    private static final String SCHEMAS_REF_PREFIX = "#/components/schemas/";

    /**
     * Prefix for JSON Schema references prefixed with {@code urn:jsonschema:}.
     * <p/>
     * Note that, by default, Jackson uses this scheme in
     * {@link com.fasterxml.jackson.module.jsonSchema.factories.VisitorContext}.
     */
    static final String URN_JSONSCHEMA_PREFIX = "urn:jsonschema:";

    /** Prefix for ForgeRock API JSON Schema references, prefixed with {@code frapi:}. */
    static final String FRAPI_PREFIX = "frapi:";

    private static final String I18N_PREFIX = LocalizableString.TRANSLATION_KEY_PREFIX + "ApiDescription#";
    private static final String FIELDS_PARAMETER_DESCRIPTION = I18N_PREFIX + "common.parameters.fields";
    private static final String PRETTYPRINT_PARAMETER_DESCRIPTION = I18N_PREFIX + "common.parameters.prettyprint";
    private static final String MIMETYPE_PARAMETER_DESCRIPTION = I18N_PREFIX + "common.parameters.mimetype";
    private static final String LOCATION_PARAMETER_DESCRIPTION = I18N_PREFIX + "common.parameters.location";

    @VisibleForTesting
    final OpenAPI openApi;
    private final ReferenceResolver referenceResolver;
    private final ApiDescription apiDescription;
    @SuppressWarnings("rawtypes")
    private final Map<String, io.swagger.v3.oas.models.media.Schema> definitionMap = new HashMap<>();

    /** {@code Location}-header description. */
    private final LocalizableString locationDescription =
            new LocalizableString(LOCATION_PARAMETER_DESCRIPTION, getClass().getClassLoader());

    /** Default constructor that is only used by unit tests. */
    @VisibleForTesting
    OpenApiTransformer() {
        openApi = null;
        referenceResolver = null;
        apiDescription = null;
    }

    /**
     * Constructor.
     *
     * @param title API title
     * @param host Hostname or IP address, with optional port
     * @param basePath Base-path on host
     * @param secure {@code true} when host is using HTTPS and {@code false} when using HTTP
     * @param apiDescription CREST API Descriptor
     * @param externalApiDescriptions External CREST API Descriptions, for resolving {@link Reference}s, or {@code null}
     */
    @VisibleForTesting
    OpenApiTransformer(final LocalizableString title, final String host, final String basePath, final boolean secure,
            final ApiDescription apiDescription, final ApiDescription... externalApiDescriptions) {
        this.apiDescription = checkNotNull(apiDescription, "apiDescription required");

        openApi = new SwaggerExtended();
        openApi.setComponents(new Components());
        openApi.setPaths(new io.swagger.v3.oas.models.Paths());
        openApi.info(buildInfo(title));

        // Build server URL from host/basePath/scheme
        if (!isEmpty(host)) {
            String scheme = secure ? "https" : "http";
            String serverUrl = scheme + "://" + host;
            if (!isEmpty(basePath)) {
                serverUrl += PathUtil.buildPath(basePath);
            }
            openApi.addServersItem(new Server().url(serverUrl));
        } else if (!isEmpty(basePath)) {
            openApi.addServersItem(new Server().url(PathUtil.buildPath(basePath)));
        }

        referenceResolver = new ReferenceResolver(apiDescription);
        if (externalApiDescriptions != null) {
            referenceResolver.registerAll(externalApiDescriptions);
        }
    }

    /**
     * Transforms an {@link ApiDescription} into an {@code OpenAPI} model.
     *
     * @param title API title
     * @param host Hostname or IP address, with optional port
     * @param basePath Base-path on host
     * @param secure {@code true} when host is using HTTPS and {@code false} when using HTTP
     * @param apiDescription CREST API Descriptor
     * @param externalApiDescriptions External CREST API Descriptions, for resolving {@link Reference}s, or {@code null}
     * @return {@code OpenAPI} model
     */
    public static OpenAPI execute(final LocalizableString title, final String host, final String basePath,
            final boolean secure, final ApiDescription apiDescription,
            final ApiDescription... externalApiDescriptions) {
        final OpenApiTransformer transformer = new OpenApiTransformer(title, host, basePath, secure, apiDescription,
                externalApiDescriptions);
        return transformer.doExecute();
    }

    /**
     * Transforms an {@link ApiDescription} into an {@code OpenAPI} model.
     * <p>
     * Note: The returned descriptor does not contain an {@code Info} object, a server, as
     * these will all depend on the deployment and/or request.
     * </p>
     *
     * @param apiDescription CREST API Descriptor
     * @param externalApiDescriptions External CREST API Descriptions, for resolving {@link Reference}s, or {@code null}
     * @return {@code OpenAPI} model
     */
    public static OpenAPI execute(ApiDescription apiDescription, ApiDescription... externalApiDescriptions) {
        final OpenApiTransformer transformer = new OpenApiTransformer(null, null, null, false, apiDescription,
                externalApiDescriptions);
        return transformer.doExecute();
    }

    /**
     * Do the work to transform an {@link ApiDescription} into an {@code OpenAPI} model.
     *
     * @return {@code OpenAPI} model
     */
    private OpenAPI doExecute() {
        buildParameters();
        buildPaths();
        buildDefinitions();
        return openApi;
    }

    /** Build globally-defined parameters, which are referred to by-reference. */
    private void buildParameters() {
        ClassLoader loader = getClass().getClassLoader();

        // _fields
        final LocalizableQueryParameter fieldsParameter = new LocalizableQueryParameter();
        fieldsParameter.setName(PARAMETER_FIELDS);
        fieldsParameter.setType("string");
        fieldsParameter.setCollectionFormat("csv");
        fieldsParameter.description(new LocalizableString(FIELDS_PARAMETER_DESCRIPTION, loader));
        openApi.getComponents().addParameters(fieldsParameter.getName(), fieldsParameter);

        // _prettyPrint
        final LocalizableQueryParameter prettyPrintParameter = new LocalizableQueryParameter();
        prettyPrintParameter.setName(PARAMETER_PRETTY_PRINT);
        prettyPrintParameter.setType("boolean");
        prettyPrintParameter.description(new LocalizableString(PRETTYPRINT_PARAMETER_DESCRIPTION, loader));
        openApi.getComponents().addParameters(prettyPrintParameter.getName(), prettyPrintParameter);

        // _mimeType
        final LocalizableQueryParameter mimeTypeParameter = new LocalizableQueryParameter();
        mimeTypeParameter.setName(PARAMETER_MIME_TYPE);
        mimeTypeParameter.setType("string");
        mimeTypeParameter.description(new LocalizableString(MIMETYPE_PARAMETER_DESCRIPTION, loader));
        openApi.getComponents().addParameters(mimeTypeParameter.getName(), mimeTypeParameter);

        // PUT-operation IF-NONE-MATCH always has * value
        final LocalizableHeaderParameter putIfNoneMatchParameter = new LocalizableHeaderParameter();
        putIfNoneMatchParameter.setName(PARAMETER_IF_NONE_MATCH);
        putIfNoneMatchParameter.setType("string");
        putIfNoneMatchParameter.setRequired(true);
        putIfNoneMatchParameter.setEnum(asList("*"));
        openApi.getComponents().addParameters(PARAMETER_IF_NONE_MATCH_ANY_ONLY, putIfNoneMatchParameter);

        // READ-operation IF-NONE-MATCH cannot have * value
        final LocalizableHeaderParameter readIfNoneMatchParameter = new LocalizableHeaderParameter();
        readIfNoneMatchParameter.setName(PARAMETER_IF_NONE_MATCH);
        readIfNoneMatchParameter.setType("string");
        openApi.getComponents().addParameters(PARAMETER_IF_NONE_MATCH_REV_ONLY, readIfNoneMatchParameter);

        // IF-MATCH
        final LocalizableHeaderParameter ifMatchParameter = new LocalizableHeaderParameter();
        ifMatchParameter.setName(PARAMETER_IF_MATCH);
        ifMatchParameter.setType("string");
        ifMatchParameter.setDefault("*");
        openApi.getComponents().addParameters(ifMatchParameter.getName(), ifMatchParameter);
    }

    /** Traverse CREST API Descriptor paths, to build the OpenAPI model. */
    private void buildPaths() {
        final Paths paths = apiDescription.getPaths();
        if (paths != null) {
            final Map<String, PathItem> pathMap = new LinkedHashMap<>();
            final List<String> pathNames = new ArrayList<>(paths.getNames());
            Collections.sort(pathNames);
            for (final String pathName : pathNames) {
                final VersionedPath versionedPath = paths.get(pathName);
                final List<Version> versions = new ArrayList<>(versionedPath.getVersions());
                Collections.sort(versions);
                for (final Version version : versions) {
                    final String versionName;
                    if (VersionedPath.UNVERSIONED.equals(version)) {
                        versionName = EMPTY_STRING;
                    } else {
                        versionName = version.toString();
                    }

                    final Resource resource = resolveResourceReference(versionedPath.get(version));

                    final String normalizedPathName = pathName.isEmpty() ? "/" : PathUtil.buildPath(pathName);

                    buildResourcePaths(resource, normalizedPathName, null, versionName,
                            Collections.<Parameter>emptyList(), pathMap);
                }
            }
            io.swagger.v3.oas.models.Paths openApiPaths = new io.swagger.v3.oas.models.Paths();
            openApiPaths.putAll(pathMap);
            openApi.setPaths(openApiPaths);
        }
    }

    private Resource resolveResourceReference(Resource resource) {
        Reference resourceReference = resource.getReference();
        if (resourceReference != null) {
            resource = referenceResolver.getService(resourceReference);
            if (resource == null) {
                throw new TransformerException("Unresolvable reference: " + resourceReference.getValue());
            }
        }
        return resource;
    }

    /**
     * Constructs paths, for a given resource, and works with OpenAPI's current inability to overload paths for a
     * given REST operation (e.g., multiple {@code get} operations) by adding a URL-fragment {@code #} suffix
     * to the end of the path.
     *
     * @param resource CREST resource
     * @param pathName Resource path-name
     * @param parentTag Tag for grouping operations together by resource/version or {@code null} if there is no parent
     * @param resourceVersion Resource version-name or empty-string
     * @param parameters CREST operation parameters
     * @param pathMap Output for OpenAPI paths that are constructed
     */
    private void buildResourcePaths(final Resource resource, final String pathName, final LocalizableString parentTag,
            final String resourceVersion, final List<Parameter> parameters, final Map<String, PathItem> pathMap) {
        final boolean hasResourceVersion = !isEmpty(resourceVersion);
        final String pathNamespace = hasResourceVersion
                ? normalizeName(pathName, resourceVersion) : normalizeName(pathName);

        LocalizableString tag = parentTag;
        if (tag == null || isEmpty(tag.toString())) {
            final LocalizableString title = resource.getTitle();
            final String titleString = title.toString();
            tag = new LocalizableString(hasResourceVersion ? titleString + " v" + resourceVersion : titleString) {
                @Override
                public String toTranslatedString(PreferredLocales locales) {
                    String tag = !isEmpty(titleString)
                            ? title.toTranslatedString(locales)
                            : pathName;
                    if (hasResourceVersion) {
                        tag += " v" + resourceVersion;
                    }
                    return tag;
                }
            };
            openApi.addTagsItem(new LocalizableTag().name(tag));
        }

        Schema resourceSchema = null;
        if (resource.getResourceSchema() != null) {
            resourceSchema = resource.getResourceSchema();
        }

        final List<Parameter> operationParameters = unmodifiableList(
                mergeParameters(new ArrayList<>(parameters), resource.getParameters()));

        buildCreate(resource, pathName, pathNamespace, tag, resourceVersion, resourceSchema,
                operationParameters, pathMap);
        buildRead(resource, pathName, pathNamespace, tag, resourceVersion, resourceSchema,
                operationParameters, pathMap);
        buildUpdate(resource, pathName, pathNamespace, tag, resourceVersion, resourceSchema,
                operationParameters, pathMap);
        buildDelete(resource, pathName, pathNamespace, tag, resourceVersion, resourceSchema,
                operationParameters, pathMap);
        buildPatch(resource, pathName, pathNamespace, tag, resourceVersion, resourceSchema,
                operationParameters, pathMap);
        buildActions(resource, pathName, pathNamespace, tag, resourceVersion,
                operationParameters, pathMap);
        buildQueries(resource, pathName, pathNamespace, tag, resourceVersion, resourceSchema,
                operationParameters, pathMap);

        buildItems(resource, pathName, tag, resourceVersion, parameters, pathMap);
        buildSubResources(resource.getSubresources(), pathName, resourceVersion, parameters, pathMap);
    }

    private void buildItems(final Resource resource, final String pathName, final LocalizableString parentTag,
            final String resourceVersion, final List<Parameter> parameters, final Map<String, PathItem> pathMap) {
        if (resource.getItems() != null) {
            final Items items = resource.getItems();

            final Resource itemsResource = items.asResource(resource.isMvccSupported(),
                    resource.getResourceSchema(), resource.getTitle(), resource.getDescription());

            final Parameter pathParameter = items.getPathParameter();
            final List<Parameter> itemsParameters = unmodifiableList(mergeParameters(mergeParameters(
                    new ArrayList<>(parameters), resource.getParameters()), pathParameter));

            final String itemsPath = buildPath(pathName, "/{" + pathParameter.getName() + "}");
            buildSubResources(items.getSubresources(), itemsPath, resourceVersion, itemsParameters, pathMap);
            buildResourcePaths(itemsResource, itemsPath, parentTag, resourceVersion,
                    itemsParameters, pathMap);
        }
    }

    private void buildSubResources(final SubResources subResources, final String pathName,
            final String resourceVersion, final List<Parameter> parameters, final Map<String, PathItem> pathMap) {
        if (subResources != null) {
            final List<String> subPathNames = new ArrayList<>(subResources.getNames());
            Collections.sort(subPathNames);
            for (final String name : subPathNames) {
                final List<Parameter> subresourcesParameters = mergeParameters(new ArrayList<>(parameters),
                        buildPathParameters(name));

                final String subPathName = buildPath(pathName, name);
                Resource subResource = resolveResourceReference(subResources.get(name));
                buildResourcePaths(subResource, subPathName, null, resourceVersion,
                        unmodifiableList(subresourcesParameters), pathMap);
            }
        }
    }

    private void buildCreate(final Resource resource, final String pathName, final String pathNamespace,
            final LocalizableString tag, final String resourceVersion, final Schema resourceSchema,
            final List<Parameter> parameters, final Map<String, PathItem> pathMap) {
        if (resource.getCreate() != null) {
            final Create create = resource.getCreate();
            switch (create.getMode()) {
            case ID_FROM_CLIENT:
                final String createPutNamespace = normalizeName(pathNamespace, "create", "put");
                final String createPutPathFragment = normalizeName(resourceVersion, "create", "put");
                final LocalizableOperation putOperation = buildOperation(create, createPutNamespace, resourceSchema,
                        resourceSchema, parameters);
                putOperation.setSummary("Create with Client-Assigned ID");

                if (resource.isMvccSupported()) {
                    putOperation.addParametersItem(
                            new io.swagger.v3.oas.models.parameters.Parameter()
                                    .$ref("#/components/parameters/" + PARAMETER_IF_NONE_MATCH_ANY_ONLY));
                }

                addOperation(putOperation, "put", pathName, createPutPathFragment, resourceVersion, tag, pathMap);
                break;
            case ID_FROM_SERVER:
                final String createPostNamespace = normalizeName(pathNamespace, "create", "post");
                final String createPostPathFragment = normalizeName(resourceVersion, "create", "post");
                final LocalizableOperation postOperation = buildOperation(create, createPostNamespace, resourceSchema,
                        resourceSchema, parameters);
                postOperation.setSummary("Create with Server-Assigned ID");

                addOperation(postOperation, "post", pathName, createPostPathFragment, resourceVersion, tag,
                        pathMap);
                break;
            default:
                throw new TransformerException("Unsupported CreateMode: " + create.getMode());
            }
        }
    }

    private void buildRead(final Resource resource, final String pathName, final String pathNamespace,
            final LocalizableString tag, final String resourceVersion, final Schema resourceSchema,
            final List<Parameter> parameters, final Map<String, PathItem> pathMap) {
        if (resource.getRead() != null) {
            final String operationNamespace = normalizeName(pathNamespace, "read");
            final String operationPathFragment = normalizeName(resourceVersion, "read");
            final Read read = resource.getRead();

            final LocalizableOperation operation = buildOperation(read, operationNamespace, null, resourceSchema,
                    parameters);
            operation.setSummary("Read");
            operation.addParametersItem(
                    new io.swagger.v3.oas.models.parameters.Parameter()
                            .$ref("#/components/parameters/" + PARAMETER_MIME_TYPE));

            if (resource.isMvccSupported()) {
                operation.addParametersItem(
                        new io.swagger.v3.oas.models.parameters.Parameter()
                                .$ref("#/components/parameters/" + PARAMETER_IF_NONE_MATCH_REV_ONLY));
            }

            addOperation(operation, "get", pathName, operationPathFragment, resourceVersion, tag, pathMap);
        }
    }

    private void buildUpdate(final Resource resource, final String pathName, final String pathNamespace,
            final LocalizableString tag, final String resourceVersion, final Schema resourceSchema,
            final List<Parameter> parameters, final Map<String, PathItem> pathMap) {
        if (resource.getUpdate() != null) {
            final String operationNamespace = normalizeName(pathNamespace, "update");
            final String operationPathFragment = normalizeName(resourceVersion, "update");
            final Update update = resource.getUpdate();

            final LocalizableOperation operation = buildOperation(update, operationNamespace, resourceSchema,
                    resourceSchema, parameters);
            operation.setSummary("Update");

            if (resource.isMvccSupported()) {
                operation.addParametersItem(
                        new io.swagger.v3.oas.models.parameters.Parameter()
                                .$ref("#/components/parameters/" + PARAMETER_IF_MATCH));
            }

            addOperation(operation, "put", pathName, operationPathFragment, resourceVersion, tag, pathMap);
        }
    }

    private void buildDelete(final Resource resource, final String pathName, final String pathNamespace,
            final LocalizableString tag, final String resourceVersion, final Schema resourceSchema,
            final List<Parameter> parameters, final Map<String, PathItem> pathMap) {
        if (resource.getDelete() != null) {
            final String operationNamespace = normalizeName(pathNamespace, "delete");
            final String operationPathFragment = normalizeName(resourceVersion, "delete");
            final Delete delete = resource.getDelete();

            final LocalizableOperation operation = buildOperation(delete, operationNamespace, null, resourceSchema,
                    parameters);
            operation.setSummary("Delete");

            if (resource.isMvccSupported()) {
                operation.addParametersItem(
                        new io.swagger.v3.oas.models.parameters.Parameter()
                                .$ref("#/components/parameters/" + PARAMETER_IF_MATCH));
            }

            addOperation(operation, "delete", pathName, operationPathFragment, resourceVersion, tag, pathMap);
        }
    }

    private void buildPatch(final Resource resource, final String pathName, final String pathNamespace,
            final LocalizableString tag, final String resourceVersion, final Schema resourceSchema,
            final List<Parameter> parameters, final Map<String, PathItem> pathMap) {
        if (resource.getPatch() != null) {
            final String operationNamespace = normalizeName(pathNamespace, "patch");
            final String operationPathFragment = normalizeName(resourceVersion, "patch");
            final Patch patch = resource.getPatch();

            final Schema requestSchema = buildPatchRequestPayload(patch.getOperations());
            final LocalizableOperation operation = buildOperation(patch, operationNamespace, requestSchema,
                    resourceSchema, parameters);
            operation.setSummary("Update via Patch Operations");

            if (resource.isMvccSupported()) {
                operation.addParametersItem(
                        new io.swagger.v3.oas.models.parameters.Parameter()
                                .$ref("#/components/parameters/" + PARAMETER_IF_MATCH));
            }

            addOperation(operation, "patch", pathName, operationPathFragment, resourceVersion, tag, pathMap);
        }
    }

    private void buildActions(final Resource resource, final String pathName, final String pathNamespace,
            final LocalizableString tag, final String resourceVersion, final List<Parameter> parameters,
            final Map<String, PathItem> pathMap) {
        if (!isEmpty(resource.getActions())) {
            final String operationNamespace = normalizeName(pathNamespace, "action");
            final String operationPathFragment = normalizeName(resourceVersion, "action");
            for (final Action action : resource.getActions()) {
                final String actionNamespace = normalizeName(operationNamespace, action.getName());
                final String actionPathFragment = normalizeName(operationPathFragment, action.getName());

                final LocalizableOperation operation = buildOperation(action, actionNamespace, action.getRequest(),
                        action.getResponse(), parameters);
                operation.setSummary("Action: " + action.getName());

                final LocalizableQueryParameter actionParameter = new LocalizableQueryParameter();
                actionParameter.setName("_action");
                actionParameter.setType("string");
                actionParameter.setEnum(asList(action.getName()));
                actionParameter.setRequired(true);
                operation.addParametersItem(actionParameter);

                addOperation(operation, "post", pathName, actionPathFragment, resourceVersion, tag, pathMap);
            }
        }
    }

    private void buildQueries(final Resource resource, final String pathName, final String pathNamespace,
            final LocalizableString tag, final String resourceVersion, final Schema resourceSchema,
            final List<Parameter> parameters, final Map<String, PathItem> pathMap) {
        if (!isEmpty(resource.getQueries())) {
            final String operationNamespace = normalizeName(pathNamespace, "query");
            final String operationPathFragment = normalizeName(resourceVersion, "query");
            for (final Query query : resource.getQueries()) {
                final String queryNamespace;
                final String queryPathFragment;
                final String summary;
                final LocalizableQueryParameter queryParameter;
                switch (query.getType()) {
                case ID:
                    queryNamespace = normalizeName(operationNamespace, "id", query.getQueryId());
                    queryPathFragment = normalizeName(operationPathFragment, "id", query.getQueryId());
                    summary = "Query by ID: " + query.getQueryId();

                    queryParameter = new LocalizableQueryParameter();
                    queryParameter.setName("_queryId");
                    queryParameter.setType("string");
                    queryParameter.setEnum(asList(query.getQueryId()));
                    queryParameter.setRequired(true);
                    break;
                case FILTER:
                    queryNamespace = normalizeName(operationNamespace, "filter");
                    queryPathFragment = normalizeName(operationPathFragment, "filter");
                    summary = "Query by Filter";

                    queryParameter = new LocalizableQueryParameter();
                    queryParameter.setName("_queryFilter");
                    queryParameter.setType("string");
                    queryParameter.setRequired(true);
                    break;
                case EXPRESSION:
                    queryNamespace = normalizeName(operationNamespace, "expression");
                    queryPathFragment = normalizeName(operationPathFragment, "expression");
                    summary = "Query by Expression";

                    queryParameter = new LocalizableQueryParameter();
                    queryParameter.setName("_queryExpression");
                    queryParameter.setType("string");
                    queryParameter.setRequired(true);
                    break;
                default:
                    throw new TransformerException("Unsupported QueryType: " + query.getType());
                }

                final Schema responsePayload;
                if (resourceSchema.getSchema() != null
                        && !"array".equals(getType(resourceSchema.getSchema()))) {
                    responsePayload = Schema.schema().schema(
                            json(object(
                                    field(TYPE, TYPE_OBJECT),
                                    field(TITLE, localizable("common.query.title")),
                                    field(PROPERTIES, object(
                                            field("result", object(
                                                    field(TYPE, TYPE_ARRAY),
                                                    field(DESCRIPTION, localizable(
                                                            "common.query.properties.result")),
                                                    field(ITEMS, resourceSchema.getSchema()))),
                                            field("resultCount", object(
                                                    field(TYPE, TYPE_INTEGER),
                                                    field(DESCRIPTION, localizable(
                                                            "common.query.properties.resultCount")),
                                                    field(DEFAULT, "0"))),
                                            field("pagedResultsCookie", object(
                                                    field(TYPE, array(TYPE_NULL, TYPE_STRING)),
                                                    field(DESCRIPTION, localizable(
                                                            "common.query.properties.pagedResultsCookie")))),
                                            field("totalPagedResultsPolicy", object(
                                                    field(TYPE, TYPE_STRING),
                                                    field(DESCRIPTION, localizable(
                                                            "common.query.properties.totalPagedResultsPolicy")),
                                                    field(DEFAULT, "NONE"))),
                                            field("totalPagedResults", object(
                                                    field(TYPE, TYPE_INTEGER),
                                                    field(DESCRIPTION, localizable(
                                                            "common.query.properties.totalPagedResults")),
                                                    field("default", "-1"))),
                                            field("remainingPagedResults", object(
                                                    field(TYPE, TYPE_INTEGER),
                                                    field(DESCRIPTION, localizable(
                                                            "common.query.properties.remainingPagedResults")),
                                                    field(DEFAULT, "-1"))))))))
                            .build();
                } else {
                    responsePayload = resourceSchema;
                }

                final LocalizableOperation operation = buildOperation(query, queryNamespace, null, responsePayload,
                        parameters);
                operation.setSummary(summary);
                operation.addParametersItem(queryParameter);

                final LocalizableQueryParameter pageSizeParamter = new LocalizableQueryParameter();
                pageSizeParamter.setName("_pageSize");
                pageSizeParamter.setType("integer");
                operation.addParametersItem(pageSizeParamter);

                if (query.getPagingModes() != null) {
                    for (final PagingMode pagingMode : query.getPagingModes()) {
                        final LocalizableQueryParameter parameter = new LocalizableQueryParameter();
                        switch (pagingMode) {
                        case COOKIE:
                            parameter.setName("_pagedResultsCookie");
                            parameter.setType("string");
                            break;
                        case OFFSET:
                            parameter.setName("_pagedResultsOffset");
                            parameter.setType("integer");
                            break;
                        default:
                            throw new TransformerException("Unsupported PagingMode: " + pagingMode);
                        }
                        operation.addParametersItem(parameter);
                    }
                }

                final LocalizableQueryParameter totalPagedResultsPolicyParameter = new LocalizableQueryParameter();
                totalPagedResultsPolicyParameter.setName("_totalPagedResultsPolicy");
                totalPagedResultsPolicyParameter.setType("string");
                final List<String> totalPagedResultsPolicyValues = new ArrayList<>();
                if (query.getCountPolicies() == null || query.getCountPolicies().length == 0) {
                    totalPagedResultsPolicyValues.add(CountPolicy.NONE.name());
                } else {
                    for (final CountPolicy countPolicy : query.getCountPolicies()) {
                        totalPagedResultsPolicyValues.add(countPolicy.name());
                    }
                }
                totalPagedResultsPolicyParameter.setEnum(totalPagedResultsPolicyValues);
                operation.addParametersItem(totalPagedResultsPolicyParameter);

                if (query.getType() != QueryType.ID) {
                    final LocalizableQueryParameter sortKeysParameter = new LocalizableQueryParameter();
                    sortKeysParameter.setName("_sortKeys");
                    sortKeysParameter.setType("string");
                    if (!isEmpty(query.getSupportedSortKeys())) {
                        sortKeysParameter.setEnum(asList(query.getSupportedSortKeys()));
                    }
                    operation.addParametersItem(sortKeysParameter);
                }

                addOperation(operation, "get", pathName, queryPathFragment, resourceVersion, tag, pathMap);
            }
        }
    }

    private static LocalizableString localizable(String value) {
        return new LocalizableString(I18N_PREFIX + value, OpenApiTransformer.class.getClassLoader());
    }

    private LocalizableOperation buildOperation(final org.forgerock.api.models.Operation operationModel,
            final String operationNamespace, final Schema requestPayload, final Schema responsePayload,
            final List<Parameter> parameters) {
        final LocalizableOperation operation = new LocalizableOperation();
        operation.setOperationId(operationNamespace);
        operation.description(operationModel.getDescription());
        applyOperationStability(operationModel.getStability(), operation);
        applyOperationParameters(mergeParameters(new ArrayList<>(parameters), operationModel.getParameters()),
                operation);
        applyOperationRequestPayload(requestPayload, operation);
        applyOperationResponsePayloads(responsePayload, operationModel.getApiErrors(), operationModel, operation);
        return operation;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void addOperation(final LocalizableOperation operation, final String method, final String pathName,
            final String pathFragment, final String resourceVersion, final LocalizableString tag,
            final Map<String, PathItem> pathMap) {
        boolean showPathFragment = false;
        if (!isEmpty(resourceVersion)) {
            showPathFragment = true;
            operation.setVendorExtension("x-resourceVersion", resourceVersion);
            io.swagger.v3.oas.models.parameters.Parameter versionHeader =
                    new io.swagger.v3.oas.models.parameters.Parameter()
                            .in("header")
                            .name(AcceptApiVersionHeader.NAME)
                            .required(true)
                            .schema(new io.swagger.v3.oas.models.media.Schema<String>()
                                    .type("string")
                                    ._enum(singletonList(AcceptApiVersionHeader.RESOURCE + "=" + resourceVersion)));
            operation.addParametersItem(versionHeader);
        }
        if (!isEmpty(tag.toString())) {
            operation.addTag(tag);
        }

        PathItem operationPath = pathMap.get(pathName);
        if (operationPath == null) {
            operationPath = new PathItem();
        } else if (!showPathFragment) {
            switch (method) {
            case "get":
                showPathFragment = operationPath.getGet() != null;
                break;
            case "post":
                showPathFragment = operationPath.getPost() != null;
                break;
            case "put":
                showPathFragment = operationPath.getPut() != null;
                break;
            case "delete":
                showPathFragment = operationPath.getDelete() != null;
                break;
            case "patch":
                showPathFragment = operationPath.getPatch() != null;
                break;
            default:
                throw new TransformerException("Unsupported method: " + method);
            }
        }

        if (showPathFragment) {
            if (pathName.indexOf('#') != -1) {
                throw new TransformerException("pathName cannot contain # character");
            }
            final String uniquePath = pathName + '#' + pathFragment;
            if (pathMap.containsKey(uniquePath)) {
                throw new TransformerException("pathFragment is not unique for given pathName");
            }
            operationPath = new PathItem();
            pathMap.put(uniquePath, operationPath);
        } else {
            pathMap.put(pathName, operationPath);
        }

        if (!setOperationOnPathItem(operationPath, method, operation)) {
            throw new TransformerException("Unsupported method: " + method);
        }
    }

    private boolean setOperationOnPathItem(PathItem pathItem, String method, Operation operation) {
        switch (method) {
        case "get":
            pathItem.setGet(operation);
            return true;
        case "post":
            pathItem.setPost(operation);
            return true;
        case "put":
            pathItem.setPut(operation);
            return true;
        case "delete":
            pathItem.setDelete(operation);
            return true;
        case "patch":
            pathItem.setPatch(operation);
            return true;
        default:
            return false;
        }
    }

    private void applyOperationStability(final Stability stability, final Operation operation) {
        if (stability == Stability.DEPRECATED || stability == Stability.REMOVED) {
            operation.setDeprecated(TRUE);
        }
    }

    private void applyOperationParameters(final List<Parameter> parameters, final Operation operation) {
        if (!parameters.isEmpty()) {
            for (final Parameter parameter : parameters) {
                final LocalizableParameter operationParameter;
                switch (parameter.getSource()) {
                case PATH:
                    operationParameter = new LocalizablePathParameter();
                    break;
                case ADDITIONAL:
                    operationParameter = new LocalizableQueryParameter();
                    break;
                default:
                    throw new TransformerException("Unsupported ParameterSource: " + parameter.getSource());
                }
                operationParameter.setName(parameter.getName());
                operationParameter.setType(parameter.getType());
                operationParameter.description(parameter.getDescription());
                operationParameter.setRequired(ValidationUtil.nullToFalse(parameter.isRequired()));
                if (!isEmpty(parameter.getEnumValues())) {
                    operationParameter.setEnum(asList(parameter.getEnumValues()));

                    if (!isEmpty(parameter.getEnumTitles())) {
                        operationParameter.addExtension("x-enum_titles",
                                asList(parameter.getEnumTitles()));
                    }
                }
                operation.addParametersItem(operationParameter);
            }
        }

        // apply common parameters
        operation.addParametersItem(
                new io.swagger.v3.oas.models.parameters.Parameter()
                        .$ref("#/components/parameters/" + PARAMETER_FIELDS));
        operation.addParametersItem(
                new io.swagger.v3.oas.models.parameters.Parameter()
                        .$ref("#/components/parameters/" + PARAMETER_PRETTY_PRINT));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void applyOperationRequestPayload(final Schema schema, final Operation operation) {
        if (schema != null) {
            final io.swagger.v3.oas.models.media.Schema mediaSchema;
            if (schema.getSchema() != null) {
                if (hasReferenceableId(schema.getSchema())) {
                    final String name = addDefinitionReference(schema.getSchema(), buildSchema(schema.getSchema()));
                    mediaSchema = new io.swagger.v3.oas.models.media.Schema().$ref(SCHEMAS_REF_PREFIX + name);
                } else {
                    mediaSchema = buildSchema(schema.getSchema());
                }
            } else {
                final String ref = getDefinitionsReference(schema.getReference());
                if (ref == null) {
                    throw new TransformerException("Invalid JSON ref");
                }
                mediaSchema = new io.swagger.v3.oas.models.media.Schema().$ref(SCHEMAS_REF_PREFIX + ref);
            }
            final LocalizableRequestBody requestBody = new LocalizableRequestBody();
            requestBody.setRequired(true);
            requestBody.setContent(new Content().addMediaType("application/json",
                    new MediaType().schema(mediaSchema)));
            operation.setRequestBody(requestBody);
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void applyOperationResponsePayloads(final Schema schema, final ApiError[] apiErrorResponses,
            final org.forgerock.api.models.Operation operationModel, final Operation operation) {
        final ApiResponses responses = new ApiResponses();
        if (schema != null) {
            final ApiResponse response = new ApiResponse();
            response.description("Success");
            io.swagger.v3.oas.models.media.Schema responseSchema;
            if (schema.getSchema() != null) {
                final io.swagger.v3.oas.models.media.Schema builtSchema = buildSchema(schema.getSchema());
                String name = addDefinitionReference(schema.getSchema(), builtSchema);
                if (name == null) {
                    name = "urn:uuid:" + UUID.randomUUID();
                    definitionMap.put(name, builtSchema);
                }
                responseSchema = new io.swagger.v3.oas.models.media.Schema().$ref(SCHEMAS_REF_PREFIX + name);
            } else {
                final String ref = getDefinitionsReference(schema.getReference());
                if (ref == null) {
                    throw new TransformerException("Invalid JSON ref");
                }
                responseSchema = new io.swagger.v3.oas.models.media.Schema().$ref(SCHEMAS_REF_PREFIX + ref);
            }
            response.content(new Content().addMediaType("application/json",
                    new MediaType().schema(responseSchema)));
            if (operationModel instanceof Create) {
                LocalizableSchema locSchema = new LocalizableSchema();
                locSchema.type("string");
                locSchema.description(locationDescription);
                response.addHeaderObject(PARAMETER_LOCATION,
                        new Header().schema(locSchema));
                responses.addApiResponse("201", response);
            } else {
                responses.addApiResponse("200", response);
            }
        }

        if (!isEmpty(apiErrorResponses)) {
            final List<ApiError> resolvedErrors = new ArrayList<>(apiErrorResponses.length);
            for (final ApiError error : apiErrorResponses) {
                resolvedErrors.add(resolveErrorReference(error));
            }
            Collections.sort(resolvedErrors, ApiError.ERROR_COMPARATOR);

            final int n = resolvedErrors.size();
            for (int i = 0; i < n; ++i) {
                final ApiError apiError = resolvedErrors.get(i);

                final int code = apiError.getCode();
                final List<LocalizableString> descriptions = new ArrayList<>();
                if (apiError.getDescription() != null) {
                    descriptions.add(apiError.getDescription());
                }
                for (int k = i + 1; k < n; ++k) {
                    final ApiError error = resolvedErrors.get(k);
                    if (error.getCode() == code) {
                        if (error.getDescription() != null) {
                            descriptions.add(error.getDescription());
                        }
                        ++i;
                    }
                }

                final LocalizableResponse response = new LocalizableResponse();
                if (descriptions.size() == 1) {
                    response.description(descriptions.get(0));
                } else if (!descriptions.isEmpty()) {
                    response.description(new LocalizableString("Aggregated bullet description list") {
                        @Override
                        public String toTranslatedString(PreferredLocales locales) {
                            final AsciiDoc bulletedList = AsciiDoc.asciiDoc();
                            for (final LocalizableString listItem : descriptions) {
                                bulletedList.unorderedList1(listItem.toTranslatedString(locales));
                            }
                            return bulletedList.toString();
                        }
                    });
                }

                final JsonValue errorSchema = buildErrorSchema(apiError);
                final io.swagger.v3.oas.models.media.Schema builtSchema = buildSchema(errorSchema);
                final String name = addDefinitionReference(errorSchema, builtSchema);
                io.swagger.v3.oas.models.media.Schema refSchema =
                        new io.swagger.v3.oas.models.media.Schema().$ref(SCHEMAS_REF_PREFIX + name);
                response.content(new Content().addMediaType("application/json",
                        new MediaType().schema(refSchema)));

                responses.addApiResponse(String.valueOf(code), response);
            }
        }
        operation.setResponses(responses);
    }

    JsonValue buildErrorSchema(final ApiError apiError) {
        String id = FRAPI_PREFIX + "models:ApiError";
        JsonValue errorCauseSchema = null;
        final Schema schema = apiError.getSchema();
        if (schema != null && schema.getSchema().isNotNull()) {
            errorCauseSchema = schema.getSchema();
            id += ':' + urnSafeHash(errorCauseSchema.toString());
        }

        return json(object(
                field(ID, id),
                field(TYPE, TYPE_OBJECT),
                field(REQUIRED, array("code", "message")),
                field(TITLE, localizable("common.error.title")),
                field(PROPERTIES, object(
                        field("code", object(
                                field(TYPE, TYPE_INTEGER),
                                field(DESCRIPTION, localizable("common.error.properties.code"))
                        )),
                        field("message", object(
                                field(TYPE, TYPE_STRING),
                                field(DESCRIPTION, localizable("common.error.properties.message"))
                        )),
                        field("reason", object(
                                field(TYPE, TYPE_STRING),
                                field(DESCRIPTION, localizable("common.error.properties.reason"))
                        )),
                        field("detail", object(
                                field(TYPE, TYPE_STRING),
                                field(DESCRIPTION, localizable("common.error.properties.detail"))
                        )),
                        fieldIfNotNull("cause", errorCauseSchema)
                ))
        ));
    }

    private ApiError resolveErrorReference(ApiError apiError) {
        if (apiError.getReference() != null) {
            apiError = referenceResolver.getError(apiError.getReference());
            if (apiError == null) {
                throw new TransformerException("Error reference not found in global error definitions");
            }
        }
        return apiError;
    }

    @VisibleForTesting
    Schema buildPatchRequestPayload(final PatchOperation[] patchOperations) {
        final List<String> enumArray = new ArrayList<>(patchOperations.length);
        for (final PatchOperation op : patchOperations) {
            enumArray.add(op.name().toLowerCase(Locale.ROOT));
        }

        Collections.sort(enumArray);
        final String operations = Joiner.on("_").join(enumArray);
        final String id = FRAPI_PREFIX + "models:Patch:" + operations;

        final JsonValue schemaValue = json(object(
                field(ID, id),
                field(TITLE, localizable("common.patch.title")),
                field(TYPE, TYPE_ARRAY),
                field(ITEMS, object(
                        field(TITLE, localizable("common.patch.items.title")),
                        field(TYPE, TYPE_OBJECT),
                        field(PROPERTIES, object(
                                field("operation", object(
                                        field(TYPE, TYPE_STRING),
                                        field(ENUM, enumArray),
                                        field(DESCRIPTION, localizable("common.patch.items.properties.operation")),
                                        field(REQUIRED, true))),
                                field("field", object(
                                        field(DESCRIPTION, localizable("common.patch.items.properties.field")),
                                        field(TYPE, TYPE_STRING))),
                                field("from", object(
                                        field(DESCRIPTION, localizable("common.patch.items.properties.from")),
                                        field(TYPE, TYPE_STRING))),
                                field("value", object(
                                        field(DESCRIPTION, localizable("common.patch.items.properties.value")),
                                        field(TYPE, TYPE_STRING)))
                        ))
                ))
        ));
        return Schema.schema().schema(schemaValue).build();
    }

    @VisibleForTesting
    Info buildInfo(final LocalizableString title) {
        return new LocalizableInfo()
            .title(title != null ? title : new LocalizableString(apiDescription.getId()))
            .description(apiDescription.getDescription())
            .version(apiDescription.getVersion());
    }

    @VisibleForTesting
    @SuppressWarnings("rawtypes")
    void buildDefinitions() {
        final Definitions definitions = apiDescription.getDefinitions();
        if (definitions != null) {
            final Set<String> definitionNames = definitions.getNames();
            for (final String name : definitionNames) {
                final Schema schema = definitions.get(name);
                if (schema.getSchema() != null) {
                    definitionMap.put(name, buildSchema(schema.getSchema()));
                }
            }
        }

        if (!definitionMap.isEmpty()) {
            Components components = openApi.getComponents();
            if (components == null) {
                components = new Components();
                openApi.setComponents(components);
            }
            components.setSchemas(definitionMap);
        }
    }

    /**
     * Converts a JSON schema into the appropriate OpenAPI 3.0 Schema.
     *
     * @param schema JSON schema
     * @return OpenAPI Schema
     */
    @VisibleForTesting
    @SuppressWarnings({ "unchecked", "rawtypes" })
    io.swagger.v3.oas.models.media.Schema buildSchema(final JsonValue schema) {
        final String type = getType(schema);
        if (type == null) {
            if (schema.isDefined("allOf")) {
                return buildAllOfSchema(schema);
            } else if (schema.isDefined("$ref")) {
                return buildReferenceSchema(schema);
            }
            throw new TransformerException(unsupportedJsonSchema(schema));
        }
        switch (type) {
        case "object":
            return buildObjectSchema(schema);
        case "array":
            return buildArraySchema(schema);
        case "any":
        case "null":
            return new LocalizableSchema().type(type);
        case "boolean":
        case "integer":
        case "number":
        case "string":
            return buildScalarSchema(schema, type);
        default:
            throw new TransformerException("Unsupported JSON Schema type '" + type + "' in schema " + schema);
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private io.swagger.v3.oas.models.media.Schema buildAllOfSchema(final JsonValue schema) {
        final List<io.swagger.v3.oas.models.media.Schema> allOf = schema.get("allOf")
                .as(listOf(schemaMapper()));
        if (allOf == null || allOf.isEmpty()) {
            throw new TransformerException(unsupportedJsonSchema(schema));
        }
        final LocalizableSchema result = new LocalizableSchema();
        setTitleAndDescriptionFromSchema(result, schema);
        result.allOf(allOf);
        return result;
    }

    private String unsupportedJsonSchema(final JsonValue schema) {
        return "Unsupported JSON schema: expected 'type', '$ref' or non-empty 'allOf' property in: '" + schema + "'";
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private io.swagger.v3.oas.models.media.Schema buildReferenceSchema(JsonValue schema) {
        final LocalizableSchema result = new LocalizableSchema();
        setTitleAndDescriptionFromSchema(result, schema);
        result.set$ref(schema.get("$ref").asString());
        result.setProperties(buildSchemaProperties(schema));
        return result;
    }

    @SuppressWarnings("rawtypes")
    private Function<JsonValue, io.swagger.v3.oas.models.media.Schema, JsonValueException> schemaMapper() {
        return new Function<JsonValue, io.swagger.v3.oas.models.media.Schema, JsonValueException>() {
            @Override
            public io.swagger.v3.oas.models.media.Schema apply(JsonValue value) throws JsonValueException {
                return buildSchema(value);
            }
        };
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private io.swagger.v3.oas.models.media.Schema buildObjectSchema(final JsonValue schema) {
        final LocalizableSchema result = new LocalizableSchema();
        result.type("object");
        result.setDiscriminator(schema.get("discriminator").asString() != null
                ? new io.swagger.v3.oas.models.media.Discriminator()
                        .propertyName(schema.get("discriminator").asString())
                : null);
        result.setProperties(buildSchemaProperties(schema));
        final List<String> required = getArrayOfJsonString("required", schema);
        if (!required.isEmpty()) {
            result.setRequired(required);
        }
        io.swagger.v3.oas.models.media.Schema additionalProps = buildSchemaFromJson(schema.get("additionalProperties"));
        if (additionalProps != null) {
            result.setAdditionalProperties(additionalProps);
        }
        setTitleAndDescriptionFromSchema(result, schema);
        return result;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private LocalizableSchema buildScalarSchema(final JsonValue schema, final String type) {
        final LocalizableSchema result = new LocalizableSchema();
        result.type(type);
        setTitleAndDescriptionFromSchema(result, schema);
        if (schema.get("default").isNotNull()) {
            result.setDefault(schema.get("default").asString());
        }

        final List<String> enumValues = getArrayOfJsonString("enum", schema);
        if (!enumValues.isEmpty()) {
            result.setEnum(enumValues);

            final JsonValue options = schema.get("options");
            if (options.isNotNull()) {
                final List<String> enumTitles = getArrayOfJsonString("enum_titles", options);
                if (!enumTitles.isEmpty()) {
                    result.addExtension("x-enum_titles", enumTitles);
                }
            }
        }

        if (schema.get("format").isNotNull()) {
            result.setFormat(schema.get("format").asString());
            if ("full-date".equals(result.getFormat()) && "string".equals(type)) {
                result.setFormat("date");
            }
        }

        return result;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private io.swagger.v3.oas.models.media.Schema buildArraySchema(final JsonValue schema) {
        final LocalizableSchema result = new LocalizableSchema();
        result.type("array");
        setTitleAndDescriptionFromSchema(result, schema);
        result.setProperties(buildSchemaProperties(schema));
        result.setItems(buildItemsSchema(schema));
        return result;
    }

    /**
     * Convert JSON schema-properties into a map of Schema objects.
     */
    @VisibleForTesting
    @SuppressWarnings({ "unchecked", "rawtypes" })
    Map<String, io.swagger.v3.oas.models.media.Schema> buildSchemaProperties(final JsonValue schema) {
        if (schema != null && schema.isNotNull()) {
            final JsonValue properties = schema.get("properties");
            if (properties.isNotNull()) {
                final Map<String, Object> propertiesMap = properties.asMap();
                final Map<String, io.swagger.v3.oas.models.media.Schema> resultMap =
                        new LinkedHashMap<>(propertiesMap.size() * 2);

                boolean sortByPropertyOrder = false;
                for (final Map.Entry<String, Object> entry : propertiesMap.entrySet()) {
                    final io.swagger.v3.oas.models.media.Schema propSchema;
                    try {
                        propSchema = buildSchemaFromJson(json(entry.getValue()));
                    } catch (RuntimeException re) {
                        logger.info("Json schema error: " + entry.getValue() + "\n"
                                + re.getMessage(), re.fillInStackTrace());
                        throw re;
                    }
                    if (propSchema != null && propSchema.getExtensions() != null
                            && propSchema.getExtensions().containsKey("x-propertyOrder")) {
                        sortByPropertyOrder = true;
                    }
                    resultMap.put(entry.getKey(), propSchema);
                }

                if (sortByPropertyOrder && resultMap.size() > 1) {
                    final List<Map.Entry<String, io.swagger.v3.oas.models.media.Schema>> entries =
                            new ArrayList<>(resultMap.entrySet());
                    Collections.sort(entries,
                            new Comparator<Map.Entry<String, io.swagger.v3.oas.models.media.Schema>>() {
                        @Override
                        public int compare(final Map.Entry<String, io.swagger.v3.oas.models.media.Schema> o1,
                                final Map.Entry<String, io.swagger.v3.oas.models.media.Schema> o2) {
                            final Integer v1 = o1.getValue().getExtensions() != null
                                    ? (Integer) o1.getValue().getExtensions().get("x-propertyOrder") : null;
                            final Integer v2 = o2.getValue().getExtensions() != null
                                    ? (Integer) o2.getValue().getExtensions().get("x-propertyOrder") : null;
                            if (v1 != null) {
                                if (v2 != null) {
                                    return v1.compareTo(v2);
                                }
                                return -1;
                            }
                            if (v2 != null) {
                                return 1;
                            }
                            return 0;
                        }
                    });

                    final Map<String, io.swagger.v3.oas.models.media.Schema> sortedMap =
                            new LinkedHashMap<>(propertiesMap.size() * 2);
                    for (final Map.Entry<String, io.swagger.v3.oas.models.media.Schema> entry : entries) {
                        sortedMap.put(entry.getKey(), entry.getValue());
                    }
                    return sortedMap;
                } else {
                    return resultMap;
                }
            }
        }
        return null;
    }

    /**
     * Builds an OpenAPI Schema from a JSON Schema definition.
     */
    @VisibleForTesting
    @SuppressWarnings({ "unchecked", "rawtypes" })
    io.swagger.v3.oas.models.media.Schema buildSchemaFromJson(final JsonValue schema) {
        if (schema == null || schema.isNull()) {
            return null;
        }

        final String format = schema.get("format").asString();
        final String type = getType(schema);

        // Handle $ref
        if (schema.get("$ref").isNotNull()) {
            final String ref = getDefinitionsReference(schema.get("$ref").asString());
            if (ref == null) {
                throw new TransformerException("Invalid JSON ref: " + schema.get("$ref").asString());
            }
            LocalizableSchema result = new LocalizableSchema();
            result.set$ref(SCHEMAS_REF_PREFIX + ref);
            setTitleAndDescriptionFromSchema(result, schema);
            return result;
        }

        if (type == null) {
            return null;
        }

        LocalizableSchema result = new LocalizableSchema();

        switch (type) {
        case "any":
        case "object": {
            if (hasReferenceableId(schema)) {
                final io.swagger.v3.oas.models.media.Schema model = buildObjectSchema(schema);
                final String name = addDefinitionReference(schema, model);
                final LocalizableSchema refSchema = new LocalizableSchema();
                refSchema.$ref(SCHEMAS_REF_PREFIX + name);
                setTitleAndDescriptionFromSchema(refSchema, schema);
                return refSchema;
            } else {
                result.type(type);
                result.setProperties(buildSchemaProperties(schema));
                final List<String> required = getArrayOfJsonString("required", schema);
                if (!required.isEmpty()) {
                    result.setRequired(required);
                }
                if (schema.get("default").isNotNull()) {
                    result.setDefault(schema.get("default").getObject());
                }
            }
            break;
        }
        case "array": {
            result.type("array");
            result.setItems(buildItemsSchema(schema));
            if (schema.get("minItems").isNotNull()) {
                result.setMinItems(schema.get("minItems").asInteger());
            }
            if (schema.get("maxItems").isNotNull()) {
                result.setMaxItems(schema.get("maxItems").asInteger());
            }
            if (schema.get("uniqueItems").isNotNull()) {
                result.setUniqueItems(schema.get("uniqueItems").asBoolean());
            }
            if (schema.get("default").isNotNull()) {
                result.setDefault(schema.get("default").asList());
            }
            break;
        }
        case "boolean":
            result.type("boolean");
            break;
        case "integer":
        case "number": {
            result.type(type);
            if (schema.get("minimum").isNotNull()) {
                result.setMinimum(BigDecimalUtil.safeValueOf(schema.get("minimum").asDouble()));
            }
            if (schema.get("maximum").isNotNull()) {
                result.setMaximum(BigDecimalUtil.safeValueOf(schema.get("maximum").asDouble()));
            }
            if (schema.get("exclusiveMinimum").isNotNull()) {
                result.setExclusiveMinimum(schema.get("exclusiveMinimum").asBoolean());
            }
            if (schema.get("exclusiveMaximum").isNotNull()) {
                result.setExclusiveMaximum(schema.get("exclusiveMaximum").asBoolean());
            }
            break;
        }
        case "null":
            return null;
        case "string": {
            result.type("string");
            if (schema.get("minLength").isNotNull()) {
                result.setMinLength(schema.get("minLength").asInteger());
            }
            if (schema.get("maxLength").isNotNull()) {
                result.setMaxLength(schema.get("maxLength").asInteger());
            }
            if (schema.get("pattern").isNotNull()) {
                result.setPattern(schema.get("pattern").asString());
            }
            break;
        }
        default:
            throw new TransformerException("Unsupported JSON schema type: " + type);
        }

        if (!isEmpty(format)) {
            result.setFormat(format);
            if ("full-date".equals(format) && "string".equals(type)) {
                result.setFormat("date");
            }
        }
        if (!"object".equals(type) && !"array".equals(type) && schema.get("default").isNotNull()) {
            result.setDefault(schema.get("default").getObject().toString());
        }
        setTitleAndDescriptionFromSchema(result, schema);

        final String readPolicy = schema.get("readPolicy").asString();
        if (!isEmpty(readPolicy)) {
            result.addExtension("x-readPolicy", readPolicy);
        }
        if (schema.get("returnOnDemand").isNotNull()) {
            result.addExtension("x-returnOnDemand", schema.get("returnOnDemand").asBoolean());
        }

        final Boolean readOnly = schema.get("readOnly").asBoolean();
        if (TRUE.equals(readOnly)) {
            result.setReadOnly(TRUE);
        } else {
            final String writePolicy = schema.get("writePolicy").asString();
            if (!isEmpty(writePolicy)) {
                result.addExtension("x-writePolicy", writePolicy);
                if (schema.get("errorOnWritePolicyFailure").isNotNull()) {
                    result.addExtension("x-errorOnWritePolicyFailure",
                            schema.get("errorOnWritePolicyFailure").asBoolean());
                }
            }
        }

        final Integer propertyOrder = schema.get("propertyOrder").asInteger();
        if (propertyOrder != null) {
            result.addExtension("x-propertyOrder", propertyOrder);
        }

        // Enum values
        final List<String> enumValues = getArrayOfJsonString("enum", schema);
        if (!enumValues.isEmpty()) {
            result.setEnum(enumValues);
            final JsonValue options = schema.get("options");
            if (options.isNotNull()) {
                final List<String> enumTitles = getArrayOfJsonString("enum_titles", options);
                if (!enumTitles.isEmpty()) {
                    result.addExtension("x-enum_titles", enumTitles);
                }
            }
        }

        return result;
    }

    @SuppressWarnings("rawtypes")
    private io.swagger.v3.oas.models.media.Schema buildItemsSchema(final JsonValue schema) {
        if (!schema.isDefined("items")) {
            final LocalizableSchema result = new LocalizableSchema();
            result.type("any");
            return result;
        }
        final JsonValue items = schema.get("items");
        if (items.isNull()) {
            throw new TransformerException("JSON-array 'items' field cannot be null: " + schema);
        }
        return buildSchemaFromJson(items);
    }

    private List<String> getArrayOfJsonString(final String field, final JsonValue schema) {
        final JsonValue value = schema.get(field);
        if (value.isNotNull() && value.isCollection()) {
            return value.asList(String.class);
        }
        return Collections.emptyList();
    }

    private String getType(final JsonValue schema) {
        final JsonValue value = schema.get("type");
        if (value.isList()) {
            final List<String> list = value.asList(String.class);
            list.remove("null");
            if (list.size() == 1) {
                return list.get(0);
            }
            logger.trace("Simplifying array of types {} to 'any' type", value);
            return "any";
        }
        return value.asString();
    }

    private boolean hasReferenceableId(final JsonValue schema) {
        return isReferenceableId(schema.get("id").asString());
    }

    private boolean isReferenceableId(final String id) {
        return id != null && (id.startsWith(URN_JSONSCHEMA_PREFIX) || id.startsWith(FRAPI_PREFIX));
    }

    @VisibleForTesting
    @SuppressWarnings("rawtypes")
    String addDefinitionReference(final JsonValue schema, final io.swagger.v3.oas.models.media.Schema model) {
        if (hasReferenceableId(schema)) {
            final String id = schema.get("id").asString();
            final io.swagger.v3.oas.models.media.Schema existingModel = definitionMap.put(id, model);
            if (existingModel != null && !existingModel.equals(model)) {
                logger.info("Replacing schema definition with id: " + id);
            }
            return id;
        }
        return null;
    }

    @VisibleForTesting
    String getDefinitionsReference(final Reference reference) {
        if (reference != null) {
            return getDefinitionsReference(reference.getValue());
        }
        return null;
    }

    @VisibleForTesting
    String getDefinitionsReference(final String reference) {
        if (!isEmpty(reference)) {
            if (isReferenceableId(reference)) {
                return reference;
            }
            // Support both old-style #/definitions/ and new-style #/components/schemas/ references
            final String oldPrefix = "#/definitions/";
            int start = reference.indexOf(oldPrefix);
            if (start != -1) {
                final String s = reference.substring(start + oldPrefix.length());
                if (!s.isEmpty()) {
                    return s;
                }
            }
            start = reference.indexOf(SCHEMAS_REF_PREFIX);
            if (start != -1) {
                final String s = reference.substring(start + SCHEMAS_REF_PREFIX.length());
                if (!s.isEmpty()) {
                    return s;
                }
            }
        }
        return null;
    }

    private void setTitleAndDescriptionFromSchema(LocalizableTitleAndDescription<?> model, JsonValue schema) {
        setTitleFromJsonValue(model, schema.get("title"));
        setDescriptionFromJsonValue(model, schema.get("description"));
    }

    static void setTitleFromJsonValue(LocalizableTitleAndDescription<?> model, JsonValue source) {
        if (source.isString()) {
            model.title(source.asString());
        } else {
            model.title((LocalizableString) source.getObject());
        }
    }

    static void setDescriptionFromJsonValue(LocalizableTitleAndDescription<?> model, JsonValue source) {
        if (source.isString()) {
            model.description(source.asString());
        } else {
            model.description((LocalizableString) source.getObject());
        }
    }

    private static String urnSafeHash(final String s) {
        return BaseEncoding.base64Url().encode(Hashing.sha1().hashString(s, UTF_8).asBytes());
    }
}
