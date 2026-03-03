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

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.atIndex;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;
import static org.forgerock.api.models.Paths.paths;
import static org.forgerock.api.models.Query.query;
import static org.forgerock.api.models.Reference.reference;
import static org.forgerock.api.models.Resource.resource;
import static org.forgerock.api.models.Schema.schema;
import static org.forgerock.api.models.VersionedPath.versionedPath;
import static org.forgerock.json.JsonValue.array;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.module.jsonSchema.jakarta.JsonSchema;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.parameters.Parameter;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.Condition;
import org.assertj.core.api.iterable.Extractor;
import org.forgerock.api.ApiTestUtil;
import org.forgerock.api.enums.CountPolicy;
import org.forgerock.api.enums.PatchOperation;
import org.forgerock.api.enums.QueryType;
import org.forgerock.api.enums.ReadPolicy;
import org.forgerock.api.enums.WritePolicy;
import org.forgerock.api.jackson.JacksonUtils;
import org.forgerock.api.models.ApiDescription;
import org.forgerock.api.models.ApiError;
import org.forgerock.api.models.Definitions;
import org.forgerock.api.models.Reference;
import org.forgerock.api.models.Resource;
import org.forgerock.api.models.Schema;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.util.i18n.LocalizableString;
import org.forgerock.util.i18n.PreferredLocales;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

@SuppressWarnings("javadoc")
public class OpenApiTransformerTest {

    public static final PreferredLocales PREFERRED_LOCALES = new PreferredLocales();

    @Test
    public void testUserAndDevicesExample() throws Exception {
        final ApiDescription apiDescription = ApiTestUtil.createUserAndDeviceExampleApiDescription();
        final OpenAPI openApi = OpenApiTransformer.execute(apiDescription);

        assertThat(openApi.getTags()).hasSize(2);
        assertTag(openApi, 0, "User Service v1.0");
        assertTag(openApi, 1, "User-Device Service v1.0");
        assertThat(openApi.getPaths()).containsOnlyKeys(
                "/admins#1.0_create_post",
                "/admins#1.0_query_filter",
                "/admins/{userId}#1.0_create_put",
                "/admins/{userId}#1.0_read",
                "/admins/{userId}#1.0_update",
                "/admins/{userId}#1.0_delete",
                "/admins/{userId}#1.0_patch",
                "/admins/{userId}#1.0_action_resetpassword",
                "/admins/{userId}/devices#1.0_create_post",
                "/admins/{userId}/devices#1.0_query_filter",
                "/admins/{userId}/devices/{deviceId}#1.0_create_put",
                "/admins/{userId}/devices/{deviceId}#1.0_read",
                "/admins/{userId}/devices/{deviceId}#1.0_update",
                "/admins/{userId}/devices/{deviceId}#1.0_delete",
                "/admins/{userId}/devices/{deviceId}#1.0_patch",
                "/admins/{userId}/devices/{deviceId}#1.0_action_markasstolen",
                "/users#1.0_create_post",
                "/users#1.0_query_filter",
                "/users/{userId}#1.0_create_put",
                "/users/{userId}#1.0_read",
                "/users/{userId}#1.0_update",
                "/users/{userId}#1.0_delete",
                "/users/{userId}#1.0_patch",
                "/users/{userId}#1.0_action_resetpassword",
                "/users/{userId}/devices#1.0_create_post",
                "/users/{userId}/devices#1.0_query_filter",
                "/users/{userId}/devices/{deviceId}#1.0_create_put",
                "/users/{userId}/devices/{deviceId}#1.0_read",
                "/users/{userId}/devices/{deviceId}#1.0_update",
                "/users/{userId}/devices/{deviceId}#1.0_delete",
                "/users/{userId}/devices/{deviceId}#1.0_patch",
                "/users/{userId}/devices/{deviceId}#1.0_action_markasstolen");
    }

