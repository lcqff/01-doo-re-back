package doore.study.application;

import static doore.member.domain.StudyRoleType.ROLE_스터디원;
import static doore.member.domain.StudyRoleType.ROLE_스터디장;
import static doore.member.exception.MemberExceptionType.NOT_FOUND_MEMBER;
import static doore.member.exception.MemberExceptionType.NOT_FOUND_MEMBER_ROLE_IN_STUDY;
import static doore.member.exception.MemberExceptionType.UNAUTHORIZED;
import static doore.study.exception.StudyExceptionType.NOT_FOUND_STUDY;

import doore.member.domain.Member;
import doore.member.domain.Participant;
import doore.member.domain.StudyRole;
import doore.member.domain.repository.MemberRepository;
import doore.member.domain.repository.ParticipantRepository;
import doore.member.domain.repository.StudyRoleRepository;
import doore.member.exception.MemberException;
import doore.study.domain.repository.StudyRepository;
import doore.study.exception.StudyException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class ParticipantCommandService {
    private final StudyRepository studyRepository;
    private final ParticipantRepository participantRepository;
    private final MemberRepository memberRepository;
    private final StudyRoleRepository studyRoleRepository;

    public void saveParticipant(Long studyId, Long memberId, Long studyLeaderId) {
        validateExistStudyLeader(studyLeaderId);
        validateExistStudy(studyId);
        Member member = validateExistMember(memberId);
        Participant participant = Participant.builder()
                .studyId(studyId)
                .member(member)
                .build();
        participantRepository.save(participant);
    }

    public void deleteParticipant(Long studyId, Long memberId, Long studyLeaderId) {
        validateExistStudyLeader(studyLeaderId);
        validateExistStudy(studyId);
        Member member = validateExistMember(memberId);
        participantRepository.deleteByStudyIdAndMember(studyId, member);
    }

    public void withdrawParticipant(Long studyId, Long memberId, Long participantId) {
        validateExistParticipant(participantId);
        validateExistStudy(studyId);
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(NOT_FOUND_MEMBER));
        participantRepository.deleteByStudyIdAndMember(studyId, member);
    }

    private void validateExistStudy(Long studyId) {
        studyRepository.findById(studyId).orElseThrow(() -> new StudyException(NOT_FOUND_STUDY));
    }

    private Member validateExistMember(Long memberId) {
        return memberRepository.findById(memberId).orElseThrow(() -> new MemberException(NOT_FOUND_MEMBER));
    }

    private void validateExistStudyLeader(Long memberId) {
        StudyRole studyRole = studyRoleRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(NOT_FOUND_MEMBER_ROLE_IN_STUDY));
        if (!studyRole.getStudyRoleType().equals(ROLE_스터디장)){
            throw new MemberException(UNAUTHORIZED);
        }
    }

    private void validateExistParticipant(Long memberId) {
        StudyRole studyRole = studyRoleRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(NOT_FOUND_MEMBER_ROLE_IN_STUDY));
        if (!studyRole.getStudyRoleType().equals(ROLE_스터디원)){
            throw new MemberException(UNAUTHORIZED);
        }
    }
}
