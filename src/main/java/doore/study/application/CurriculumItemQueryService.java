package doore.study.application;

import doore.study.application.dto.response.CurriculumItemResponse;
import doore.study.application.dto.response.PersonalCurriculumItemResponse;
import doore.study.domain.ParticipantCurriculumItem;
import doore.study.domain.repository.CurriculumItemRepository;
import doore.study.domain.repository.ParticipantCurriculumItemRepository;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CurriculumItemQueryService {
    private final CurriculumItemRepository curriculumItemRepository;
    private final ParticipantCurriculumItemRepository participantCurriculumItemRepository;

    public List<CurriculumItemResponse> getCurriculums(final Long studyId) {
        return CurriculumItemResponse.from(curriculumItemRepository.findAllByStudyIdOrderByItemOrderAsc(studyId));
    }

    public List<PersonalCurriculumItemResponse> getMyCurriculum(final Long studyId, final Long memberId) {
        final List<ParticipantCurriculumItem> participantCurriculumItems =
                participantCurriculumItemRepository.findAllByStudyIdAndMemberId(studyId, memberId);
        return participantCurriculumItems.stream()
                .map(PersonalCurriculumItemResponse::from)
                .sorted(Comparator.comparingInt(PersonalCurriculumItemResponse::itemOrder))
                .toList();
    }
}
