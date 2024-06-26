package doore.restdocs.docs;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import doore.restdocs.RestDocsTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;

public class MemberApiDocsTest extends RestDocsTest {
    private String accessToken;

    @BeforeEach
    void setUp() {
        accessToken = "mocked-access-token";
        when(jwtTokenGenerator.generateToken(any(String.class))).thenReturn(accessToken);
    }

    @Test
    @DisplayName("[성공] 유효한 요청이면 팀장 권한이 정상적으로 위임된다.")
    void transferTeamLeader_유효한_요청이면_팀장_권한이_정상적으로_위임된다() throws Exception {
        doNothing().when(memberCommandService).transferTeamLeader(any(), any(), any());

        mockMvc.perform(RestDocumentationRequestBuilders.patch("/teams/{teamId}/mandate/{newTeamLeaderId}", 1, 1)
                        .header(HttpHeaders.AUTHORIZATION, accessToken))
                .andExpect(status().isNoContent())
                .andDo(document("transfer-team-Leader", pathParameters(
                        parameterWithName("teamId").description("팀 id"),
                        parameterWithName("newTeamLeaderId").description("변경될 팀장 id"))));
    }

    @Test
    @DisplayName("[성공] 유효한 요청이면 스터디장 권한이 정상적으로 위임된다.")
    void transferStudyLeader_유효한_요청이면_스터디장_권한이_정상적으로_위임된다() throws Exception {
        doNothing().when(memberCommandService).transferStudyLeader(any(), any(), any());

        mockMvc.perform(RestDocumentationRequestBuilders.patch("/study/{studyId}/mandate/{newStudyLeaderId}", 1, 1)
                        .header(HttpHeaders.AUTHORIZATION, accessToken))
                .andExpect(status().isNoContent())
                .andDo(document("transfer-study-Leader", pathParameters(
                        parameterWithName("studyId").description("스터디 id"),
                        parameterWithName("newStudyLeaderId").description("변경될 스터디장 id"))));
    }

    @Test
    @DisplayName("[성공] 유효한 요청이면 회원 탈퇴에 성공한다.")
    void deleteMember_유효한_요청이면_회원_탈퇴에_성공한다() throws Exception {
        doNothing().when(memberCommandService).deleteMember(any());

        mockMvc.perform(RestDocumentationRequestBuilders.delete("/members")
                        .header(HttpHeaders.AUTHORIZATION, accessToken))
                .andExpect(status().isNoContent())
                .andDo(document("delete-member"));
    }

    //todo: docs code

    @Test
    @DisplayName("[성공] 프로필 이름 수정에 성공한다.")
    void updateMyPageName_프로필_이름_수정에_성공한다() throws Exception {
        String requestJson = "{\"newName\":\"요시\"}";
        doNothing().when(memberCommandService).updateMyPageName(any(), any());

        mockMvc.perform(RestDocumentationRequestBuilders.patch("/profile/name")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                        .header(HttpHeaders.AUTHORIZATION, accessToken))
                .andExpect(status().isNoContent())
                .andDo(document("update-my-page-name"));
    }
}
