package doore.study.application;

import static doore.member.MemberFixture.미나;
import static doore.study.StudyFixture.algorithmStudy;
import static org.assertj.core.api.Assertions.assertThat;

import doore.helper.IntegrationTest;
import doore.member.domain.Member;
import doore.member.domain.Participant;
import doore.member.domain.StudyRole;
import doore.member.domain.StudyRoleType;
import doore.member.domain.repository.MemberRepository;
import doore.member.domain.repository.ParticipantRepository;
import doore.member.domain.repository.StudyRoleRepository;
import doore.study.application.dto.request.CurriculumItemManageDetailRequest;
import doore.study.application.dto.request.CurriculumItemManageRequest;
import doore.study.application.dto.response.PersonalCurriculumItemResponse;
import doore.study.domain.CurriculumItem;
import doore.study.domain.ParticipantCurriculumItem;
import doore.study.domain.Study;
import doore.study.domain.repository.CurriculumItemRepository;
import doore.study.domain.repository.ParticipantCurriculumItemRepository;
import doore.study.domain.repository.StudyRepository;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class CurriculumItemQueryServiceTest  extends IntegrationTest {
    @Autowired
    private CurriculumItemCommandService curriculumItemCommandService;
    @Autowired
    private CurriculumItemQueryService curriculumItemQueryService;
    @Autowired
    private CurriculumItemRepository curriculumItemRepository;
    @Autowired
    private StudyRepository studyRepository;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private ParticipantRepository participantRepository;
    @Autowired
    private ParticipantCurriculumItemRepository participantCurriculumItemRepository;
    @Autowired
    private StudyRoleRepository studyRoleRepository;

    private Study study;
    private CurriculumItem curriculumItem1;
    private CurriculumItem curriculumItem2;
    private CurriculumItem curriculumItem3;
    private CurriculumItemManageRequest request;
    private Long memberId;
    private Member member;


    @BeforeEach
    void setUp() {
        study = studyRepository.save(algorithmStudy());

        curriculumItem1 = CurriculumItem.builder().id(1L).itemOrder(1).name("Spring Study").study(study).build();
        curriculumItemRepository.save(curriculumItem1);
        curriculumItem2 = CurriculumItem.builder().id(2L).itemOrder(2).name("CS Study").study(study).build();
        curriculumItemRepository.save(curriculumItem2);
        curriculumItem3 = CurriculumItem.builder().id(3L).itemOrder(3).name("Infra Study").study(study).build();
        curriculumItemRepository.save(curriculumItem3);

        request = CurriculumItemManageRequest.builder()
                .curriculumItems(getCurriculumItems())
                .deletedCurriculumItems(getDeletedCurriculumItems())
                .build();
        member = memberRepository.save(미나());
        memberId = member.getId();

        StudyRole studyRole = studyRoleRepository.save(StudyRole.builder()
                .studyRoleType(StudyRoleType.ROLE_스터디장)
                .studyId(study.getId())
                .memberId(memberId)
                .build());
    }


    private List<CurriculumItemManageDetailRequest> getCurriculumItems() {
        final List<CurriculumItemManageDetailRequest> curriculumItems = new ArrayList<>();
        curriculumItems.add(
                CurriculumItemManageDetailRequest.builder().id(1L).itemOrder(1).name("Change Spring Study").build());
        curriculumItems.add(CurriculumItemManageDetailRequest.builder().id(2L).itemOrder(4).name("CS Study").build());
        curriculumItems.add(
                CurriculumItemManageDetailRequest.builder().id(3L).itemOrder(2).name("Infra Study").build());
        curriculumItems.add(
                CurriculumItemManageDetailRequest.builder().itemOrder(3).name("Algorithm Study").build());
        return curriculumItems;
    }

    private List<CurriculumItemManageDetailRequest> getDeletedCurriculumItems() {
        final List<CurriculumItemManageDetailRequest> deletedCurriculumItems = new ArrayList<>();
        deletedCurriculumItems.add(
                CurriculumItemManageDetailRequest.builder().id(3L).itemOrder(2).name("Infra Study").build());
        return deletedCurriculumItems;
    }

    @Test
    @DisplayName("[성공] 정상적으로 수정된 나의 커리큘럼을 조회할 수 있다.")
    public void getMyCurriculum_정상적으로_수정된_나의_커리큘럼을_조회할_수_있다() throws Exception {
        //given
        Participant participant = Participant.builder().member(member).studyId(study.getId()).build();
        participantRepository.save(participant);

        ParticipantCurriculumItem participantCurriculumItem1 = ParticipantCurriculumItem.builder()
                .participantId(participant.getId())
                .curriculumItem(curriculumItem1)
                .build();
        ParticipantCurriculumItem participantCurriculumItem2 = ParticipantCurriculumItem.builder()
                .participantId(participant.getId())
                .curriculumItem(curriculumItem2)
                .build();
        ParticipantCurriculumItem participantCurriculumItem3 = ParticipantCurriculumItem.builder()
                .participantId(participant.getId())
                .curriculumItem(curriculumItem3)
                .build();
        participantCurriculumItemRepository.saveAll(
                List.of(participantCurriculumItem1, participantCurriculumItem2, participantCurriculumItem3));

        curriculumItemCommandService.manageCurriculum(request, study.getId(), memberId);

        //when
        List<ParticipantCurriculumItem> items = participantCurriculumItemRepository.findAll();
        List<PersonalCurriculumItemResponse> responses = curriculumItemQueryService.getMyCurriculum(study.getId(),
                memberId);

        //then
        assertThat(responses).hasSize(items.size());
        System.out.println(responses);
        assertThat(responses.get(0).id()).isEqualTo(items.get(0).getId());
        assertThat(responses.get(1).id()).isEqualTo(items.get(1).getId());
        assertThat(responses.get(2).id()).isEqualTo(items.get(2).getId());
    }
}
