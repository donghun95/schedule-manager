package com.dsa.schedule_manager;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OpenApiContractIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void openApiDocumentsActualSuccessStatusCodes() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/auth/signup'].post.responses['201']").exists())
                .andExpect(jsonPath("$.paths['/api/auth/logout'].post.responses['204']").exists())
                .andExpect(jsonPath("$.paths['/api/schedules'].post.responses['201']").exists())
                .andExpect(jsonPath("$.paths['/api/schedules'].get.responses['200']").exists())
                .andExpect(jsonPath("$.paths['/api/schedules'].get.responses['400']").exists())
                .andExpect(jsonPath("$.paths['/api/schedules'].get.responses['401']").exists())
                .andExpect(jsonPath("$.paths['/api/schedules/{id}'].delete.responses['204']").exists())
                .andExpect(jsonPath("$.paths['/api/schedules/{id}'].patch.responses['409']").exists())
                .andExpect(jsonPath(
                        "$.paths['/api/schedules/{scheduleId}/participants'].post.responses['201']")
                        .exists())
                .andExpect(jsonPath(
                        "$.paths['/api/schedules/{scheduleId}/participants'].get.responses['200']")
                        .exists())
                .andExpect(jsonPath(
                        "$.paths['/api/schedules/{scheduleId}/participants'].get.responses['401']")
                        .exists())
                .andExpect(jsonPath(
                        "$.paths['/api/schedules/{scheduleId}/participants'].get.responses['403']")
                        .exists())
                .andExpect(jsonPath(
                        "$.paths['/api/schedules/{scheduleId}/participants'].get.responses['404']")
                        .exists())
                .andExpect(jsonPath(
                        "$.paths['/api/schedules/{scheduleId}/participants'].post.responses['400']")
                        .exists())
                .andExpect(jsonPath(
                        "$.paths['/api/schedules/{scheduleId}/participants'].post.responses['401']")
                        .exists())
                .andExpect(jsonPath(
                        "$.paths['/api/schedules/{scheduleId}/participants'].post.responses['403']")
                        .exists())
                .andExpect(jsonPath(
                        "$.paths['/api/schedules/{scheduleId}/participants'].post.responses['404']")
                        .exists())
                .andExpect(jsonPath(
                        "$.paths['/api/schedules/{scheduleId}/participants'].post.responses['409']")
                        .exists())
                .andExpect(jsonPath(
                        "$.paths['/api/schedules/{scheduleId}/participants/{userId}'].delete.responses['204']")
                        .exists())
                .andExpect(jsonPath(
                        "$.paths['/api/schedules/{scheduleId}/participants/{userId}'].delete.responses['401']")
                        .exists())
                .andExpect(jsonPath(
                        "$.paths['/api/schedules/{scheduleId}/participants/{userId}'].delete.responses['403']")
                        .exists())
                .andExpect(jsonPath(
                        "$.paths['/api/schedules/{scheduleId}/participants/{userId}'].delete.responses['404']")
                        .exists())
                .andExpect(jsonPath(
                        "$.paths['/api/schedules/{scheduleId}/status'].patch.responses['400']")
                        .exists())
                .andExpect(jsonPath(
                        "$.paths['/api/schedules/{scheduleId}/status'].patch.responses['401']")
                        .exists())
                .andExpect(jsonPath(
                        "$.paths['/api/schedules/{scheduleId}/status'].patch.responses['403']")
                        .exists())
                .andExpect(jsonPath(
                        "$.paths['/api/schedules/{scheduleId}/status'].patch.responses['404']")
                        .exists())
                .andExpect(jsonPath(
                        "$.paths['/api/schedules/{scheduleId}/status'].patch.responses['409']")
                        .exists())
                .andExpect(jsonPath(
                        "$.paths['/api/schedules/{scheduleId}/history'].get.responses['401']")
                        .exists())
                .andExpect(jsonPath(
                        "$.paths['/api/schedules/{scheduleId}/history'].get.responses['403']")
                        .exists())
                .andExpect(jsonPath(
                        "$.paths['/api/schedules/{scheduleId}/history'].get.responses['404']")
                        .exists())
                .andExpect(jsonPath(
                        "$.paths['/api/schedules/{id}'].delete.responses['409']")
                        .exists())
                .andExpect(jsonPath("$.paths['/api/auth/signup'].post.responses['200']").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/auth/logout'].post.responses['200']").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/schedules'].post.responses['200']").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/schedules/{id}'].delete.responses['200']").doesNotExist());
    }

    @Test
    void openApiDocumentsListParametersErrorsAndSessionCookie() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$.paths['/api/schedules'].get.parameters[*].name",
                        containsInAnyOrder(
                                "status",
                                "fromAt",
                                "toAt",
                                "cursorScheduledAt",
                                "cursorId",
                                "size")))
                .andExpect(jsonPath(
                        "$.paths['/api/schedules'].get.parameters[*].required",
                        containsInAnyOrder(false, false, false, false, false, false)))
                .andExpect(jsonPath(
                        "$.paths['/api/schedules'].get.responses['400'].content"
                                + "['application/json'].schema['$ref']",
                        is("#/components/schemas/ErrorResponse")))
                .andExpect(jsonPath(
                        "$.paths['/api/schedules'].get.responses['200'].content"
                                + "['application/json'].schema['$ref']",
                        is("#/components/schemas/CursorResponseScheduleSummaryResponse")))
                .andExpect(jsonPath(
                        "$.paths['/api/schedules'].post.responses['201'].content"
                                + "['application/json'].schema['$ref']",
                        is("#/components/schemas/ScheduleResponse")))
                .andExpect(jsonPath(
                        "$.paths['/api/schedules/{scheduleId}/participants'].get.responses['200']"
                                + ".content['application/json'].schema.type",
                        is("array")))
                .andExpect(jsonPath(
                        "$.components.securitySchemes.sessionCookie.type",
                        is("apiKey")))
                .andExpect(jsonPath(
                        "$.components.securitySchemes.sessionCookie.in",
                        is("cookie")))
                .andExpect(jsonPath(
                        "$.components.securitySchemes.sessionCookie.name",
                        is("SCHEDULEID")))
                .andExpect(jsonPath(
                        "$.paths['/api/schedules'].get.security[0].sessionCookie")
                        .exists())
                .andExpect(jsonPath(
                        "$.components.schemas.ScheduleSummaryResponse.properties.status.enum",
                        containsInAnyOrder("PLANNED", "IN_PROGRESS", "DONE", "CANCELED")))
                .andExpect(jsonPath(
                        "$.components.schemas.ScheduleSummaryResponse.properties.accessType.enum",
                        containsInAnyOrder("OWNER", "PARTICIPANT")))
                .andExpect(jsonPath(
                        "$.components.schemas.CursorResponseScheduleSummaryResponse.properties"
                                + ".nextCursorScheduledAt.type",
                        containsInAnyOrder("string", "null")))
                .andExpect(jsonPath(
                        "$.components.schemas.CursorResponseScheduleSummaryResponse.properties"
                                + ".nextCursorId.type",
                        containsInAnyOrder("integer", "null")));
    }

    @Test
    void allDocumentedClientErrorsUseTheCommonErrorResponse() throws Exception {
        String json = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode paths = objectMapper.readTree(json).get("paths");
        JsonNode listParameters = paths.get("/api/schedules").get("get").get("parameters");
        JsonNode statusParameter = null;
        for (JsonNode parameter : listParameters) {
            if ("status".equals(parameter.get("name").asString())) {
                statusParameter = parameter;
                break;
            }
        }
        assertThat(statusParameter).isNotNull();
        List<String> queryStatusValues = new ArrayList<>();
        for (JsonNode enumValue : statusParameter.get("schema").get("enum")) {
            queryStatusValues.add(enumValue.asString());
        }
        assertThat(queryStatusValues)
                .containsExactly("PLANNED", "IN_PROGRESS", "DONE", "CANCELED");

        int clientErrorCount = 0;
        for (JsonNode path : paths) {
            for (JsonNode operation : path) {
                JsonNode responses = operation.get("responses");
                if (responses == null) {
                    continue;
                }
                for (var responseEntry : responses.properties()) {
                    if (!responseEntry.getKey().startsWith("4")) {
                        continue;
                    }
                    clientErrorCount++;
                    assertThat(responseEntry.getValue()
                            .at("/content/application~1json/schema/$ref")
                            .asString())
                            .isEqualTo("#/components/schemas/ErrorResponse");
                }
            }
        }
        assertThat(clientErrorCount).isGreaterThan(0);
    }

    @Test
    void openApiClassifiesPublicAndProtectedOperations() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/auth/signup'].post.security").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/auth/login'].post.security").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/auth/logout'].post.security[0].sessionCookie").exists())
                .andExpect(jsonPath("$.paths['/api/users/me'].get.security[0].sessionCookie").exists())
                .andExpect(jsonPath("$.paths['/api/schedules'].get.security[0].sessionCookie").exists())
                .andExpect(jsonPath("$.paths['/api/schedules'].post.security[0].sessionCookie").exists())
                .andExpect(jsonPath("$.paths['/api/schedules/{id}'].get.security[0].sessionCookie").exists())
                .andExpect(jsonPath("$.paths['/api/schedules/{id}'].patch.security[0].sessionCookie").exists())
                .andExpect(jsonPath("$.paths['/api/schedules/{id}'].delete.security[0].sessionCookie").exists())
                .andExpect(jsonPath("$.paths['/api/schedules/{scheduleId}/participants']"
                        + ".get.security[0].sessionCookie").exists())
                .andExpect(jsonPath("$.paths['/api/schedules/{scheduleId}/participants']"
                        + ".post.security[0].sessionCookie").exists())
                .andExpect(jsonPath("$.paths['/api/schedules/{scheduleId}/participants/{userId}']"
                        + ".delete.security[0].sessionCookie").exists())
                .andExpect(jsonPath("$.paths['/api/schedules/{scheduleId}/status']"
                        + ".patch.security[0].sessionCookie").exists())
                .andExpect(jsonPath("$.paths['/api/schedules/{scheduleId}/history']"
                        + ".get.security[0].sessionCookie").exists());
    }

    @Test
    void swaggerUiRedirectIsPublic() throws Exception {
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/swagger-ui/index.html"));
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }
}
