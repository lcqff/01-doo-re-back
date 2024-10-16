package doore.team.application;

import static doore.member.domain.TeamRoleType.ROLE_팀원;
import static doore.member.domain.TeamRoleType.ROLE_팀장;
import static doore.member.exception.MemberExceptionType.ALREADY_JOIN_TEAM_MEMBER;
import static doore.member.exception.MemberExceptionType.NOT_FOUND_MEMBER;
import static doore.team.exception.TeamExceptionType.EXPIRED_LINK;
import static doore.team.exception.TeamExceptionType.NOT_FOUND_TEAM;
import static doore.team.exception.TeamExceptionType.NOT_MATCH_LINK;

import doore.file.application.S3ImageFileService;
import doore.member.application.convenience.TeamRoleValidateAccessPermission;
import doore.member.domain.Member;
import doore.member.domain.MemberTeam;
import doore.member.domain.Participant;
import doore.member.domain.TeamRole;
import doore.member.domain.repository.MemberRepository;
import doore.member.domain.repository.MemberTeamRepository;
import doore.member.domain.repository.ParticipantRepository;
import doore.member.domain.repository.TeamRoleRepository;
import doore.member.exception.MemberException;
import doore.study.domain.CurriculumItem;
import doore.study.domain.ParticipantCurriculumItem;
import doore.study.domain.Study;
import doore.study.domain.repository.CurriculumItemRepository;
import doore.study.domain.repository.ParticipantCurriculumItemRepository;
import doore.study.domain.repository.StudyRepository;
import doore.team.application.dto.request.TeamCreateRequest;
import doore.team.application.dto.request.TeamInviteCodeRequest;
import doore.team.application.dto.request.TeamUpdateRequest;
import doore.team.application.dto.response.TeamInviteCodeResponse;
import doore.team.domain.Team;
import doore.team.domain.TeamRepository;
import doore.team.exception.TeamException;
import doore.util.RandomUtil;
import doore.util.RedisUtil;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional
@RequiredArgsConstructor
public class TeamCommandService {

    private final TeamRepository teamRepository;
    private final MemberRepository memberRepository;
    private final TeamRoleRepository teamRoleRepository;
    private final StudyRepository studyRepository;
    private final CurriculumItemRepository curriculumItemRepository;
    private final ParticipantCurriculumItemRepository participantCurriculumItemRepository;
    private final MemberTeamRepository memberTeamRepository;
    private final ParticipantRepository participantRepository;
    private final S3ImageFileService s3ImageFileService;
    private final RedisUtil redisUtil;
    private final TeamRoleValidateAccessPermission teamRoleValidateAccessPermission;

    private static final String INVITE_LINK_PREFIX = "teamId=%d";

    public void createTeam(final TeamCreateRequest request, final MultipartFile file, final Long memberId) {
        // TODO: 팀 생성자를 팀 관리자로 등록 (2024/5/9 완료)
        Member member = validateExistMember(memberId);
        final String imageUrl = s3ImageFileService.upload(file);
        try {
            final Team team = Team.builder()
                    .name(request.name())
                    .description(request.description())
                    .imageUrl(imageUrl)
                    .build();
            teamRepository.save(team);

            createMemberTeam(member, team.getId());
            assignTeamLeaderRole(team.getId(), memberId);
        } catch (final Exception e) {
            s3ImageFileService.deleteFile(imageUrl);
        }
    }

    public void updateTeam(final Long teamId, final TeamUpdateRequest request, final Long memberId) {
        teamRoleValidateAccessPermission.validateExistTeamLeader(teamId, memberId);
        final Team team = validateExistTeam(teamId);
        team.update(request.name(), request.description());
    }

    public void updateTeamImage(final Long teamId, final MultipartFile file, final Long memberId) {
        teamRoleValidateAccessPermission.validateExistTeamLeader(teamId, memberId);
        final Team team = validateExistTeam(teamId);

        if (team.hasImage()) {
            final String beforeImageUrl = team.getImageUrl();
            s3ImageFileService.deleteFile(beforeImageUrl);
        }
        final String newImageUrl = s3ImageFileService.upload(file);
        team.updateImageUrl(newImageUrl);
    }

    public void deleteTeam(final Long teamId, final Long memberId) {
        teamRoleValidateAccessPermission.validateExistTeamLeader(teamId, memberId);
        final Team team = validateExistTeam(teamId);
        teamRepository.delete(team);
        if (team.hasImage()) {
            s3ImageFileService.deleteFile(team.getImageUrl());
        }
        deleteMemberTeamAndParticipant(teamId);
        deleteStudyAndCurriculumItemAndParticipantCurriculumItem(teamId);
    }

