package com.racing.controller.members;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.racing.controller.vo.ApiResult;
import com.racing.controller.vo.MemberStakeVo;
import com.racing.service.members.StakeService;
import com.racing.util.LoginStatusSaveUtil;

@RestController
public class MemberStakeController {

	@Autowired
	private StakeService stakeService;

  @RequestMapping(value = "/member/stake", method = RequestMethod.POST)
  public Object memberStake(@RequestBody List<MemberStakeVo> stakeList) {
    Integer userId = LoginStatusSaveUtil.getUserClientId();
    if (userId == null) {
      return ApiResult.createNoLoginReuslt();
    }
    return stakeService.memberStake(userId, stakeList);
  }

}
