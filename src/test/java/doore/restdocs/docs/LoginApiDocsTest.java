package doore.restdocs.docs;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import doore.login.application.dto.request.GoogleLoginRequest;
import doore.login.application.dto.response.LoginResponse;
import doore.restdocs.RestDocsTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.RequestFieldsSnippet;
import org.springframework.restdocs.payload.ResponseFieldsSnippet;
import org.springframework.restdocs.request.QueryParametersSnippet;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

public class LoginApiDocsTest extends RestDocsTest {

    @Test
    @DisplayName("구글 로그인을 한다.")
    public void 구글_로그인을_한다() throws Exception {
        //given
        final GoogleLoginRequest request = new GoogleLoginRequest("Authorization_Code");
        final LoginResponse response = new LoginResponse(3L, "doore.access.token");
        when(loginService.loginByGoogle(any()))
                .thenReturn(response);

        //when & then
        final QueryParametersSnippet quereyParameters = queryParameters(
                parameterWithName("code").description("구글에서 발급받은 인가 코드")
        );

        final ResponseFieldsSnippet responseFields = responseFields(
                numberFieldWithPath("memberId", "로그인한 사용자의 id"),
                stringFieldWithPath("token", "Access Token")
        );

        final MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("code", "google authorization code");
        mockMvc.perform(get("/login/google")
                .contentType(MediaType.APPLICATION_JSON).params(params)
                .content(asJsonString(request))).andExpect(status().isOk())
                .andDo(document("login-google", quereyParameters, responseFields));
    }
}
