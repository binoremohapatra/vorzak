package com.vorzak;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InvestigationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testReplayInvestigationEndpoint() throws Exception {
        mockMvc.perform(post("/api/investigations/replay/case-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode").value("REPLAY"))
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    void testSubmitInvestigationWithPngFile() throws Exception {
        // Valid PNG header bytes
        byte[] validPng = new byte[] {
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
                0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52
        };

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test_suspicious.png",
                "image/png",
                validPng
        );

        org.springframework.mock.web.MockPart claimPart = new org.springframework.mock.web.MockPart("claimText", "This photo shows a breaking incident".getBytes());

        mockMvc.perform(multipart("/api/investigations")
                        .file(file)
                        .part(claimPart)
                        .header("Idempotency-Key", "test-key-1"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.sha256").exists())
                .andExpect(jsonPath("$.mode").value("LIVE"));
    }
}
