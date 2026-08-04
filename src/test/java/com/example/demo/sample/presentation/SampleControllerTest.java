package com.example.demo.sample.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.demo.sample.application.command.CreateSampleMessageCommand;
import com.example.demo.sample.application.result.SampleMessageResult;
import com.example.demo.sample.application.usecase.CreateSampleMessageUseCase;
import com.example.demo.sample.application.usecase.GetSampleMessageUseCase;
import com.example.demo.sample.presentation.converter.SampleCommandConverter;
import com.example.demo.sample.presentation.converter.SampleResultConverter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SampleController.class)
@Import({
    SampleCommandConverter.class,
    SampleResultConverter.class,
    SampleControllerTest.MockBeans.class
})
class SampleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CreateSampleMessageUseCase createSampleMessageUseCase;

    @Autowired
    private GetSampleMessageUseCase getSampleMessageUseCase;

    @BeforeEach
    void setUp() {
        when(createSampleMessageUseCase.execute(any(CreateSampleMessageCommand.class)))
                .thenReturn(new SampleMessageResult("Created from API"));
        when(getSampleMessageUseCase.execute())
                .thenReturn(new SampleMessageResult("Hello from demo"));
    }

    @Test
    void 메시지를_생성하면_created와_json을_응답한다() throws Exception {
        mockMvc.perform(post("/api/samples")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Created from API\"}"))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("{\"message\":\"Created from API\"}"));
    }

    @Test
    void 메시지를_조회하면_ok와_json을_응답한다() throws Exception {
        mockMvc.perform(get("/api/samples"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("{\"message\":\"Hello from demo\"}"));
    }

    @Test
    void blank_메시지는_bad_request를_응답한다() throws Exception {
        mockMvc.perform(post("/api/samples")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\" \"}"))
                .andExpect(status().isBadRequest());
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class MockBeans {

        @Bean
        CreateSampleMessageUseCase createSampleMessageUseCase() {
            return mock(CreateSampleMessageUseCase.class);
        }

        @Bean
        GetSampleMessageUseCase getSampleMessageUseCase() {
            return mock(GetSampleMessageUseCase.class);
        }
    }
}
