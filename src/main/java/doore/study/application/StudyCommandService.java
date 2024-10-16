package doore.study.application;

import static doore.study.exception.StudyExceptionType.INVALID_ENDDATE;
import static doore.study.exception.StudyExceptionType.NOT_FOUND_STATUS;

import doore.member.application.convenience.StudyRoleConvenience;
import doore.member.application.convenience.StudyRoleValidateAccessPermission;
import doore.member.application.convenience.TeamRoleValidateAccessPermission;
import doore.member.domain.Participant;
import doore.member.domain.repository.ParticipantRepository;
import doore.study.application.convenience.StudyAuthorization;
import doore.study.application.dto.request.StudyCreateRequest;
import doore.study.application.dto.request.StudyUpdateRequest;
import doore.study.domain.CurriculumItem;
import doore.study.domain.ParticipantCurriculumItem;
import doore.study.domain.Study;
import doore.study.domain.StudyStatus;
import doore.study.domain.repository.CurriculumItemRepository;
import doore.study.domain.repository.ParticipantCurriculumItemRepository;
import doore.study.domain.repository.StudyRepository;
import doore.study.exception.StudyException;
import doore.team.application.convenience.TeamAuthorization;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class StudyCommandService {
    private final StudyRepository studyRepository;
    private final CurriculumItemRepository curriculumItemRepository;
    private final ParticipantCurriculumItemRepository participantCurriculumItemRepository;
    private final ParticipantRepository participantRepository;
    private final ParticipantCommandService participantCommandService;
    private final StudyRoleConvenience studyRoleConvenience;
    private final StudyAuthorization studyAuthorization;
    private final TeamAuthorization teamAuthorization;

    private final TeamRoleValidateAccessPermission teamRoleValidateAccessPermission;
    private final StudyRoleValidateAccessPermission studyRoleValidateAccessPermission;

    public void createStudy(final StudyCreateRequest request, final Long teamId, final Long memberId) {
        teamRoleValidateAccessPermission.validateExistMemberTeam(teamId, memberId);
        teamAuthorization.validateExistTeam(teamId);
        checkEndDateValid(request.startDate(), request.endDate());
        final Study study = studyRepository.save(request.toStudy(teamId));
        studyRoleConvenience.assignStudyLeaderRole(study.getId(), memberId);
        saveParticipant(study.getId(), memberId, memberId);
    }

    private void saveParticipant(final Long studyId, final Long memberId, final Long studyLeaderId) {
        participantCommandService.saveParticipant(studyId, memberId, studyLeaderId);
    }

    private void checkEndDateValid(final LocalDate startDate, final LocalDate endDate) {
        if (endDate != null && startDate.isAfter(endDate)) {
            throw new StudyException(INVALID_ENDDATE);
        }
    }

    public void deleteStudy(final Long studyId, final Long memberId) {
        studyRoleValidateAccessPermission.validateExistStudyLeader(studyId, memberId);
        studyAuthorization.validateExistStudy(studyId);

        deleteCurriculumItemAndParticipantCurriculumItem(studyId);
        deleteParticipant(studyId);
        studyRepository.deleteById(studyId);
    }

    public void updateStudy(final StudyUpdateRequest request, final Long studyId, final Long memberId) {
        studyRoleValidateAccessPermission.validateExistStudyLeader(studyId, memberId);
        final Study study = studyAuthorization.getStudyOrThrow(studyId);
        study.update(request.name(), request.description(), request.startDate(), request.endDate(), request.status());
    }

    public void terminateStudy(final Long studyId, final Long memberId) {
        studyRoleValidateAccessPermission.validateExistStudyLeader(studyId, memberId);
        final Study study = studyAuthorization.getStudyOrThrow(studyId);
        study.terminate();
    }

    public void changeStudyStatus(final String status, final Long studyId, final Long memberId) {
        studyRoleValidateAccessPermission.validateExistStudyLeader(studyId, memberId);
        final Study study = studyAuthorization.getStudyOrThrow(studyId);
        try {
            final StudyStatus changedStatus = StudyStatus.valueOf(status);
            study.changeStatus(changedStatus);
        } catch (final IllegalArgumentException e) {
            throw new StudyException(NOT_FOUND_STATUS);
        }
    }

    private void deleteCurriculumItemAndParticipantCurriculumItem(final Long studyId) {
        final List<CurriculumItem> curriculumItems = curriculumItemRepository.findAllByStudyId(studyId);
        final List<Long> curriculumItemIds = curriculumItems.stream()
                .map(CurriculumItem::getId)
                .toList();

        curriculumItems.forEach(CurriculumItem::delete); // todo: 수료증 개발 시 delete 로직 확인 필요

        curriculumItemIds.forEach(curriculumItemId -> {
            final List<ParticipantCurriculumItem> items = participantCurriculumItemRepository.findAllByCurriculumItemId(
                    curriculumItemId);
            items.forEach(ParticipantCurriculumItem::delete); // todo: 수료증 개발 시 delete 로직 확인 필요
        });
    }

    private void deleteParticipant(final Long studyId) {
        final List<Participant> participants = participantRepository.findAllByStudyId(studyId);
        participantRepository.deleteAll(participants);
    }
}
