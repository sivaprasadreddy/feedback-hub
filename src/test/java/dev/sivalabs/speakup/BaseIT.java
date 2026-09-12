package dev.sivalabs.speakup;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Import(TestcontainersConfig.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Sql(scripts = "/test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
public abstract class BaseIT {
    @Autowired
    protected MockMvcTester mvc;

    protected MvcTestResult login(String email, String password) {
        return mvc.post()
                .uri("/login")
                .param("username", email)
                .param("password", password)
                .with(csrf())
                .exchange();
    }

    protected static MockHttpSession session(MvcTestResult result) {
        return (MockHttpSession) result.getMvcResult().getRequest().getSession(false);
    }
}
