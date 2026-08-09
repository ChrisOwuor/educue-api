package com.owuor.educue.auth;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;


import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class AuthCsrfIntegrationTest {
    @Autowired WebApplicationContext context;
    MockMvc mvc;

    @BeforeEach void setup() { mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build(); }

    @Test
    void loginAcceptsIssuedCsrfCookieAndHeader() throws Exception {
        var csrf = mvc.perform(get("/api/auth/csrf")).andExpect(status().isOk()).andReturn();
        Cookie cookie = csrf.getResponse().getCookie("XSRF-TOKEN");
        String token = java.net.URLDecoder.decode(cookie.getValue(), java.nio.charset.StandardCharsets.UTF_8);

        mvc.perform(post("/api/auth/login")
                        .cookie(cookie)
                        .header("X-XSRF-TOKEN", token)
                        .contentType("application/json")
                        .content("{\"email\":\"admin@educue.local\",\"password\":\"ChangeMe@123\"}"))
                .andExpect(status().isOk());
    }

//    @Test
//    void hodLoginCanSerializeLazyDepartmentAfterAuthentication() throws Exception {
//        var csrf = mvc.perform(get("/api/auth/csrf")).andExpect(status().isOk()).andReturn();
//        Cookie cookie = csrf.getResponse().getCookie("XSRF-TOKEN");
//        String token = java.net.URLDecoder.decode(cookie.getValue(), java.nio.charset.StandardCharsets.UTF_8);
//
//        mvc.perform(post("/api/auth/login")
//                        .cookie(cookie)
//                        .header("X-XSRF-TOKEN", token)
//                        .contentType("application/json")
//                        .content("{\"email\":\"hod.nursing@educue.local\",\"password\":\"ChangeMe@123\"}"))
//                .andExpect(status().isOk())
//                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.departmentName")
//                        .value("Nursing and Midwifery"));
//    }

//    @Test
//    void authenticatedHodCanLoadCoursesWithDepartmentNames() throws Exception {
//        var csrf = mvc.perform(get("/api/auth/csrf")).andExpect(status().isOk()).andReturn();
//        Cookie csrfCookie = csrf.getResponse().getCookie("XSRF-TOKEN");
//        String token = java.net.URLDecoder.decode(csrfCookie.getValue(), java.nio.charset.StandardCharsets.UTF_8);
//
//        var login = mvc.perform(post("/api/auth/login")
//                        .cookie(csrfCookie)
//                        .header("X-XSRF-TOKEN", token)
//                        .contentType("application/json")
//                        .content("{\"email\":\"hod.nursing@educue.local\",\"password\":\"ChangeMe@123\"}"))
//                .andExpect(status().isOk())
//                .andReturn();
//
//        Cookie accessToken = login.getResponse().getCookie("access_token");
//        mvc.perform(get("/api/courses").cookie(accessToken))
//                .andExpect(status().isOk())
//                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.content[0].departmentName").isNotEmpty());
//
//        mvc.perform(get("/api/units").cookie(accessToken))
//                .andExpect(status().isOk())
//                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.content[0].departmentName").isNotEmpty());
//    }
}
