package com.study.focus.notification.service;

import com.study.focus.account.domain.UserProfile;
import com.study.focus.account.repository.UserProfileRepository;
import com.study.focus.common.exception.BusinessException;
import com.study.focus.common.exception.CommonErrorCode;
import com.study.focus.common.service.GroupService;
import com.study.focus.notification.domain.AudienceType;
import com.study.focus.notification.domain.Notification;
import com.study.focus.notification.dto.GetNotificationDetailResponse;
import com.study.focus.notification.dto.GetNotificationsListResponse;
import com.study.focus.notification.repository.NotificationRepository;
import com.study.focus.study.domain.Study;
import com.study.focus.study.domain.StudyMember;
import com.study.focus.study.repository.StudyMemberRepository;
import com.study.focus.study.repository.StudyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final StudyMemberRepository studyMemberRepository;
    private final UserProfileRepository userProfileRepository;
    private final NotificationRepository notificationRepository;
    private final StudyRepository studyRepository;
    private final GroupService groupService;

    // 알림 목록 가져오기
    @Transactional
    public List<GetNotificationsListResponse> getNotifications(Long studyId, Long userId) {
        groupService.memberValidation(studyId,userId);
        Study study = studyRepository.findById(studyId).orElseThrow(() -> new BusinessException(CommonErrorCode.INVALID_REQUEST));
        List<Notification> notifications = notificationRepository.findAllByStudyOrderByCreatedAtDescIdDesc(study);
        return notifications.stream().map(a -> new GetNotificationsListResponse(a.getId(),a.getTitle(),a.getAudienceType())).toList();
    }

    // 알림 상세 데이터 가져오기
    @Transactional
    public GetNotificationDetailResponse getNotificationDetail(Long studyId, Long notificationId, Long userId) {
        groupService.memberValidation(studyId,userId);
        Study study = studyRepository.findById(studyId).orElseThrow(() -> new BusinessException(CommonErrorCode.INVALID_REQUEST));
        Notification notification = notificationRepository.findById(notificationId).orElseThrow(() -> new BusinessException(CommonErrorCode.INVALID_REQUEST));
        return GetNotificationDetailResponse.builder().createAt(notification.getCreatedAt()).title(notification.getTitle()).description(notification.getDescription()).build();
    }

    // 과제 알림 생성
    @Transactional
    public void addAssignmentNotification(Study study, Long actorId, String assignmentTitle){
        StudyMember actor = studyMemberRepository.findByStudyIdAndUserId(study.getId(),actorId).orElseThrow(() -> new BusinessException(CommonErrorCode.INVALID_REQUEST));
        String title = assignmentTitle + " 과제 생성 알림.";
        String description = "과제 게시판에 " + assignmentTitle + " 게시글이 생성되었습니다. 마감일 확인 후 마감일까지 제출 바랍니다.";

        Notification notification = Notification.builder().study(study).actor(actor)
                .audienceType(AudienceType.ALL_MEMBERS).title(title).description(description).build();

        notificationRepository.save(notification);
    }

    // 공지 알림 생성
    @Transactional
    public void addAnnouncementNotification(Study study, Long actorId, String announcementTitle){
        StudyMember actor = studyMemberRepository.findByStudyIdAndUserId(study.getId(),actorId).orElseThrow(() -> new BusinessException(CommonErrorCode.INVALID_REQUEST));
        String title = announcementTitle + " 공지 생성 알림";
        String description = "공지 게시판에 " + announcementTitle + " 게시글이 생성되었습니다. 스터디원들께서는 확인 바랍니다.";

        Notification notification = Notification.builder().study(study).actor(actor)
                .audienceType(AudienceType.ALL_MEMBERS).title(title).description(description).build();

        notificationRepository.save(notification);
    }

    // 새로운 회원 알림 생성
    @Transactional
    public void addNewMemberNotification(Study study, Long actorId){
        StudyMember actor = studyMemberRepository.findByStudyIdAndUserId(study.getId(),actorId).orElseThrow(() -> new BusinessException(CommonErrorCode.INVALID_REQUEST));
        UserProfile user = userProfileRepository.findByUser(actor.getUser()).orElseThrow(() -> new BusinessException(CommonErrorCode.INVALID_REQUEST));
        String username = user.getNickname();

        String title = username + "님이 새로 가입하였습니다!";
        String description = username + "님이 스터디에 새롭게 들어왔습니다! 모두 반갑게 맞아주세요~";

        Notification notification = Notification.builder().study(study).actor(actor)
                .audienceType(AudienceType.ALL_MEMBERS).title(title).description(description).build();

        notificationRepository.save(notification);
    }

    // 기존 회원 탈퇴 일림 생성
    @Transactional
    public void addOutMemberNotification(Study study, Long actorId){

        StudyMember actor = studyMemberRepository.findByStudyIdAndUserId(study.getId(),actorId).orElseThrow(() -> new BusinessException(CommonErrorCode.INVALID_REQUEST));
        UserProfile user = userProfileRepository.findByUser(actor.getUser()).orElseThrow(() -> new BusinessException(CommonErrorCode.INVALID_REQUEST));
        String username = user.getNickname();

        String title = "회원 탈퇴 알림";
        String description = username + "님이 스터디에서 탈퇴하셨습니다.";

        Notification notification = Notification.builder().study(study).actor(actor)
                .audienceType(AudienceType.ALL_MEMBERS).title(title).description(description).build();

        notificationRepository.save(notification);
    }

    // 신규 지원서 알림 생성
    @Transactional
    public void addNewApplicationNotification(Study study, Long actorId){

        StudyMember actor = studyMemberRepository.findByStudyIdAndUserId(study.getId(),actorId).orElseThrow(() -> new BusinessException(CommonErrorCode.INVALID_REQUEST));

        String title = "신규 지원서 알림";
        String description = "신규 회원이 승인 대기 중입니다. 그룹장께서는 확인 바랍니다.";

        Notification notification = Notification.builder().study(study).actor(actor)
                .audienceType(AudienceType.LEADER_ONLY).title(title).description(description).build();

        notificationRepository.save(notification);
    }
}
