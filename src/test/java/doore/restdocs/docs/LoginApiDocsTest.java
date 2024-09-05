package doore.restdocs.docs;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import doore.login.application.dto.request.GoogleLoginRequest;
import doore.login.application.dto.response.LoginResponse;
import doore.restdocs.RestDocsTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.RequestFieldsSnippet;
import org.springframework.restdocs.payload.ResponseFieldsSnippet;

public class LoginApiDocsTest extends RestDocsTest {

    @Test
    @DisplayName("구글 로그인을 한다.")
    public void 구글_로그인을_한다() throws Exception {
        //given
        final GoogleLoginRequest request = new GoogleLoginRequest("Redirect_Uri","Authorization_Code");
        final LoginResponse response = new LoginResponse(3L, "doore.access.token");
        when(loginService.loginByGoogle(any(GoogleLoginRequest.class)))
                .thenReturn(response);

        //when & then
        final RequestFieldsSnippet requestFields = requestFields(
                stringFieldWithPath("redirectUri", "리다이렉트 Uri"),
                stringFieldWithPath("code", "구글에서 발급받은 인가 코드")
        );

        final ResponseFieldsSnippet responseFields = responseFields(
                numberFieldWithPath("memberId", "로그인한 사용자의 id"),
                stringFieldWithPath("token", "Access Token")
        );

        mockMvc.perform(post("/login/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request))).andExpect(status().isOk())
                .andDo(document("login-google", requestFields, responseFields));
    }
}
