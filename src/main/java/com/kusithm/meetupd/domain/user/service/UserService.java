package com.kusithm.meetupd.domain.user.service;

import com.kusithm.meetupd.common.error.ConflictException;
import com.kusithm.meetupd.common.error.EntityNotFoundException;
import com.kusithm.meetupd.common.error.ForbiddenException;
import com.kusithm.meetupd.domain.user.dto.UserMypageResponseDto;
import com.kusithm.meetupd.domain.user.dto.request.BuyUserTicketRequestDto;
import com.kusithm.meetupd.domain.user.dto.request.SpendUserTicketRequestDto;
import com.kusithm.meetupd.domain.user.dto.request.UpdateUserAccountInfoRequestDto;
import com.kusithm.meetupd.domain.user.dto.request.UserProfileUpdateRequestDto;
import com.kusithm.meetupd.domain.user.dto.response.IsUserUseTicketResponseDto;
import com.kusithm.meetupd.domain.user.dto.response.SpendUserTicketResponseDto;
import com.kusithm.meetupd.domain.user.dto.response.UserCheckResponseDto;
import com.kusithm.meetupd.domain.user.dto.response.UserTicketCountResponseDto;
import com.kusithm.meetupd.domain.user.entity.User;
import com.kusithm.meetupd.domain.user.entity.UserTicketSpend;
import com.kusithm.meetupd.domain.user.mysql.UserRepository;
import com.kusithm.meetupd.domain.user.mysql.UserTicketSpendRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.kusithm.meetupd.common.error.ErrorCode.*;
import static com.kusithm.meetupd.domain.user.entity.UserTicketSpend.createUserTicketSpend;

@RequiredArgsConstructor
@Transactional
@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserTicketSpendRepository userTicketSpendRepository;

    public UserCheckResponseDto getUserByToken(Long userId) {
        User findUser = getUserByUserId(userId);

        return UserCheckResponseDto.builder()
                .userId(findUser.getId())
                .username(findUser.getUsername())
                .build();
    }

    public void updateUserAccountInfo(Long userId, UpdateUserAccountInfoRequestDto request) {
        User user = getUserByUserId(userId);
        user.updateUserProfile(
                request.getUsername(),
                request.getLocation(),
                request.getMajor(),
                request.getTask(),
                request.getSelfIntroduce());
    }

    public void updateUserProfile(Long userId, UserProfileUpdateRequestDto request) {
        User findUser = getUserByUserId(userId);
        updateUserProfile(findUser, request);
    }

    private void updateUserProfile(User user, UserProfileUpdateRequestDto request) {
        user.updateUserProfile(request.getInternships(), request.getAwards(), request.getTools(), request.getCertificates());
    }

    public UserMypageResponseDto getMypageUser(Long userId) {
        User findUser = getUserByUserId(userId);
        return new UserMypageResponseDto(findUser);
    }

    public UserTicketCountResponseDto buyUserTicket(Long userId, BuyUserTicketRequestDto request) {
        User findUser = getUserByUserId(userId);
        addUserTicket(findUser, request.getBuyAmount());
        return UserTicketCountResponseDto.of(findUser.getTicketCount());
    }

    public UserTicketCountResponseDto getUserTicketCount(Long userId) {
        User findUser = getUserByUserId(userId);
        return UserTicketCountResponseDto.of(findUser.getTicketCount());
    }

    public IsUserUseTicketResponseDto checkUserUseTicketToThisUser(Long userId, Long targetUserId) {
        validateUserExist(targetUserId);
        Boolean isSpend = checkUserUseTicketToTargetUser(userId, targetUserId);
        return IsUserUseTicketResponseDto.of(isSpend);
    }

    public SpendUserTicketResponseDto spendUserTicket(Long userId, SpendUserTicketRequestDto request) {
        User buyUser = getUserByUserId(userId);
        validateUserExist(request.getPurchaseUserId());
        validateUserNotSpendTicketToTargetUser(userId, request.getPurchaseUserId());
        // 유저 티켓 하나 이상인지 확인
        validateUserHaveOneMoreTicket(buyUser);
        useTicket(buyUser, request.getPurchaseUserId());
        return SpendUserTicketResponseDto.of(buyUser.getTicketCount());
    }

    private User getUserByUserId(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException(USER_NOT_FOUND));
    }

    private void addUserTicket(User user, Integer ticketAmount) {
        user.getTicket().addTicketCount(ticketAmount);
    }

    private Boolean checkUserUseTicketToTargetUser(Long userId, Long targetUserId) {
        return userTicketSpendRepository.existsByUserIdAndPurchaseUserId(userId, targetUserId);
    }

    private void validateUserNotSpendTicketToTargetUser(Long userId, Long targetUserId) {
        if(userTicketSpendRepository.existsByUserIdAndPurchaseUserId(userId, targetUserId)) {
            throw new ConflictException(ALREADY_USER_USE_TICKET);
        }
    }

    private void validateUserExist(Long purchaseUserId) {
        if(!userRepository.existsById(purchaseUserId)) {
            throw new EntityNotFoundException(USER_NOT_FOUND);
        }
    }

    private void validateUserHaveOneMoreTicket(User user) {
        if(user.getTicketCount() <= 0) {
            throw new ForbiddenException(USER_NOT_HAVE_ENOUGH_TICKET);
        }
    }

    private void useTicket(User user, Long purchaseUserId) {
        UserTicketSpend userTicketSpend = createUserTicketSpend(user.getId(), purchaseUserId);
        userTicketSpendRepository.save(userTicketSpend);
        user.getTicket().spendTicket();
    }

}
