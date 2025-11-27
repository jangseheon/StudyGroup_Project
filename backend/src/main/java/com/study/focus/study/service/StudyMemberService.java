package com.study.focus.study.service;

import com.study.focus.account.domain.User;
import com.study.focus.account.dto.GetMyProfileResponse;
import com.study.focus.account.service.UserService;
import com.study.focus.common.exception.BusinessException;
import com.study.focus.common.exception.CommonErrorCode;
import com.study.focus.common.exception.UserErrorCode;
import com.study.focus.common.service.GroupService;
import com.study.focus.notification.service.NotificationService;
import com.study.focus.study.domain.StudyMember;
import com.study.focus.study.domain.StudyMemberStatus;
import com.study.focus.study.domain.StudyRole;
import com.study.focus.study.dto.GetStudyMembersResponse;
import com.study.focus.study.dto.StudyMemberDto;
import com.study.focus.study.repository.StudyMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class StudyMemberService {

    private final StudyMemberRepository studyMemberRepository;
    private final NotificationService notificationService;
    private final GroupService groupService;
    private  final UserService userService;

    //스터디 멤버 목록 가져오기
    public GetStudyMembersResponse getMembers(Long studyId, Long userId) {
        //Validation
        groupService.memberValidation(studyId, userId);

        //Data set
        List<StudyMember> groupStudyMembers = studyMemberRepository.
                findAllByStudy_IdAndStatus(studyId, StudyMemberStatus.JOINED);
        List<GetMyProfileResponse> userProfileList = groupStudyMembers.stream().map(
                s -> userService.getMyProfile(s.getUser().getId())
        ).toList();

        List<StudyMemberDto> members = new ArrayList<>();
        IntStream.range(0,groupStudyMembers.size())
                .forEach(index ->{
                    GetMyProfileResponse userProfile = userProfileList.get(index);
                    StudyMember studyMember = groupStudyMembers.get(index);
                    User user = studyMember.getUser();
                    members.add(new StudyMemberDto(user.getId(),userProfile.getNickname(),
                            userProfile.getProfileImageUrl(),studyMember.getRole().name()
                            ,user.getLastLoginAt()));
                });

        //Data Return
        return new GetStudyMembersResponse(studyId,members);
    }

    // 그룹 탈퇴
    @Transactional
    public void leaveStudy(Long studyId, Long requestUserId) {
        // 스터디 멤버 확인
        StudyMember studyMember = studyMemberRepository.findByStudyIdAndUserIdAndStatus(studyId, requestUserId,StudyMemberStatus.JOINED)
                .orElseThrow(()->new BusinessException(CommonErrorCode.INVALID_PARAMETER));

        // 방장 탈퇴 불가 (위임 과정이 없기때문에)
        if(studyMember.getRole() == StudyRole.LEADER){
            throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
        }
        
        // 멤버 탈퇴 알림 생성
        notificationService.addOutMemberNotification(studyMember.getStudy(), studyMember.getUser().getId());
        
        // 멤버 상태 변경 (soft)
        studyMember.updateStatus(StudyMemberStatus.LEFT);
    }

    // 그룹 인원 추방하기 (방장)
    public void expelMember(Long studyId, Long expelUserId, Long requestUserId) {
        // 방장 권한 확인
        StudyMember leaderMember = studyMemberRepository.findByStudyIdAndRole(studyId, StudyRole.LEADER)
                .orElseThrow(()->new BusinessException(CommonErrorCode.INVALID_REQUEST));
        if(!leaderMember.getUser().getId().equals(requestUserId)){
            throw new BusinessException(UserErrorCode.URL_FORBIDDEN);
        }

        // 자기 추방 불가
        if(requestUserId.equals(expelUserId)){
            throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
        }

        // 추방대상이 그룹원인지 확인
        StudyMember memberExpel = studyMemberRepository.findByStudyIdAndUserIdAndStatus(studyId, expelUserId, StudyMemberStatus.JOINED)
                .orElseThrow(()-> new BusinessException(CommonErrorCode.INVALID_PARAMETER));

        // 멤버 추방
        memberExpel.updateStatus(StudyMemberStatus.BANNED);
        // 멤버 추방 알림 생성
        notificationService.addOutMemberNotification(leaderMember.getStudy(),memberExpel.getUser().getId());
    }
}