    @Test
    public void testTransformWithUnversionedPaths() throws Exception {
        final ApiDescription apiDescription = ApiTestUtil.createApiDescription(false);
        final OpenAPI openApi = OpenApiTransformer.execute(apiDescription);

        assertThat(openApi.getTags()).hasSize(1);
        assertTag(openApi, 0, "Resource title");
        assertThat(openApi.getPaths()).containsOnlyKeys(
                "/testPath",
                "/testPath#_action_action1",
                "/testPath#_query_expression",
                "/testPath#_query_filter",
                "/testPath#_query_id_id1",
                "/testPath#_query_id_id2");
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void testTransformWithVersionedPaths() throws Exception {
        final ApiDescription apiDescription = ApiTestUtil.createApiDescription(true);
        final OpenAPI openApi = OpenApiTransformer.execute(apiDescription);

        // decorate OpenAPI object with application-specific features like auth headers, after this class completes
        final Parameter usernameHeader = new Parameter()
                .in("header")
                .name("X-OpenAM-Username")
                .schema(new io.swagger.v3.oas.models.media.Schema<String>()
                        .type("string")._default("openam-admin"))
                .required(true);
        OpenApiHelper.addHeaderToAllOperations(usernameHeader, openApi);

        final Parameter passwordHeader = new Parameter()
                .in("header")
                .name("X-OpenAM-Password")
                .schema(new io.swagger.v3.oas.models.media.Schema<String>()
                        .type("string")._default("openam-admin"))
                .required(true);
        OpenApiHelper.addHeaderToAllOperations(passwordHeader, openApi);

        OpenApiHelper.visitAllOperations(
                new OpenApiHelper.OperationVisitor() {
                    @Override
                    public void visit(final Operation operation) {
                        // add header "Accept-API-Version: resource=XXX, protocol=1.0"
                        final String version = (String) operation.getExtensions().get("x-resourceVersion");
                        assertThat(version).isIn("1.0", "2.0");
                        assertThat(operation.getParameters()).areAtLeastOne(new Condition<Parameter>() {
                            @Override
                            public boolean matches(Parameter parameter) {
                                if (!"header".equals(parameter.getIn())) {
                                    return false;
                                }
                                if (parameter.getSchema() == null || parameter.getSchema().getEnum() == null) {
                                    return false;
                                }
                                assertThat(parameter.getSchema().getEnum())
                                        .containsOnly("resource=" + version);
                                return true;
                            }
                        });
                    }
                }, openApi);

        assertThat(openApi.getTags()).hasSize(2);
        assertTag(openApi, 0, "Resource title v1.0");
        assertTag(openApi, 1, "Resource title v2.0");

        assertThat(openApi.getPaths()).containsOnlyKeys(
                "/testPath#1.0_create_post",
                "/testPath#1.0_read",
                "/testPath#1.0_update",
                "/testPath#1.0_delete",
                "/testPath#1.0_patch",
                "/testPath#1.0_action_action1",
                "/testPath#1.0_query_expression",
                "/testPath#1.0_query_filter",
                "/testPath#1.0_query_id_id1",
                "/testPath#1.0_query_id_id2",
                "/testPath#2.0_create_put",
                "/testPath#2.0_read",
                "/testPath#2.0_update",
                "/testPath#2.0_delete",
                "/testPath#2.0_patch",
                "/testPath#2.0_action_action1",
                "/testPath#2.0_action_action2",
                "/testPath#2.0_query_expression",
                "/testPath#2.0_query_filter",
                "/testPath#2.0_query_id_id1",
                "/testPath#2.0_query_id_id2");
    }

    private void assertTag(OpenAPI openApi, int tagNumber, String expected) {
        assertThat(openApi.getTags().get(tagNumber)).isInstanceOf(LocalizableTag.class);
        LocalizableTag tag = (LocalizableTag) openApi.getTags().get(tagNumber);
        assertThat(tag.getLocalizableName().toTranslatedString(PREFERRED_LOCALES)).isEqualTo(expected);
    }

    @Test
    public void testBuildPatchRequestPayload() {
        final OpenApiTransformer transformer = new OpenApiTransformer();
        final Schema schema = transformer.buildPatchRequestPayload(
                new PatchOperation[]{PatchOperation.ADD, PatchOperation.REMOVE});

        final List<Object> enumList = schema.getSchema().get(
                new JsonPointer("/items/properties/operation/enum")).asList();
        assertThat(enumList).containsOnly("add", "remove");
        assertThat(schema.getSchema().get("id").asString())
                .isEqualTo("frapi:models:Patch:add_remove");
    }

    @Test
    public void testBuildInfo() {
        final ApiDescription apiDescription = ApiDescription.apiDescription()
                .id("frapi:test")
                .version("2.0")
                .description(new LocalizableString("My Description"))
                .build();
        final OpenApiTransformer transformer = new OpenApiTransformer(new LocalizableString("Test"), "localhost:8080",
                "/", false, apiDescription);

        final Info info = transformer.buildInfo(new LocalizableString("My Title"));

        assertThat(info).isEqualTo(new LocalizableInfo()
                .title(new LocalizableString("My Title"))
                .description(new LocalizableString("My Description"))
                .version("2.0"));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void testBuildDefinitions() {
        final Definitions definitions = Definitions.definitions()
                .put("myDef", schema().schema(json(object(field("type", "object")))).build())
                .build();
        final ApiDescription apiDescription = ApiDescription.apiDescription()
                .id("frapi:test")
                .version("2.0")
                .description(new LocalizableString("My Description"))
                .definitions(definitions)
                .build();
        final OpenApiTransformer transformer = new OpenApiTransformer(new LocalizableString("Test"), "localhost:8080",
                "/", false, apiDescription);

        transformer.buildDefinitions();

        assertThat(transformer.openApi.getComponents().getSchemas()).containsEntry("myDef",
                new LocalizableSchema().type("object"));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @DataProvider(name = "buildSchemaData")
    public Object[][] buildSchemaData() {
        return new Object[][]{
                {null, null, NullPointerException.class},
                {json(null), null, TransformerException.class},
                {json(object(field("type", "not_a_json_schema_type"))), null, TransformerException.class},
                {json(object(field("type", "object"))), new LocalizableSchema().type("object"), null},
                {json(object(
                        field("type", "object"),
                        field("properties", object(field("name", object(field("type", "string"))))),
                        field("required", array("name")),
                        field("title", "My Title"),
                        field("description", "My Description"))),
                        new Supplier<io.swagger.v3.oas.models.media.Schema>() {
                            @Override
                            public io.swagger.v3.oas.models.media.Schema get() {
                                final Map<String, io.swagger.v3.oas.models.media.Schema> properties = new HashMap<>();
                                properties.put("name", new LocalizableSchema().type("string"));

                                final LocalizableSchema o = new LocalizableSchema();
                                o.type("object");
                                o.setProperties(properties);
                                o.setRequired(asList("name"));
                                o.setTitle("My Title");
                                o.setDescription("My Description");
                                return o;
                            }
                        }.get(), null},
                {json(object(field("type", "array"))),
                        new Supplier<io.swagger.v3.oas.models.media.Schema>() {
                            @Override
                            public io.swagger.v3.oas.models.media.Schema get() {
                                final LocalizableSchema o = new LocalizableSchema();
                                o.type("array");
                                o.setItems(new LocalizableSchema().type("any"));
                                return o;
                            }
                        }.get(), null},
                {json(object(
                        field("type", "array"),
                        field("items", object(field("type", "string"))),
                        field("title", "My Title"),
                        field("description", "My Description"))),
                        new Supplier<io.swagger.v3.oas.models.media.Schema>() {
                            @Override
                            public io.swagger.v3.oas.models.media.Schema get() {
                                final LocalizableSchema o = new LocalizableSchema();
                                o.type("array");
                                o.setItems(new LocalizableSchema().type("string"));
                                o.setTitle("My Title");
                                o.setDescription("My Description");
                                return o;
                            }
                        }.get(), null},
                {json(object(field("type", "boolean"))), new LocalizableSchema().type("boolean"), null},
                {json(object(field("type", "integer"))), new LocalizableSchema().type("integer"), null},
                {json(object(field("type", "number"))), new LocalizableSchema().type("number"), null},
                {json(object(field("type", "null"))), new LocalizableSchema().type("null"), null},
                {json(object(field("type", "any"))), new LocalizableSchema().type("any"), null},
                // array of non-"null" types defaults to "any"
                {json(object(field("type", array("string", "object")))), new LocalizableSchema().type("any"), null},
                // array of two types has "null" type removed, and single remaining type will be used
                {json(object(field("type", array("string", "null")))), new LocalizableSchema().type("string"), null},
                {json(object(field("type", "string"))), new LocalizableSchema().type("string"), null},
                {json(object(
                        field("type", "string"),
                        field("default", "my_default"))),
                        new Supplier<io.swagger.v3.oas.models.media.Schema>() {
                            @Override
                            public io.swagger.v3.oas.models.media.Schema get() {
                                final LocalizableSchema o = new LocalizableSchema();
                                o.type("string");
                                o.setDefault("my_default");
                                return o;
                            }
                        }.get(), null},
                {json(object(
                        field("type", "string"),
                        field("format", "full-date"))),
                        new Supplier<io.swagger.v3.oas.models.media.Schema>() {
                            @Override
                            public io.swagger.v3.oas.models.media.Schema get() {
                                final LocalizableSchema o = new LocalizableSchema();
                                o.type("string");
                                o.setFormat("date");
                                return o;
                            }
                        }.get(), null},
                {json(object(
                        field("type", "string"),
                        field("enum", array("enum_1", "enum_2")),
                        field("options", object(field("enum_titles", array("enum_1_title", "enum_2_title")))))),
                        new Supplier<io.swagger.v3.oas.models.media.Schema>() {
                            @Override
                            public io.swagger.v3.oas.models.media.Schema get() {
                                final LocalizableSchema o = new LocalizableSchema();
                                o.type("string");
                                o.setEnum(Arrays.asList("enum_1", "enum_2"));
                                o.addExtension("x-enum_titles", Arrays.asList("enum_1_title", "enum_2_title"));
                                return o;
                            }
                        }.get(), null},
                {json(object(
                        field("type", "object"),
                        field("additionalProperties", object(field("type", "string"))))),
                        new Supplier<io.swagger.v3.oas.models.media.Schema>() {
                            @Override
                            public io.swagger.v3.oas.models.media.Schema get() {
                                final LocalizableSchema o = new LocalizableSchema();
                                o.type("object");
                                o.setAdditionalProperties(new LocalizableSchema().type("string"));
                                return o;
                            }
                        }.get(), null},
                {json(object(
                        field("title", "This is a cool title"),
                        field("description", "This is a cool description"),
                        field("allOf", array(
                            object(field("$ref", "#/definitions/someDefinition")),
                            object(field("type", "object")))))),
                        new Supplier<io.swagger.v3.oas.models.media.Schema>() {
                            @Override
                            public io.swagger.v3.oas.models.media.Schema get() {
                                final LocalizableSchema m1 = new LocalizableSchema();
                                m1.set$ref("#/definitions/someDefinition");

                                final LocalizableSchema m2 = new LocalizableSchema();
                                m2.type("object");

                                final LocalizableSchema o = new LocalizableSchema();
                                o.setTitle("This is a cool title");
                                o.setDescription("This is a cool description");
                                o.allOf(asList(m1, m2));
                                return o;
                            }
                        }.get(), null},
                {json(object(
                        field("title", "This is a cool title"),
                        field("description", "This is a cool description"),
                        field("allOf", null))),
                 null, TransformerException.class},
                {json(object(
                        field("title", "This is a cool title"),
                        field("description", "This is a cool description"),
                        field("allOf", array()))),
                 null, TransformerException.class},
                {jsonValueForSchema(PojoOuter.class),
                        new Supplier<io.swagger.v3.oas.models.media.Schema>() {
                            @Override
                            public io.swagger.v3.oas.models.media.Schema get() {
                                // this one has a 'title' because it is the first encounter of PojoInner class
                                final LocalizableSchema pojoProp1 = new LocalizableSchema();
                                pojoProp1.set$ref("#/components/schemas/"
                                        + "urn:jsonschema:org:forgerock:api:transform:PojoInner");
                                pojoProp1.description(PojoOuter.DESCRIPTION_1);
                                pojoProp1.title(PojoInner.TITLE);

                                // there is no 'title', because this second encounter was always a JSON Reference
                                final LocalizableSchema pojoProp2 = new LocalizableSchema();
                                pojoProp2.set$ref("#/components/schemas/"
                                        + "urn:jsonschema:org:forgerock:api:transform:PojoInner");
                                pojoProp2.description(PojoOuter.DESCRIPTION_2);

                                final LocalizableSchema model = new LocalizableSchema();
                                model.type("object");
                                model.title(PojoOuter.TITLE);
                                final Map<String, io.swagger.v3.oas.models.media.Schema> properties = new HashMap<>();
                                properties.put("pojoProp1", pojoProp1);
                                properties.put("pojoProp2", pojoProp2);
                                model.setProperties(properties);
                                return model;
                            }
                        }.get(), null
                },
        };
    }

    @SuppressWarnings("rawtypes")
    @Test(dataProvider = "buildSchemaData")
    public void testBuildSchema(final JsonValue schema, final io.swagger.v3.oas.models.media.Schema expectedReturnValue,
            final Class<? extends Throwable> expectedException) {
        final OpenApiTransformer transformer = new OpenApiTransformer();

        if (expectedException != null) {
            assertThatExceptionOfType(expectedException).isThrownBy(() -> {
                transformer.buildSchema(schema);
            });
        } else {
            final io.swagger.v3.oas.models.media.Schema actualReturnValue = transformer.buildSchema(schema);

            // Compare the two schemas as JSON to avoid equality issues
            assertEqualAsJson(expectedReturnValue, actualReturnValue);
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @DataProvider(name = "buildSchemaFromJsonData")
    public Object[][] buildSchemaFromJsonData() {
        return new Object[][]{
                {null, null, null},
                {json(null), null, null},
                {json(object(field("type", "not_a_json_schema_type"))), null, TransformerException.class},
                {json(object(field("type", "null"))), null, null},
                {json(object(
                        field("type", "any"))),
                        new Supplier<io.swagger.v3.oas.models.media.Schema>() {
                            @Override
                            public io.swagger.v3.oas.models.media.Schema get() {
                                final LocalizableSchema o = new LocalizableSchema();
                                o.type("any");
                                return o;
                            }
                        }.get(), null},
                // array of non-"null" types defaults to "any"
                {json(object(
                        field("type", array("string", "object")))),
                        new Supplier<io.swagger.v3.oas.models.media.Schema>() {
                            @Override
                            public io.swagger.v3.oas.models.media.Schema get() {
                                final LocalizableSchema o = new LocalizableSchema();
                                o.type("any");
                                return o;
                            }
                        }.get(), null},
                // array of two types has "null" type removed, and single remaining type will be used
                {json(object(field("type", array("object", "null")))),
                        new LocalizableSchema().type("object"), null},
                {json(object(field("type", "object"))),
                        new LocalizableSchema().type("object"), null},
                {json(object(
                        field("type", "object"),
                        field("properties", object(field("name", object(field("type", "string"))))),
                        field("required", array("name")),
                        field("default", object(field("name", "myName"))))),
                        new Supplier<io.swagger.v3.oas.models.media.Schema>() {
                            @Override
                            public io.swagger.v3.oas.models.media.Schema get() {
                                final Map<String, io.swagger.v3.oas.models.media.Schema> properties = new HashMap<>();
                                properties.put("name", new LocalizableSchema().type("string"));

                                final LocalizableSchema o = new LocalizableSchema();
                                o.type("object");
                                o.setProperties(properties);
                                o.setRequired(Arrays.asList("name"));
                                o.setDefault(object(field("name", "myName")));
                                return o;
                            }
                        }.get(), null},
                {json(object(field("type", "array"))),
                        new Supplier<io.swagger.v3.oas.models.media.Schema>() {
                            @Override
                            public io.swagger.v3.oas.models.media.Schema get() {
                                final LocalizableSchema o = new LocalizableSchema();
                                o.type("array");
                                o.setItems(new LocalizableSchema().type("any"));
                                return o;
                            }
                        }.get(), null},
                {json(object(field("type", "array"), field("items", null))), null, TransformerException.class},
                {json(object(
                        field("type", "array"),
                        field("items", object(field("type", "string"))),
                        field("minItems", 1),
                        field("maxItems", 10),
                        field("uniqueItems", true),
                        field("default", array("myValue")))),
                        new Supplier<io.swagger.v3.oas.models.media.Schema>() {
                            @Override
                            public io.swagger.v3.oas.models.media.Schema get() {
                                final LocalizableSchema o = new LocalizableSchema();
                                o.type("array");
                                o.setItems(new LocalizableSchema().type("string"));
                                o.setMinItems(1);
                                o.setMaxItems(10);
                                o.setUniqueItems(true);
                                o.setDefault(array("myValue"));
                                return o;
                            }
                        }.get(), null},
                {json(object(field("type", "boolean"))),
                        new LocalizableSchema().type("boolean"), null},
                {json(object(
                        field("type", "boolean"),
                        field("default", true))),
                        new Supplier<io.swagger.v3.oas.models.media.Schema>() {
                            @Override
                            public io.swagger.v3.oas.models.media.Schema get() {
                                final LocalizableSchema o = new LocalizableSchema();
                                o.type("boolean");
                                o.setDefault("true");
                                return o;
                            }
                        }.get(), null},
                {json(object(field("type", "integer"))),
                        new LocalizableSchema().type("integer"), null},
                {json(object(field("type", "integer"), field("format", "int32"))),
                        new Supplier<io.swagger.v3.oas.models.media.Schema>() {
                            @Override
                            public io.swagger.v3.oas.models.media.Schema get() {
                                final LocalizableSchema o = new LocalizableSchema();
                                o.type("integer");
                                o.setFormat("int32");
                                return o;
                            }
                        }.get(), null},
                {json(object(field("type", "integer"), field("format", "int64"))),
                        new Supplier<io.swagger.v3.oas.models.media.Schema>() {
                            @Override
                            public io.swagger.v3.oas.models.media.Schema get() {
                                final LocalizableSchema o = new LocalizableSchema();
                                o.type("integer");
                                o.setFormat("int64");
                                return o;
                            }
                        }.get(), null},
                {json(object(
                        field("type", "integer"),
                        field("format", "int64"),
                        field("minimum", 1.0),
                        field("maximum", 2.0),
                        field("exclusiveMinimum", true),
                        field("exclusiveMaximum", true),
                        field("readOnly", true),
                        field("default", 1))),
                        new Supplier<io.swagger.v3.oas.models.media.Schema>() {
                            @Override
                            public io.swagger.v3.oas.models.media.Schema get() {
                                final LocalizableSchema o = new LocalizableSchema();
                                o.type("integer");
                                o.setFormat("int64");
                                o.setMinimum(BigDecimal.valueOf(1.0d));
                                o.setMaximum(BigDecimal.valueOf(2.0d));
                                o.setExclusiveMinimum(true);
                                o.setExclusiveMaximum(true);
                                o.setReadOnly(true);
                                o.setDefault("1");
                                return o;
                            }
                        }.get(), null},
                {json(object(field("type", "number"))),
                        new LocalizableSchema().type("number"), null},
                {json(object(field("type", "number"), field("format", "int32"))),
                        new Supplier<io.swagger.v3.oas.models.media.Schema>() {
                            @Override
                            public io.swagger.v3.oas.models.media.Schema get() {
                                final LocalizableSchema o = new LocalizableSchema();
                                o.type("number");
                                o.setFormat("int32");
                                return o;
                            }
                        }.get(), null},
                {json(object(field("type", "number"), field("format", "int64"))),
                        new Supplier<io.swagger.v3.oas.models.media.Schema>() {
                            @Override
                            public io.swagger.v3.oas.models.media.Schema get() {
                                final LocalizableSchema o = new LocalizableSchema();
                                o.type("number");
                                o.setFormat("int64");
                                return o;
                            }
                        }.get(), null},
                {json(object(field("type", "number"), field("format", "float"))),
                        new Supplier<io.swagger.v3.oas.models.media.Schema>() {
                            @Override
                            public io.swagger.v3.oas.models.media.Schema get() {
                                final LocalizableSchema o = new LocalizableSchema();
                                o.type("number");
                                o.setFormat("float");
                                return o;
                            }
                        }.get(), null},
                {json(object(field("type", "number"), field("format", "double"))),
                        new Supplier<io.swagger.v3.oas.models.media.Schema>() {
                            @Override
                            public io.swagger.v3.oas.models.media.Schema get() {
                                final LocalizableSchema o = new LocalizableSchema();
                                o.type("number");
                                o.setFormat("double");
                                return o;
                            }
                        }.get(), null},
                {json(object(
                        field("type", "number"),
                        field("format", "double"),
                        field("minimum", 1.0),
                        field("maximum", 2.0),
                        field("exclusiveMinimum", true),
                        field("exclusiveMaximum", true),
                        field("default", 1.1))),
                        new Supplier<io.swagger.v3.oas.models.media.Schema>() {
                            @Override
                            public io.swagger.v3.oas.models.media.Schema get() {
                                final LocalizableSchema o = new LocalizableSchema();
                                o.type("number");
                                o.setFormat("double");
                                o.setMinimum(BigDecimal.valueOf(1.0d));
                                o.setMaximum(BigDecimal.valueOf(2.0d));
                                o.setExclusiveMinimum(true);
                                o.setExclusiveMaximum(true);
                                o.setDefault("1.1");
                                return o;
                            }
                        }.get(), null},
                {json(object(field("type", "string"))),
                        new LocalizableSchema().type("string"), null},
                {json(object(field("type", "string"), field("format", "byte"))),
                        new Supplier<io.swagger.v3.oas.models.media.Schema>() {
                            @Override
                            public io.swagger.v3.oas.models.media.Schema get() {
                                final LocalizableSchema o = new LocalizableSchema();
                                o.type("string");
                                o.setFormat("byte");
                                return o;
                            }
                        }.get(), null},
                {json(object(
                        field("type", "string"),
                        field("format", "byte"),
                        field("default", "AA=="))),
                        new Supplier<io.swagger.v3.oas.models.media.Schema>() {
                            @Override
                            public io.swagger.v3.oas.models.media.Schema get() {
                                final LocalizableSchema o = new LocalizableSchema();
                                o.type("string");
                                o.setFormat("byte");
                                o.setDefault("AA==");
                                return o;
                            }
                        }.get(), null},
                {json(object(field("type", "string"), field("format", "binary"))),
                        new Supplier<io.swagger.v3.oas.models.media.Schema>() {
                            @Override
                            public io.swagger.v3.oas.models.media.Schema get() {
                                final LocalizableSchema o = new LocalizableSchema();
                                o.type("string");
                                o.setFormat("binary");
                                return o;
                            }
                        }.get(), null},
                {json(object(
                        field("type", "string"),
                        field("format", "binary"),
                        field("default", "Rm9yZ2VSb2Nr"))),
                        new Supplier<io.swagger.v3.oas.models.media.Schema>() {
                            @Override
                            public io.swagger.v3.oas.models.media.Schema get() {
                                final LocalizableSchema o = new LocalizableSchema();
                                o.type("string");
                                o.setFormat("binary");
                                o.setDefault("Rm9yZ2VSb2Nr");
                                return o;
                            }
                        }.get(), null},
                {json(object(field("type", "string"), field("format", "date"))),
                        new Supplier<io.swagger.v3.oas.models.media.Schema>() {
                            @Override
                            public io.swagger.v3.oas.models.media.Schema get() {
                                final LocalizableSchema o = new LocalizableSchema();
                                o.type("string");
                                o.setFormat("date");
                                return o;
                            }
                        }.get(), null},
                {json(object(
                        field("type", "string"),
                        field("format", "full-date"),
                        field("default", "2010-11-17"))),
                        new Supplier<io.swagger.v3.oas.models.media.Schema>() {
                            @Override
                            public io.swagger.v3.oas.models.media.Schema get() {
                                final LocalizableSchema o = new LocalizableSchema();
                                o.type("string");
                                o.setFormat("date");
                                o.setDefault("2010-11-17");
                                return o;
                            }
                        }.get(), null},
                {json(object(field("type", "string"), field("format", "date-time"))),
                        new Supplier<io.swagger.v3.oas.models.media.Schema>() {
                            @Override
                            public io.swagger.v3.oas.models.media.Schema get() {
                                final LocalizableSchema o = new LocalizableSchema();
                                o.type("string");
                                o.setFormat("date-time");
                                return o;
                            }
                        }.get(), null},
                {json(object(
                        field("type", "string"),
                        field("format", "date-time"),
                        field("default", "2010-11-17T00:00:00Z"))),
                        new Supplier<io.swagger.v3.oas.models.media.Schema>() {
                            @Override
                            public io.swagger.v3.oas.models.media.Schema get() {
                                final LocalizableSchema o = new LocalizableSchema();
                                o.type("string");
                                o.setFormat("date-time");
                                o.setDefault("2010-11-17T00:00:00Z");
                                return o;
                            }
                        }.get(), null},
                {json(object(field("type", "string"), field("format", "password"))),
                        new Supplier<io.swagger.v3.oas.models.media.Schema>() {
                            @Override
                            public io.swagger.v3.oas.models.media.Schema get() {
                                final LocalizableSchema o = new LocalizableSchema();
                                o.type("string");
                                o.setFormat("password");
                                return o;
                            }
                        }.get(), null},
                {json(object(field("type", "string"), field("format", "uuid"))),
                        new Supplier<io.swagger.v3.oas.models.media.Schema>() {
                            @Override
                            public io.swagger.v3.oas.models.media.Schema get() {
                                final LocalizableSchema o = new LocalizableSchema();
                                o.type("string");
                                o.setFormat("uuid");
                                return o;
                            }
                        }.get(), null},
                {json(object(
                        field("type", "string"),
                        field("format", "an_unsupported_format"),
                        field("minLength", 1),
                        field("maxLength", 10),
                        field("pattern", "^[a-z]{1,10}$"),
                        field("default", "abc"),
                        field("title", "My Title"),
                        field("description", "My Description"),
                        field("readOnly", false),
                        field("readPolicy", ReadPolicy.USER.name()),
                        field("returnOnDemand", false),
                        field("writePolicy", WritePolicy.WRITABLE.name()),
                        field("errorOnWritePolicyFailure", false),
                        field("propertyOrder", 100))),
                        new Supplier<io.swagger.v3.oas.models.media.Schema>() {
                            @Override
                            public io.swagger.v3.oas.models.media.Schema get() {
                                final LocalizableSchema o = new LocalizableSchema();
                                o.type("string");
                                o.setFormat("an_unsupported_format");
                                o.setMinLength(1);
                                o.setMaxLength(10);
                                o.setPattern("^[a-z]{1,10}$");
                                o.setDefault("abc");
                                o.setTitle("My Title");
                                o.setDescription("My Description");
                                o.addExtension("x-readPolicy", ReadPolicy.USER.name());
                                o.addExtension("x-returnOnDemand", false);
                                o.addExtension("x-writePolicy", WritePolicy.WRITABLE.name());
                                o.addExtension("x-errorOnWritePolicyFailure", false);
                                o.addExtension("x-propertyOrder", 100);
                                return o;
                            }
                        }.get(), null},
        };
    }

    @SuppressWarnings("rawtypes")
    @Test(dataProvider = "buildSchemaFromJsonData")
    public void testBuildSchemaFromJson(final JsonValue schema,
            final io.swagger.v3.oas.models.media.Schema expectedReturnValue,
            final Class<? extends Throwable> expectedException) {
        final OpenApiTransformer transformer = new OpenApiTransformer();
        final io.swagger.v3.oas.models.media.Schema actualReturnValue;
        try {
            actualReturnValue = transformer.buildSchemaFromJson(schema);
        } catch (final Exception e) {
            if (expectedException != null) {
                assertThat(e).isInstanceOf(expectedException);
            }
            return;
        }

        if (expectedException != null) {
            failBecauseExceptionWasNotThrown(expectedException);
        }

        assertThat(actualReturnValue).isEqualTo(expectedReturnValue);
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void testBuildSchemaProperties() {
        // build schema with properties out-of-order (given propertyOrder field)
        final JsonValue schema = json(object(
                field("type", "object"),
                field("properties", object(
                        field("fieldOrderNone", object(
                                field("type", "string"))),
                        field("fieldOrder100", object(
                                field("type", "string"),
                                field("propertyOrder", 100))),
                        field("fieldOrder1", object(
                                field("type", "string"),
                                field("propertyOrder", 1))),
                        field("fieldOrderNoneToo", object(
                                field("type", "string")))
                ))));

        final OpenApiTransformer transformer = new OpenApiTransformer();
        final Map<String, io.swagger.v3.oas.models.media.Schema> schemaProperties =
                transformer.buildSchemaProperties(schema);

        // check that properties are now in correct order
        final Iterator<String> iterator = schemaProperties.keySet().iterator();
        assertThat(iterator.next()).isEqualTo("fieldOrder1");
        assertThat(iterator.next()).isEqualTo("fieldOrder100");
        assertThat(iterator.next()).isEqualTo("fieldOrderNone");
        assertThat(iterator.next()).isEqualTo("fieldOrderNoneToo");
    }

    @Test
    public void testGetDefinitionsReference() {
        final OpenApiTransformer transformer = new OpenApiTransformer();
        final String reference = transformer.getDefinitionsReference(
                reference().value("#/definitions/myDef").build());
        assertThat(reference).isEqualTo("myDef");
        assertThat(transformer.getDefinitionsReference((Reference) null)).isNull();
    }

    @DataProvider
    public static Object[][] countPolicies() {
        // @Checkstyle:off
        return new Object[][] {
                { null, new String[] { "NONE" } },
                { new CountPolicy[] { }, new String[] { "NONE" } },
                { new CountPolicy[] { CountPolicy.NONE }, new String[] { "NONE" } },
                { new CountPolicy[] { CountPolicy.EXACT }, new String[] { "EXACT" } },
                { new CountPolicy[] { CountPolicy.EXACT, CountPolicy.ESTIMATE }, new String[] { "EXACT", "ESTIMATE" } }
        };
        // @Checkstyle:on
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test(dataProvider = "countPolicies")
    public void testTotalPagedResultPolicyIsProperlyFilled(CountPolicy[] policies, String[] expected) throws Exception {
        Resource resource = resource()
                .resourceSchema(schema().schema(json(object(field("type", "object")))).build())
                .title("test")
                .mvccSupported(false)
                .query(query().queryId("test").countPolicies(policies).type(QueryType.ID).build())
                .build();
        final ApiDescription apiDescription = ApiDescription.apiDescription()
                .id("frapi:test")
                .version("1.0")
                .paths(paths().put("/test", versionedPath().put("1.0", resource).build()).build())
                .build();
        OpenAPI openApi = OpenApiTransformer.execute(new LocalizableString("Test"), "localhost:8080",
                "/", false, apiDescription);

        List<Parameter> parameters = openApi.getPaths().get("/test#1.0_query_id_test").getGet().getParameters();
        assertThat(parameters)
                .filteredOn(totalPagedResultsPolicy())
                .hasSize(1)
                .hasOnlyElementsOfType(Parameter.class)
                .extracting(enumValues())
                .has(countPolicies(expected), atIndex(0));
    }

    @Test
    public void testBuildErrorSchema() {
        final OpenApiTransformer transformer = new OpenApiTransformer();

        final ApiError apiError = ApiError.apiError()
                .code(404)
                .description("404 description")
                .build();
        final JsonValue apiErrorSchema = transformer.buildErrorSchema(apiError);

        final ApiError apiErrorWithCause = ApiError.apiError()
                .code(404)
                .description("404 description")
                .schema(Schema.schema()
                        .schema(json(object(field("type", "object"))))
                        .build())
                .build();
        final JsonValue apiErrorWithCauseSchema = transformer.buildErrorSchema(apiErrorWithCause);

        final String id = "frapi:models:ApiError";
        final JsonPointer causePointer = new JsonPointer("/properties/cause");

        assertThat(apiErrorSchema.get("id").asString()).isEqualTo(id);
        assertThat(apiErrorSchema.get(causePointer)).isNull();

        assertThat(apiErrorWithCauseSchema.get("id").asString()).startsWith(id).isNotEqualTo(id);
        assertThat(apiErrorWithCauseSchema.get(causePointer)).isNotNull();
    }

    @SuppressWarnings("rawtypes")
    private static void assertEqualAsJson(final io.swagger.v3.oas.models.media.Schema expected,
            final io.swagger.v3.oas.models.media.Schema actual) {
        final String    expectedJson    = objectToJsonString(expected),
                        actualJson      = objectToJsonString(actual);

        assertThat(actualJson).isEqualTo(expectedJson);
    }

    @SuppressWarnings("rawtypes")
    private static String objectToJsonString(final io.swagger.v3.oas.models.media.Schema object) {
        String json = null;

        try {
            json = JacksonUtils.createGenericMapper().writeValueAsString(object);
        } catch (JsonProcessingException e) {
            fail("Could not serialize object to JSON", e);
        }

        return json;
    }

    private static JsonValue jsonValueForSchema(final Class<?> type) {
        try {
            final JsonSchema schema = JacksonUtils.schemaFor(type);
            final byte[] bytes = JacksonUtils.OBJECT_MAPPER.writeValueAsBytes(schema);
            return json(JacksonUtils.OBJECT_MAPPER.readValue(bytes, Object.class));
        } catch (IOException e) {
            throw new RuntimeException("Failed to serialize JSON", e);
        }
    }

    private static Condition<List<String>> countPolicies(final String[] expected) {
        return new Condition<List<String>>() {
            @Override
            public boolean matches(final List<String> value) {
                return value.containsAll(Arrays.asList(expected)) && value.size() == expected.length;
            }
        };
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static Extractor<Parameter, List<String>> enumValues() {
        return new Extractor<Parameter, List<String>>() {
            @Override
            public List<String> extract(final Parameter input) {
                return input.getSchema().getEnum();
            }
        };
    }

    private static Condition<Parameter> totalPagedResultsPolicy() {
        return new Condition<Parameter>() {
            @Override
            public boolean matches(final Parameter parameter) {
                return "_totalPagedResultsPolicy".equals(parameter.getName());
            }
        };
    }

    private interface Supplier<T> {
        T get();
    }
}
