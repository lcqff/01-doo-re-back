package doore.member.application;

import static doore.member.MemberFixture.미나;
import static doore.member.MemberFixture.아마란스;
import static doore.member.domain.StudyRoleType.ROLE_스터디원;
import static doore.member.domain.StudyRoleType.ROLE_스터디장;
import static doore.member.domain.TeamRoleType.ROLE_팀원;
import static doore.member.domain.TeamRoleType.ROLE_팀장;
import static doore.member.exception.MemberExceptionType.NOT_FOUND_MEMBER;
import static doore.team.exception.TeamExceptionType.NOT_FOUND_TEAM;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

import doore.helper.IntegrationTest;
import doore.login.application.dto.response.GoogleAccountProfileResponse;
import doore.member.domain.Member;
import doore.member.domain.StudyRole;
import doore.member.domain.TeamRole;
import doore.member.domain.repository.MemberRepository;
import doore.member.domain.repository.StudyRoleRepository;
import doore.member.domain.repository.TeamRoleRepository;
import doore.member.exception.MemberException;
import doore.study.StudyFixture;
import doore.study.domain.Study;
import doore.study.domain.repository.StudyRepository;
import doore.team.TeamFixture;
import doore.team.domain.Team;
import doore.team.domain.TeamRepository;
import doore.team.exception.TeamException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class MemberCommandServiceTest extends IntegrationTest {
    @Autowired
    private MemberCommandService memberCommandService;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private TeamRepository teamRepository;
    @Autowired
    private StudyRepository studyRepository;
    @Autowired
    private TeamRoleRepository teamRoleRepository;
    @Autowired
    private StudyRoleRepository studyRoleRepository;

    private Member member;
    private Team team;
    private Study study;
    private TeamRole previousTeamLeaderRole;
    private StudyRole previousStudyLeaderRole;

    @BeforeEach
    void init() {
        member = memberRepository.save(아마란스());
        team = teamRepository.save(TeamFixture.team());
        study = studyRepository.save(StudyFixture.algorithmStudy());
        previousTeamLeaderRole = TeamRole.builder()
                .teamId(team.getId())
                .teamRoleType(ROLE_팀장)
                .memberId(member.getId())
                .build();
        teamRoleRepository.save(previousTeamLeaderRole);
        previousStudyLeaderRole = StudyRole.builder()
                .studyId(study.getId())
                .studyRoleType(ROLE_스터디장)
                .memberId(member.getId())
                .build();
        studyRoleRepository.save(previousStudyLeaderRole);
    }

    @Test
    @DisplayName("[성공] 이미 등록된 회원이 있다면 해당 회원을 반환한다")
    void findOrCreateMemberBy_이미_등록된_회원이_있다면_해당_회원을_반환한다_성공() {
        //given
        final long beforeCount = memberRepository.count();
        final GoogleAccountProfileResponse profile = new GoogleAccountProfileResponse(member.getGoogleId(),
                "bbb@gmail.com", true, "아마란스", "마란스", "아", "https://aaa", "ko");

        //when
        final Member actual = memberCommandService.findOrCreateMemberBy(profile);
        final long afterCount = memberRepository.count();

        //then
        assertAll(
                () -> assertThat(actual).usingRecursiveComparison()
                        .isEqualTo(member),
                () -> assertThat(afterCount).isEqualTo(beforeCount)
        );
    }

    @Test
    @DisplayName("[성공] 등록된 회원이 없다면 회원을 새로 추가하고 반환한다")
    void findOrCreateMemberBy_등록된_회원이_없다면_회원을_새로_추가하고_반환한다_성공() {
        //given
        final Long beforeCount = memberRepository.count();
        final String newMemberGoogleId = "4321";
        final GoogleAccountProfileResponse profile = new GoogleAccountProfileResponse(newMemberGoogleId,
                "ccc@gmail.com", true, "프리지아", "리지아", "프", "https://aaa", "ko");

        //when
        final Member newMember = memberCommandService.findOrCreateMemberBy(profile);
        final Long afterCount = memberRepository.count();

        //then
        assertAll(
                () -> assertThat(newMember.getGoogleId()).isEqualTo(newMemberGoogleId),
                () -> assertThat(afterCount).isEqualTo(beforeCount + 1)
        );
    }

    @Test
    @DisplayName("[성공] 팀장 직위가 정상적으로 위임된다")
    void transferTeamLeader_팀장_직위가_정상적으로_위임된다_성공() {
        Member newMember = memberRepository.save(아마란스());
        TeamRole teamRole = TeamRole.builder()
                .teamId(team.getId())
                .teamRoleType(ROLE_팀원)
                .memberId(newMember.getId())
                .build();
        teamRoleRepository.save(teamRole);

        memberCommandService.transferTeamLeader(team.getId(), newMember.getId(), member.getId());
        TeamRole changedTeamRole = teamRoleRepository.findTeamRoleByTeamIdAndMemberId(team.getId(), newMember.getId())
                .orElseThrow();

        assertThat(changedTeamRole.getTeamRoleType()).isEqualTo(ROLE_팀장);
    }

    @Test
    @DisplayName("[성공] 팀장 직위가 정상적으로 위임되면 원래 팀장은 팀원이 된다")
    void transferTeamLeader_팀장_직위가_정상적으로_위임되면_원래_팀장은_팀원이_된다_성공() {
        Member newMember = memberRepository.save(아마란스());
        TeamRole teamRole = TeamRole.builder()
                .teamId(team.getId())
                .teamRoleType(ROLE_팀원)
                .memberId(newMember.getId())
                .build();
        teamRoleRepository.save(teamRole);
        assertThat(previousTeamLeaderRole.getTeamRoleType()).isEqualTo(ROLE_팀장);

        memberCommandService.transferTeamLeader(team.getId(), newMember.getId(), member.getId());
        TeamRole changedTeamRole = teamRoleRepository.findTeamRoleByTeamIdAndMemberId(team.getId(), member.getId())
                .orElseThrow();

        assertThat(changedTeamRole.getTeamRoleType()).isEqualTo(ROLE_팀원);
    }

    @Test
    @DisplayName("[성공] 스터디장 직위가 정상적으로 위임된다")
    void transferStudyLeader_스터디장_직위가_정상적으로_위임된다_성공() {
        Member newMember = memberRepository.save(아마란스());
        StudyRole studyRole = StudyRole.builder()
                .studyId(study.getId())
                .studyRoleType(ROLE_스터디원)
                .memberId(newMember.getId())
                .build();
        studyRoleRepository.save(studyRole);

        memberCommandService.transferStudyLeader(study.getId(), newMember.getId(), member.getId());
        StudyRole changedStudyRole = studyRoleRepository.findStudyRoleByStudyIdAndMemberId(study.getId(),
                newMember.getId()).orElseThrow();

        assertThat(changedStudyRole.getStudyRoleType()).isEqualTo(ROLE_스터디장);
    }

    @Test
    @DisplayName("[성공] 스터디장 직위가 정상적으로 위임되면 원래 스터디장은 스터디원이 된다")
    void transferStudyLeader_스터디장_직위가_정상적으로_위임되면_원래_스터디장은_스터디원이_된다_성공() {
        Member newMember = memberRepository.save(아마란스());
        StudyRole studyRole = StudyRole.builder()
                .studyId(study.getId())
                .studyRoleType(ROLE_스터디원)
                .memberId(newMember.getId())
                .build();
        studyRoleRepository.save(studyRole);
        assertThat(previousStudyLeaderRole.getStudyRoleType()).isEqualTo(ROLE_스터디장);

        memberCommandService.transferStudyLeader(study.getId(), newMember.getId(), member.getId());
        StudyRole changedStudyRole = studyRoleRepository.findStudyRoleByStudyIdAndMemberId(study.getId(),
                member.getId()).orElseThrow();

        assertThat(changedStudyRole.getStudyRoleType()).isEqualTo(ROLE_스터디원);
    }

    @Test
    @DisplayName("[실패] 유효하지 않은 멤버라면 팀장 위임에 실패한다")
    void transferTeamLeader_유효하지_않은_멤버리면_팀장_위임에_실패한다_실패() throws Exception {
        Long invalidMemberId = 10L;
        Team team = TeamFixture.team();
        teamRepository.save(team);

        assertThatThrownBy(() -> {
            memberCommandService.transferTeamLeader(team.getId(), invalidMemberId, member.getId());
        }).isInstanceOf(MemberException.class).hasMessage(NOT_FOUND_MEMBER.errorMessage());
    }

    @Test
    @DisplayName("[실패] 유효하지 않은 팀이라면 팀장 위임에 실패한다")
    void transferTeamLeader_유효하지_않은_팀이리면_팀장_위임에_실패한다_실패() {
        Long invalidTeamId = 10L;

        assertThatThrownBy(() -> {
            memberCommandService.transferTeamLeader(invalidTeamId, member.getId(), member.getId());
        }).isInstanceOf(TeamException.class).hasMessage(NOT_FOUND_TEAM.errorMessage());
    }

    @Test
    @DisplayName("[실패] 팀장이 아닌 사람이 팀장 위임을 시도하면 실패한다")
    void transferTeamLeader_팀장이_아닌_사람이_팀장_위임을_시도하면_실패한다() {
        Member notTeamLeaderMember = memberRepository.save(미나());

        assertThatThrownBy(() -> {
            memberCommandService.transferTeamLeader(team.getId(), member.getId(), notTeamLeaderMember.getId());
        });
    }

    @Test
    @DisplayName("[실패] 스터디장이 아닌 사람이 스터디장 위임을 시도하면 실패한다")
    void transferStudyLeader_스터디장이_아닌_사람이_스터디장_위임을_시도하면_실패한다() {
        Member notStudyLeaderMember = memberRepository.save(미나());

        assertThatThrownBy(() -> {
            memberCommandService.transferStudyLeader(study.getId(), member.getId(), notStudyLeaderMember.getId());
        });
    }

    @Test
    @DisplayName("[성공] 프로필 이름 수정에 성공한다.")
    void updateMyPageName_프로필_이름_수정에_성공한다_성공() {
        String updateName = "요시";
        memberCommandService.updateMyPageName(member.getId(), updateName);

        Member findMember = memberRepository.findById(member.getId()).get();
        assertThat(findMember.getName()).isEqualTo(updateName);
    }
}
