package doore.study.application;

import static doore.study.exception.CurriculumItemExceptionType.CANNOT_CREATE_CURRICULUM_ITEM;
import static doore.study.exception.CurriculumItemExceptionType.INVALID_ITEM_ORDER;
import static doore.study.exception.CurriculumItemExceptionType.NOT_FOUND_CURRICULUM_ITEM;
import static doore.study.exception.StudyExceptionType.NOT_FOUND_PARTICIPANT;
import static doore.study.exception.StudyExceptionType.NOT_FOUND_STUDY;

import doore.garden.domain.Garden;
import doore.garden.domain.GardenType;
import doore.garden.domain.repository.GardenRepository;
import doore.member.application.convenience.StudyRoleValidateAccessPermission;
import doore.member.domain.Participant;
import doore.member.domain.repository.ParticipantRepository;
import doore.study.application.dto.request.CurriculumItemManageDetailRequest;
import doore.study.application.dto.request.CurriculumItemManageRequest;
import doore.study.domain.CurriculumItem;
import doore.study.domain.ParticipantCurriculumItem;
import doore.study.domain.Study;
import doore.study.domain.repository.CurriculumItemRepository;
import doore.study.domain.repository.ParticipantCurriculumItemRepository;
import doore.study.domain.repository.StudyRepository;
import doore.study.exception.CurriculumItemException;
import doore.study.exception.StudyException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CurriculumItemCommandService {

    private final CurriculumItemRepository curriculumItemRepository;
    private final ParticipantCurriculumItemRepository participantCurriculumItemRepository;
    private final StudyRepository studyRepository;
    private final ParticipantRepository participantRepository;
    private final GardenRepository gardenRepository;

    private final StudyRoleValidateAccessPermission studyRoleValidateAccessPermission;

    public void manageCurriculum(final CurriculumItemManageRequest request, final Long studyId, final Long memberId) {
        studyRoleValidateAccessPermission.validateExistStudyLeader(studyId, memberId);
        final List<CurriculumItemManageDetailRequest> curriculumItems = request.curriculumItems();
        checkItemOrderDuplicate(curriculumItems);
        checkItemOrderRange(curriculumItems);
        createCurriculum(studyId, curriculumItems);
        updateCurriculum(curriculumItems);

        final List<CurriculumItemManageDetailRequest> deletedCurriculumItems = request.deletedCurriculumItems();
        deleteCurriculum(deletedCurriculumItems);
        sortCurriculum();
    }

    public void checkCurriculum(final Long curriculumId, final Long participantId, final Long memberId) {
        final Study study = studyRepository.findByCurriculumItemId(curriculumId);
        studyRoleValidateAccessPermission.validateExistParticipant(study.getId(), memberId);
        final CurriculumItem curriculumItem = getCurriculumItemOrThrow(curriculumId);
        final Participant participant = getParticipantOrThrow(participantId);
        final ParticipantCurriculumItem participantCurriculumItem = getParticipantCurriculumItemOrThrow(curriculumItem,
                participant);

        participantCurriculumItem.checkCompletion();
        handleGardenBasedOnCompletionStatus(participantCurriculumItem);
    }

    private void createGarden(final ParticipantCurriculumItem participantCurriculumItem) {
        final Garden garden = GardenType.getSupplierOf(participantCurriculumItem.getClass().getSimpleName())
                .of(participantCurriculumItem);
        gardenRepository.save(garden);
    }

    private void deleteGarden(final ParticipantCurriculumItem participantCurriculumItem) {
        final Long contributionId = participantCurriculumItem.getId();
        final GardenType gardenType = GardenType.getGardenTypeOf(participantCurriculumItem.getClass().getSimpleName());
        gardenRepository.deleteByContributionIdAndType(contributionId, gardenType);
    }

    private void handleGardenBasedOnCompletionStatus(final ParticipantCurriculumItem participantCurriculumItem) {
        if (participantCurriculumItem.getIsChecked()) {
            createGarden(participantCurriculumItem);
            return;
        }
        deleteGarden(participantCurriculumItem);
    }

    private void checkItemOrderDuplicate(final List<CurriculumItemManageDetailRequest> curriculumItems) {
        final Set<Integer> uniqueItemOrders = new HashSet<>();

        curriculumItems.stream()
                .map(CurriculumItemManageDetailRequest::itemOrder)
                .forEach(itemOrder -> {
                    if (!uniqueItemOrders.add(itemOrder)) {
                        throw new CurriculumItemException(INVALID_ITEM_ORDER);
                    }
                });
    }

    private void checkItemOrderRange(final List<CurriculumItemManageDetailRequest> curriculumItems) {
        curriculumItems.stream()
                .mapToInt(CurriculumItemManageDetailRequest::itemOrder)
                .forEach(itemOrder -> {
                    if (itemOrder < Integer.MIN_VALUE || itemOrder > Integer.MAX_VALUE) {
                        throw new CurriculumItemException(INVALID_ITEM_ORDER);
                    }
                });
    }

    private void createCurriculum(final Long studyId, final List<CurriculumItemManageDetailRequest> curriculumItems) {
        if (curriculumItemRepository.count() >= 99) {
            throw new CurriculumItemException(CANNOT_CREATE_CURRICULUM_ITEM);
        }
        curriculumItems.stream()
                .filter(curriculumItem -> !isExistsCurriculumItem(curriculumItem.id()))
                .forEach(curriculumItem -> createCurriculumItemAndAssignToParticipants(studyId, curriculumItem));
    }

    private boolean isExistsCurriculumItem(final Long curriculumItemId) {
        return curriculumItemRepository.existsById(curriculumItemId);
    }

    public void createCurriculumItemAndAssignToParticipants(final Long studyId,
                                                            final CurriculumItemManageDetailRequest curriculumItemRequest) {
        final Study study = getStudyOrThrow(studyId);
        final List<Participant> participants = participantRepository.findAllByStudyId(studyId);
        final CurriculumItem curriculumItems = createCurriculumItem(curriculumItemRequest, study);
        final List<ParticipantCurriculumItem> participantCurriculumItems = createParticipantCurriculumItems(
                curriculumItems, participants);
        participantCurriculumItemRepository.saveAll(participantCurriculumItems);
    }

    private CurriculumItem createCurriculumItem(final CurriculumItemManageDetailRequest request, final Study study) {
        return curriculumItemRepository.save(CurriculumItem.builder()
                .name(request.name())
                .itemOrder(request.itemOrder())
                .study(study)
                .build());
    }

    private List<ParticipantCurriculumItem> createParticipantCurriculumItems(final CurriculumItem curriculumItem,
                                                                             final List<Participant> participants) {
        return participants.stream()
                .map(participant -> ParticipantCurriculumItem.builder()
                        .curriculumItem(curriculumItem)
                        .participantId(participant.getId())
                        .build())
                .toList();
    }

    private void updateCurriculum(final List<CurriculumItemManageDetailRequest> curriculumItems) {
        for (final CurriculumItemManageDetailRequest requestItem : curriculumItems) {
            final CurriculumItem existingItem = getCurriculumItemOrThrow(requestItem.id());

            existingItem.updateIfNameDifferent(requestItem.name());
            existingItem.updateIfItemOrderDifferent(requestItem.itemOrder());
        }
    }

    private void deleteCurriculum(final List<CurriculumItemManageDetailRequest> deletedCurriculumItems) {
        deletedCurriculumItems.stream()
                .map(CurriculumItemManageDetailRequest::id)
                .forEach(curriculumItemId -> {
                    participantCurriculumItemRepository.deleteAllByCurriculumItemId(curriculumItemId);
                    curriculumItemRepository.deleteById(curriculumItemId);
                });
    }

    private void sortCurriculum() {
        final List<CurriculumItem> sortedCurriculum = curriculumItemRepository.findAllByOrderByItemOrderAsc();

        IntStream.range(0, sortedCurriculum.size())
                .forEach(i -> sortedCurriculum.get(i).updateItemOrder(i + 1));
        curriculumItemRepository.saveAll(sortedCurriculum);
    }

    private Study getStudyOrThrow(final Long studyId) {
        return studyRepository.findById(studyId).orElseThrow(() -> new StudyException(NOT_FOUND_STUDY));
    }

    private CurriculumItem getCurriculumItemOrThrow(final Long curriculumId) {
        return curriculumItemRepository.findById(curriculumId)
                .orElseThrow(() -> new CurriculumItemException(NOT_FOUND_CURRICULUM_ITEM));
    }

    private Participant getParticipantOrThrow(final Long participantId) {
        return participantRepository.findById(participantId)
                .orElseThrow(() -> new StudyException(NOT_FOUND_PARTICIPANT));
    }

    private ParticipantCurriculumItem getParticipantCurriculumItemOrThrow(final CurriculumItem curriculumItem,
                                                                          final Participant participant) {
        return participantCurriculumItemRepository.findByCurriculumItemIdAndParticipantId(
                curriculumItem.getId(), participant.getId()).orElseThrow();
    }
}
