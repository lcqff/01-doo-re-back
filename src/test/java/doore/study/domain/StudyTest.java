package doore.study.domain;

import static doore.study.StudyFixture.algorithmStudy;
import static org.junit.jupiter.api.Assertions.assertEquals;

import doore.study.application.dto.request.StudyUpdateRequest;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class StudyTest {

    @Test
    @DisplayName("커리큘럼을 생성할 수 있다.")
    public void 커리큘럼을_생성할_수_있다_성공() {
        final Study study = algorithmStudy();
        final CurriculumItem curriculumItem = CurriculumItem.builder()
                .name("커리큘럼 1단계")
                .itemOrder(1)
                .study(study)
                .build();
        final CurriculumItem otherCurriculumItem = CurriculumItem.builder()
                .name("커리큘럼 2단계")
                .itemOrder(2)
                .study(study)
                .build();
        final List<CurriculumItem> curriculumItems = List.of(curriculumItem, otherCurriculumItem);

        study.createCurriculumItems(curriculumItems);
        assertEquals(curriculumItems, study.getCurriculumItems());
    }

    @Test
    @DisplayName("스터디의 내용을 변경할 수 있다.")
    public void 스터디의_내용을_변경할_수_있다_성공() {
        final Study study = algorithmStudy();
        final StudyUpdateRequest request = StudyUpdateRequest.builder()
                .name("스프링")
                .description("스프링 스터디 입니다.")
                .startDate(LocalDate.parse("2023-01-01"))
                .endDate(LocalDate.parse("2024-01-01"))
                .status(StudyStatus.IN_PROGRESS)
                .build();
        study.update(request.name(), request.description(), request.startDate(), request.endDate(), request.status());
        assertEquals(study.getName(), request.name());
        assertEquals(study.getDescription(), request.description());
    }

}
