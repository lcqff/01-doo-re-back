package doore.restdocs.docs;

import static doore.document.domain.DocumentAccessType.*;
import static doore.document.domain.DocumentType.IMAGE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.multipart;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestPartFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.partWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.restdocs.request.RequestDocumentation.requestParts;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import doore.document.application.dto.request.DocumentCreateRequest;
import doore.document.application.dto.request.DocumentUpdateRequest;
import doore.document.application.dto.response.DocumentCondensedResponse;
import doore.document.application.dto.response.DocumentDetailResponse;
import doore.document.application.dto.response.FileResponse;
import doore.document.domain.DocumentAccessType;
import doore.restdocs.RestDocsTest;
import java.io.FileInputStream;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockPart;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

public class DocumentDocsTest extends RestDocsTest {

    @Test
    @DisplayName("학습자료를 생성한다.")
    public void 학습자료를_생성한다() throws Exception {
        DocumentCreateRequest request = new DocumentCreateRequest("발표 자료", "이번주 발표자료입니다.",
                DocumentAccessType.TEAM, IMAGE, null, 1L);
        final MockPart mockPart = getMockPart("request", request);
        mockPart.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        final MockMultipartFile file = new MockMultipartFile(
                "files",
                "testImage.png",
                "image/png",
                new FileInputStream("src/test/resources/images/testImage.png")
        );

        mockMvc.perform(multipart("/{groupType}/{groupId}/documents", "teams", 1)
                        .part(mockPart)
                        .file(file)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andDo(print())
                .andExpect(status().isCreated())
                .andDo(document("document-create",
                        pathParameters(
                                parameterWithName("groupType").description("학습자료가 속한 그룹(teams/studies)"),
                                parameterWithName("groupId").description("학습자료가 속한 그룹 id")
                        ), requestParts(
                                partWithName("request").description("학습자료 정보"),
                                partWithName("files").description("첨부 파일들")
                        ), requestPartFields(
                                "request",
                                stringFieldWithPath("title", "학습자료 제목"),
                                stringFieldWithPath("description", "학습자료 설명"),
                                stringFieldWithPath("accessType", "학습자료 공개 범위"),
                                stringFieldWithPath("type", "자료 유형 (image, document, url)"),
                                stringFieldWithPath("url", "url(url 유형인 경우)").optional(),
                                numberFieldWithPath("uploaderId", "업로더 아이디")
                        )
                ));
    }

    @Test
    @DisplayName("학습자료 목록을 조회한다.")
    public void 학습자료_목록을_조회한다() throws Exception {
        //given
        DocumentCondensedResponse documentCondensedResponse =
                new DocumentCondensedResponse(1L, "학습자료1", "학습자료1 입니다.", LocalDate.parse("2020-02-02"), 1L);
        DocumentCondensedResponse otherDocumentCondensedResponse =
                new DocumentCondensedResponse(2L, "학습자료2", "학습자료2 입니다.", LocalDate.parse("2020-02-03"), 2L);
        Page<DocumentCondensedResponse> documentCondensedResponses = new PageImpl<>(
                List.of(documentCondensedResponse, otherDocumentCondensedResponse), PageRequest.of(0,4),2);
        //when
        when(documentQueryService.getAllDocument(any(), any(), any(PageRequest.class)))
                .thenReturn(documentCondensedResponses);

        //then
        final MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("page", "0");
        params.add("size", "4");
        mockMvc.perform(get("/{groupType}/{groupId}/documents", "teams", 1).params(params))
                .andExpect(status().isOk())
                .andDo(document("document-get-list", pathParameters(
                                parameterWithName("groupType").description("학습자료가 속한 그룹(teams/studies)"),
                                parameterWithName("groupId").description("학습자료가 속한 그룹 id")
                        ), queryParameters(
                                parameterWithName("page").description("페이지 (default: 0)"),
                                parameterWithName("size").description("한 페이지당 불러올 개수 (default: 4)")
                        )
                ));
    }

    @Test
    @DisplayName("학습자료를 조회한다.")
    public void 학습자료를_조회한다() throws Exception {
        //given
        FileResponse fileResponse = new FileResponse(1L, "s3 url");
        DocumentDetailResponse documentDetailResponse = DocumentDetailResponse.builder()
                .id(1L)
                .title("학습자료")
                .description("학습자료 입니다.")
                .accessType(ALL)
                .type(IMAGE)
                .files(List.of(fileResponse))
                .date(LocalDate.parse("2024-02-28"))
                .uploader("김땡땡")
                .build();

        //when
        when(documentQueryService.getDocument(any())).thenReturn(documentDetailResponse);

        //then
        mockMvc.perform(get("/{documentId}", 1))
                .andExpect(status().isOk())
                .andDo(document("document-get", pathParameters(
                                parameterWithName("documentId").description("학습자료 id")
                        ),
                        responseFields(
                                numberFieldWithPath("id", "학습자료 id"),
                                stringFieldWithPath("title", "학습자료 제목"),
                                stringFieldWithPath("description", "학습자료 소개"),
                                stringFieldWithPath("accessType", "학습자료 공개 범위(TEAM, ALL)"),
                                stringFieldWithPath("type", "학습자료 타입(IMAGE, DOCUMENT, URL)"),
                                arrayFieldWithPath("files", "학습자료 첨부파일"),
                                numberFieldWithPath("files[].id", "첨부파일 id"),
                                stringFieldWithPath("files[].url", "첨부파일 URL"),
                                stringFieldWithPath("date", "학습자료 업로드 날짜"),
                                stringFieldWithPath("uploader", "학습자료 업로더")
                        )
                ));
    }

    @Test
    @DisplayName("학습자료를 수정한다.")
    public void 학습자료를_수정한다() throws Exception {
        //given
        DocumentUpdateRequest request = new DocumentUpdateRequest("수정된 제목", "수정된 설명", TEAM);

        //then
        mockMvc.perform(put("/{documentId}", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andDo(document("document-update",
                        pathParameters(
                                parameterWithName("documentId").description("학습자료 id")
                        ),
                        requestFields(
                                stringFieldWithPath("title", "수정할 학습자료 제목"),
                                stringFieldWithPath("description", "수정할 학습자료 설명"),
                                stringFieldWithPath("accessType", "수정할 학습자료 공개 범위")
                        )
                ));
    }

    @Test
    @DisplayName("학습자료를 삭제한다.")
    public void 학습자료를_삭제한다() throws Exception {
        mockMvc.perform(delete("/{documentId}", 1))
                .andExpect(status().isNoContent())
                .andDo(document("document-delete", pathParameters(
                                parameterWithName("documentId").description("학습자료 id")
                        )
                ));
    }
}
