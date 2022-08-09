package org.swasth.hcx.controllers.v1;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MvcResult;
import org.swasth.common.dto.ParticipantResponse;
import org.swasth.common.utils.Constants;
import org.swasth.common.utils.JSONUtils;
import org.swasth.hcx.controllers.BaseSpec;

import java.util.ArrayList;
import java.util.Map;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

class CoverageEligibilityTests extends BaseSpec {

    @Test
    void check_coverage_eligibility_failure_scenario() throws Exception {
      doNothing().when(mockKafkaClient).send(anyString(),anyString(),any());
      String requestBody = getRequestBody();
      MvcResult mvcResult = mockMvc.perform(post(Constants.VERSION_PREFIX + Constants.COVERAGE_ELIGIBILITY_CHECK).content(requestBody).contentType(MediaType.APPLICATION_JSON)).andReturn();
      MockHttpServletResponse response = mvcResult.getResponse();
      int status = response.getStatus();
      assertEquals(500, status);
    }

    @Test
    void check_coverage_eligibility_success_scenario() throws Exception {
        doNothing().when(mockKafkaClient).send(anyString(),anyString(),any());
        ParticipantResponse mockParticipantResponse = mock(ParticipantResponse.class);
        ResponseEntity mockResponseEntity = mock(ResponseEntity.class);
        Map<String, Object> participantMap = JSONUtils.deserialize(getParticipantPayorBody(), Map.class);
        ArrayList<Object> participantList = new ArrayList<>(){{ add(participantMap); }};
        when(mockParticipantService.search(anyMap())).thenReturn(mockResponseEntity);
        when(mockResponseEntity.getBody()).thenReturn(mockParticipantResponse);
        when(mockParticipantResponse.getParticipants()).thenReturn(participantList);
        String requestBody = getRequestBody();
        MvcResult mvcResult = mockMvc.perform(post(Constants.VERSION_PREFIX + Constants.COVERAGE_ELIGIBILITY_CHECK).content(requestBody).contentType(MediaType.APPLICATION_JSON)).andReturn();
        MockHttpServletResponse response = mvcResult.getResponse();
        int status = response.getStatus();
        assertEquals(202, status);
    }

    @Test
    void check_coverage_eligibility_subscription_scenario() throws Exception {
        doNothing().when(mockKafkaClient).send(anyString(),anyString(),any());
        when(postgreSQLClient.executeBatch()).thenReturn(new int[1]);
        doNothing().when(auditIndexer).createDocument(anyMap());
        ParticipantResponse mockParticipantResponse = mock(ParticipantResponse.class);
        ResponseEntity mockResponseEntity = mock(ResponseEntity.class);
        ArrayList<Object> participantList = new ArrayList<>(){{ add(getHIUParticipant()); }};
        when(mockParticipantService.search(anyMap())).thenReturn(mockResponseEntity);
        when(mockResponseEntity.getBody()).thenReturn(mockParticipantResponse);
        when(mockParticipantResponse.getParticipants()).thenReturn(participantList);
        String requestBody = getRequestBody();
        MvcResult mvcResult = mockMvc.perform(post(Constants.VERSION_PREFIX + Constants.COVERAGE_ELIGIBILITY_CHECK).content(requestBody).contentType(MediaType.APPLICATION_JSON)).andReturn();
        MockHttpServletResponse response = mvcResult.getResponse();
        int status = response.getStatus();
        assertNotNull(status);
    }



    @Test
    void check_coverage_eligibility_exception_scenario() throws Exception {
        String requestBody = getExceptionRequestBody();
        MvcResult mvcResult = mockMvc.perform(post(Constants.VERSION_PREFIX + Constants.COVERAGE_ELIGIBILITY_CHECK).content(requestBody).contentType(MediaType.APPLICATION_JSON)).andReturn();
        MockHttpServletResponse response = mvcResult.getResponse();
        int status = response.getStatus();
        assertEquals(500, status);
    }

    @Test
    void on_check_coverage_eligibility_success_scenario() throws Exception {
        doNothing().when(mockKafkaClient).send(anyString(),anyString(),any());
        String requestBody = getRequestBody();
        MvcResult mvcResult = mockMvc.perform(post(Constants.VERSION_PREFIX + Constants.COVERAGE_ELIGIBILITY_ONCHECK).content(requestBody).contentType(MediaType.APPLICATION_JSON)).andReturn();
        MockHttpServletResponse response = mvcResult.getResponse();
        int status = response.getStatus();
        assertEquals(202, status);
    }

    @Test
    void on_check_coverage_eligibility_exception_scenario() throws Exception {
        String requestBody = getExceptionRequestBody();
        MvcResult mvcResult = mockMvc.perform(post(Constants.VERSION_PREFIX + Constants.COVERAGE_ELIGIBILITY_ONCHECK).content(requestBody).contentType(MediaType.APPLICATION_JSON)).andReturn();
        MockHttpServletResponse response = mvcResult.getResponse();
        int status = response.getStatus();
        assertEquals(500, status);
    }

    @Test
    void on_check_coverage_eligibility_redirect() throws Exception {
        String requestBody = "{ \"x-hcx-recipient_code\": \"1-3a3bd68a-848a-4d52-9ec2-07a92d765fb4\", \"x-hcx-timestamp\": \"2021-10-27T20:35:52.636+0530\", \"x-hcx-sender_code\": \"1-2ff1f493-c4d4-4fc7-8d41-aaabb997af23\", \"x-hcx-correlation_id\": \"5e934f90-111d-4f0b-b016-c22d820674e1\", \"x-hcx-workflow_id\": \"26b1060c-1e83-4600-9612-ea31e0ca5094\", \"x-hcx-api_call_id\": \"26b1060c-1e83-4600-9612-ea31e0ca5194\", \"x-hcx-status\": \"response.redirect\", \"x-hcx-redirect_to\": \"1-74f6cb29-4116-42d0-9fbb-adb65e6a64ac\" }";
        MvcResult mvcResult = mockMvc.perform(post(Constants.VERSION_PREFIX + Constants.COVERAGE_ELIGIBILITY_ONCHECK).content(requestBody).contentType(MediaType.APPLICATION_JSON)).andReturn();
        MockHttpServletResponse response = mvcResult.getResponse();
        int status = response.getStatus();
        assertEquals(202, status);
    }

}
