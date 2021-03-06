package com.racing.service.user;

import com.racing.constant.UserConstant;
import com.racing.controller.vo.ApiResult;
import com.racing.controller.vo.UserPointsInfoVO;
import com.racing.controller.vo.manager.UserIdVO;
import com.racing.controller.vo.manager.UserInfoVo;
import com.racing.model.po.User;
import com.racing.model.po.UserAccountRecord;
import com.racing.model.po.WechatClient;
import com.racing.model.repo.MembersRepo;
import com.racing.model.repo.UserAccountRecordRepo;
import com.racing.model.repo.UserRepo;
import com.racing.model.repo.WechatClientRepo;
import com.racing.util.EncryptUtil;
import com.racing.util.PageUtil;
import jodd.util.StringUtil;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class UserService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserService.class);

    @Autowired
    UserRepo userRepo;
    @Autowired
    UserAccountRecordRepo userAccountRecordRepo;
    @Autowired
    WechatClientRepo wechatClientRepo;
    @Autowired
    MembersRepo membersRepo;


    public ApiResult getUserList(String nicName, Integer userId) {
        List<UserInfoVo> result = new ArrayList<UserInfoVo>();

        List<User> userList = userRepo.selectUser(nicName, userId);
        if (CollectionUtils.isNotEmpty(userList)) {
            for (User user : userList) {
                UserInfoVo vo = new UserInfoVo();
                try {
                    PropertyUtils.copyProperties(vo, user);
                } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                    LOGGER.error("拷贝bean出现异常！", e);
                }
                if (StringUtil.isNotEmpty(user.getClientSn())) {// 有微信机器人
                    vo.setIsHaveClient(true);
                    vo.setClientSn(user.getClientSn());
                    if (user.getClientExpireDate().compareTo(new Date()) > 0) {// 未过期
                        vo.setIsClientExpired(false);
                    } else {// 已过期
                        vo.setIsClientExpired(true);
                    }
                } else {// 无客户端
                    vo.setIsHaveClient(false);
                }
                result.add(vo);
            }
        }

        return ApiResult.createSuccessReuslt(result);
    }

    public ApiResult selectUserIdInfo() {
        List<UserIdVO> voList = new ArrayList<UserIdVO>();
        List<User> userList = userRepo.selectUser();
        if (CollectionUtils.isNotEmpty(userList)) {
            for (User user : userList) {
                UserIdVO idVO = new UserIdVO();
                idVO.setId(user.getId());
                idVO.setNickName(user.getNickName());
                idVO.setUserName(user.getUserName());
            }
        }
        return ApiResult.createSuccessReuslt(voList);
    }

    public ApiResult getUserPointsInfoList(String nickName, Integer userId, Integer page) {
        List<User> userList = userRepo.selectUser(nickName, userId, PageUtil.createPage(page, 15));
        int count = 0;
        List<UserPointsInfoVO> userPointsInfoList = new ArrayList<UserPointsInfoVO>();
        if (CollectionUtils.isNotEmpty(userList)) {
            for (User user : userList) {
                UserPointsInfoVO infoVO = new UserPointsInfoVO();
                infoVO.setUserId(user.getId());
                infoVO.setNickName(user.getNickName());
                infoVO.setUserName(user.getUserName());
                infoVO.setTotalPoints(user.getTotalPoints());
                infoVO.setUserPoints(user.getUserPoints());
                infoVO.setMembersPoints(user.getMembersPoints());
                userPointsInfoList.add(infoVO);
            }
            count = userRepo.count(nickName, userId);
        }
        return ApiResult.createSuccessReuslt(userPointsInfoList, page, 15, count);
    }

    @Transactional(rollbackFor = Exception.class)
    public Object updatePoint(Integer userId, BigDecimal points) {
        User user = userRepo.selectById(userId);
        if (user.getUserPoints().add(points).compareTo(BigDecimal.ZERO) == -1) {
            return ApiResult.createErrorReuslt("分数不够减");
        }
        int returnNum = userRepo.updatePoint(userId, points);
        if (returnNum == 0) {
            return ApiResult.createErrorReuslt("分数变更失败");
        }
        UserAccountRecord userAccountRecord = new UserAccountRecord();
        userAccountRecord.setUserId(userId);
        if (points.compareTo(BigDecimal.ZERO) >= 0) {
            userAccountRecord.setType(UserConstant.ACCOUNT_RECORD_TYPE_MANAGER_ADD);
        } else {
            userAccountRecord.setType(UserConstant.ACCOUNT_RECORD_TYPE_MANAGER_ADD);
        }
        userAccountRecord.setOperationTotalPoints(points);
        userAccountRecord.setResultTotalPoints(user.getTotalPoints());
        userAccountRecord.setOperationUserPoints(points);
        userAccountRecord.setResultUserPoints(user.getUserPoints());
        userAccountRecord.setOperationMembersPoints(BigDecimal.ZERO);
        userAccountRecord.setResultMembersPoints(user.getMembersPoints());
        userAccountRecord.setOperationTime(new Date());
        returnNum = userAccountRecordRepo.insert(userAccountRecord);
        if (returnNum == 0) {
            return ApiResult.createErrorReuslt("分数变更记录插入失败");
        }
        return ApiResult.createSuccessReuslt();
    }

    @Transactional(rollbackFor = Exception.class)
    public Object updateUserNickName(Integer userId, String nickName) {
        User user = userRepo.selectById(userId);
        if (null == user) {
            return ApiResult.createErrorReuslt("分盘不存在");
        }
        user.setNickName(nickName);
        int result = userRepo.updateUser(user);
        if (result == 1) {
            return ApiResult.createSuccessReuslt();
        }
        return ApiResult.createErrorReuslt("更新失败");
    }

    @Transactional(rollbackFor = Exception.class)
    public Object addUser(String nickName, String userName, String password) {
        User user = userRepo.getByUserName(userName);
        if (user != null) {
            return ApiResult.createErrorReuslt("此用户名已存在");
        }
        user = new User();
        user.setNickName(nickName);
        user.setUserName(userName);
        user.setPassword(EncryptUtil.md5AndSha1Upcase(password));
        user.setCreateTime(new Date());
        user.setIsEnable(false);
        user.setMembersPoints(BigDecimal.ZERO);
        user.setTotalPoints(BigDecimal.ZERO);
        user.setUserPoints(BigDecimal.ZERO);
        int result = userRepo.insert(user);
        if (result == 1) {
            return ApiResult.createSuccessReuslt();
        } else {
            return ApiResult.createErrorReuslt("创建失败");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public Object updateUserIsEnable(Integer userId, boolean isEnable) {
        User user = userRepo.selectById(userId);
        if (null == user) {
            return ApiResult.createErrorReuslt("分盘不存在");
        }
        user.setIsEnable(isEnable);
        int result = userRepo.updateUser(user);
        if (result > 0) {
            return ApiResult.createSuccessReuslt();
        }
        return ApiResult.createErrorReuslt("更新失败");
    }

    @Transactional(rollbackFor = Exception.class)
    public Object delete(Integer userId) {
        int result = userRepo.delete(userId);
        if (result == 1) {
            return ApiResult.createSuccessReuslt();
        }
        return ApiResult.createErrorReuslt("删除失败");
    }

    @Transactional(rollbackFor = Exception.class)
    public Object addRobot(Integer userId, String clientSn, Date clientExpireDate) {
        User user = userRepo.selectById(userId);
        if (user == null) {
            return ApiResult.createErrorReuslt("用户不存在");
        }
        user.setClientExpireDate(clientExpireDate);
        if (StringUtil.isNotEmpty(clientSn)) {
            user.setClientSn(clientSn);
            user.setClientIsEnable(true);
            WechatClient wechatClient = new WechatClient();
            wechatClient.setAppointStakeRate(new BigDecimal("9.7"));
            wechatClient.setRankingStakeRate(new BigDecimal("1.94"));
            wechatClient.setUpOrDownRate(new BigDecimal("1.94"));
            wechatClient.setFirstAddSecondOddRate(new BigDecimal("1.63"));
            wechatClient.setFirstAddSecondEvenRate(new BigDecimal("2"));
            wechatClient.setFirstAddSecondBigRate(new BigDecimal("2"));
            wechatClient.setFirstAddSecondSmallRate(new BigDecimal("1.63"));
            wechatClient.setFirstAddSecondAppoint3StakeRate(new BigDecimal("41"));
            wechatClient.setFirstAddSecondAppoint4StakeRate(new BigDecimal("41"));
            wechatClient.setFirstAddSecondAppoint5StakeRate(new BigDecimal("21"));
            wechatClient.setFirstAddSecondAppoint6StakeRate(new BigDecimal("21"));
            wechatClient.setFirstAddSecondAppoint7StakeRate(new BigDecimal("12"));
            wechatClient.setFirstAddSecondAppoint8StakeRate(new BigDecimal("12"));
            wechatClient.setFirstAddSecondAppoint9StakeRate(new BigDecimal("10.3"));
            wechatClient.setFirstAddSecondAppoint10StakeRate(new BigDecimal("10.3"));
            wechatClient.setFirstAddSecondAppoint11StakeRate(new BigDecimal("8.3"));
            wechatClient.setFirstAddSecondAppoint12StakeRate(new BigDecimal("10.3"));
            wechatClient.setFirstAddSecondAppoint13StakeRate(new BigDecimal("10.3"));
            wechatClient.setFirstAddSecondAppoint14StakeRate(new BigDecimal("12"));
            wechatClient.setFirstAddSecondAppoint15StakeRate(new BigDecimal("12"));
            wechatClient.setFirstAddSecondAppoint16StakeRate(new BigDecimal("21"));
            wechatClient.setFirstAddSecondAppoint17StakeRate(new BigDecimal("21"));
            wechatClient.setFirstAddSecondAppoint18StakeRate(new BigDecimal("41"));
            wechatClient.setFirstAddSecondAppoint19StakeRate(new BigDecimal("41"));
            wechatClient.setUserId(userId);
            wechatClientRepo.add(wechatClient);
        }
        int result = userRepo.updateUser(user);
        if (result == 1) {
            return ApiResult.createSuccessReuslt();
        } else {
            return ApiResult.createErrorReuslt("操作机器人失败");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public Object updateRobotIsEnable(Integer userId, boolean isEnable) {
        User user = userRepo.selectById(userId);
        if (null == user) {
            return ApiResult.createErrorReuslt("分盘不存在");
        }
        user.setClientIsEnable(isEnable);
        int result = userRepo.updateUser(user);
        if (result == 1) {
            return ApiResult.createSuccessReuslt();
        }
        return ApiResult.createErrorReuslt("更新失败");
    }

    @Transactional(rollbackFor = Exception.class)
    public Object updateUserNickNameAndPassword(Integer userId, String nickName, String password) {
        User user = userRepo.selectById(userId);
        if (null == user) {
            return ApiResult.createErrorReuslt("分盘不存在");
        }
        user.setNickName(nickName);
        user.setPassword(EncryptUtil.md5AndSha1Upcase(password));
        int result = userRepo.updateUser(user);
        if (result == 1) {
            return ApiResult.createSuccessReuslt();
        }
        return ApiResult.createErrorReuslt("更新失败");
    }

    public Object getUserPoint(Integer userId) {
        User user = userRepo.selectById(userId);
        if (null == user) {
            return ApiResult.createErrorReuslt("分盘不存在");
        }
        UserPointsInfoVO userPointsInfoVO = new UserPointsInfoVO();
        try {
            PropertyUtils.copyProperties(userPointsInfoVO, user);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        return ApiResult.createSuccessReuslt(userPointsInfoVO);
    }

    public Object getUser(Integer userId) {
        UserInfoVo vo = new UserInfoVo();

        User user = userRepo.selectById(userId);
        if (user != null) {
            try {
                PropertyUtils.copyProperties(vo, user);
            } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                LOGGER.error("拷贝bean出现异常！", e);
            }
            if (StringUtil.isNotEmpty(user.getClientSn())) {// 有微信机器人
                vo.setIsHaveClient(true);
                if (user.getClientExpireDate().compareTo(new Date()) > 0) {// 未过期
                    vo.setIsClientExpired(false);
                } else {// 已过期
                    vo.setIsClientExpired(true);
                }
            } else {// 无客户端
                vo.setIsHaveClient(false);
            }
        }
        return ApiResult.createSuccessReuslt(vo);
    }
}
