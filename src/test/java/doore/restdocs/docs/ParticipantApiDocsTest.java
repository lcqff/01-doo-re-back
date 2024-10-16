package doore.restdocs.docs;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import doore.member.domain.StudyRoleType;
import doore.restdocs.RestDocsTest;
import doore.study.application.dto.response.ParticipantResponse;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;

public class ParticipantApiDocsTest extends RestDocsTest {
    private String accessToken;

    @BeforeEach
    void setUp() {
        accessToken = "mocked-access-token";
        when(jwtTokenGenerator.generateToken(any(String.class))).thenReturn(accessToken);
    }

    @Test
    @DisplayName("참여자를 추가한다.")
    void 참여자를_추가한다_성공() throws Exception {
        mockMvc.perform(RestDocumentationRequestBuilders.post("/studies/{studyId}/members/{memberId}", 1, 1)
                        .header(HttpHeaders.AUTHORIZATION, accessToken))
                .andExpect(status().isCreated())
                .andDo(document("participant-save", pathParameters(
                                parameterWithName("studyId").description("스터디 id"),
                                parameterWithName("memberId").description("회원 id")
                        )
                ));
    }

    @Test
    @DisplayName("참여자를 삭제한다.")
    void 참여자를_삭제한다_성공() throws Exception {
        mockMvc.perform(RestDocumentationRequestBuilders.delete("/studies/{studyId}/members/{memberId}", 1, 1)
                        .header(HttpHeaders.AUTHORIZATION, accessToken))
                .andExpect(status().isNoContent())
                .andDo(document("participant-delete", pathParameters(
                                parameterWithName("studyId").description("스터디 id"),
                                parameterWithName("memberId").description("회원 id")
                        )
                ));
    }

    @Test
    @DisplayName("참여자가 탈퇴한다.")
    void 참여자가_탈퇴한다_성공() throws Exception {
        mockMvc.perform(RestDocumentationRequestBuilders.delete("/studies/{studyId}/members", 1)
                        .header(HttpHeaders.AUTHORIZATION, accessToken))
                .andExpect(status().isNoContent())
                .andDo(document("participant-withdraw",
                        pathParameters(parameterWithName("studyId").description("스터디 id")),
                        requestHeaders(headerWithName("Authorization").description("member id"))
                ));
    }

    @Test
    @DisplayName("참여자를 조회한다.")
    void 참여자를_조회한다_성공() throws Exception {
        final ParticipantResponse participantResponse = new ParticipantResponse(
                1L, "팜", "pom@gmail.com", "imageUrl", StudyRoleType.ROLE_스터디원);

        when(participantQueryService.findAllParticipants(any(), any())).thenReturn(List.of(participantResponse));

        mockMvc.perform(RestDocumentationRequestBuilders.get("/studies/{studyId}/members", 1)
                        .header(HttpHeaders.AUTHORIZATION, accessToken))
                .andExpect(status().isOk())
                .andDo(document("participant-get", pathParameters(
                        parameterWithName("studyId").description("스터디 id"))
                ));
    }
}