    private Team validateExistTeam(final Long teamId) {
        return teamRepository.findById(teamId)
                .orElseThrow(() -> new TeamException(NOT_FOUND_TEAM));
    }

    public TeamInviteCodeResponse generateTeamInviteCode(final Long teamId, final Long memberId) {
        validateExistTeam(teamId);
        teamRoleValidateAccessPermission.validateExistTeamLeader(teamId, memberId);

        final Optional<String> link = redisUtil.getData(INVITE_LINK_PREFIX.formatted(teamId), String.class);
        if (link.isEmpty()) {
            final String randomCode = RandomUtil.generateRandomCode('0', 'z', 10);
            redisUtil.setDataExpire(INVITE_LINK_PREFIX.formatted(teamId), randomCode, RedisUtil.toTomorrow());
            return new TeamInviteCodeResponse(randomCode);
        }
        return new TeamInviteCodeResponse(link.get());
    }

    public void joinTeam(final Long teamId, final TeamInviteCodeRequest request, final Long memberId) {
        validateExistTeam(teamId);
        Member member = validateExistMember(memberId);

        final Optional<String> link = redisUtil.getData(INVITE_LINK_PREFIX.formatted(teamId), String.class);

        if (link.isPresent()) {
            validateMatchLink(link.get(), request.code());
            // TODO: 2/14/24 권한 관련 작업이 추가되면 팀원으로 회원 추가, 이미 가입된 팀원이라면 예외 처리. (2024/7/3 완료)
            duplicateCheckTeamMember(teamId, memberId);
            assignTeamMemberRole(teamId, memberId);
            createMemberTeam(member, teamId);
        }
        throw new TeamException(EXPIRED_LINK);
    }

    private void validateMatchLink(final String link, final String userLink) {
        if (!link.equals(userLink)) {
            throw new TeamException(NOT_MATCH_LINK);
        }
    }

    private Member validateExistMember(final Long memberId) {
        return memberRepository.findById(memberId).orElseThrow(() -> new MemberException(NOT_FOUND_MEMBER));
    }

    private void duplicateCheckTeamMember(final Long teamId, final Long memberId) {
        if (memberTeamRepository.existsByTeamIdAndMemberId(teamId, memberId)) {
            throw new MemberException(ALREADY_JOIN_TEAM_MEMBER);
        }
    }

    private void assignTeamLeaderRole(final Long teamId, final Long memberId) {
        teamRoleRepository.save(TeamRole.builder()
                .teamId(teamId)
                .teamRoleType(ROLE_팀장)
                .memberId(memberId)
                .build());
    }

    private void assignTeamMemberRole(final Long teamId, final Long memberId) {
        teamRoleRepository.save(TeamRole.builder()
                .teamId(teamId)
                .teamRoleType(ROLE_팀원)
                .memberId(memberId)
                .build());
    }

    private void createMemberTeam(final Member member, final Long teamId) {
        memberTeamRepository.save(MemberTeam.builder()
                .member(member)
                .isDeleted(false)
                .teamId(teamId)
                .build());
    }

    private void deleteMemberTeamAndParticipant(final Long teamId) {
        final List<MemberTeam> memberTeams = memberTeamRepository.findAllByTeamId(teamId);
        memberTeamRepository.deleteAll(memberTeams);

        final List<Study> studies = studyRepository.findAllByTeamId(teamId);
        studies.forEach(study -> {
            final List<Participant> participants = participantRepository.findAllByStudyId(study.getId());
            participantRepository.deleteAll(participants);
        });
    }

    private void deleteStudyAndCurriculumItemAndParticipantCurriculumItem(final Long teamId) {
        final List<Study> studies = studyRepository.findAllByTeamId(teamId);

        studies.forEach(study -> {
            study.delete();
            final List<CurriculumItem> curriculumItems = curriculumItemRepository.findAllByStudyId(study.getId());

            curriculumItems.forEach(curriculumItem -> {
                curriculumItem.delete(); // todo: 수료증 개발 시 확인 필
                final List<ParticipantCurriculumItem> participantCurriculumItems = participantCurriculumItemRepository.findAllByCurriculumItemId(
                        curriculumItem.getId());
                participantCurriculumItems.forEach(ParticipantCurriculumItem::delete); // todo: 수료증 개발 시 delete 로직 확인 필요
            });
        });
    }
}
