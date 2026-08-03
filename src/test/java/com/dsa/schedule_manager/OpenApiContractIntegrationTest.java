package com.dsa.schedule_manager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OpenApiContractIntegrationTest {

    @Autowired
    MockMvc mockMvc;


    @Test
    void openApiDocumentsActualSuccessStatusCodes() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/auth/signup'].post.responses['201']").exists())
                .andExpect(jsonPath("$.paths['/api/auth/logout'].post.responses['204']").exists())
                .andExpect(jsonPath("$.paths['/api/schedules'].post.responses['201']").exists())
                .andExpect(jsonPath("$.paths['/api/schedules/{id}'].delete.responses['204']").exists())
                .andExpect(jsonPath("$.paths['/api/schedules/{id}'].patch.responses['409']").exists())
                .andExpect(jsonPath("$.paths['/api/auth/signup'].post.responses['200']").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/auth/logout'].post.responses['200']").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/schedules'].post.responses['200']").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/schedules/{id}'].delete.responses['200']").doesNotExist());
    }
}
