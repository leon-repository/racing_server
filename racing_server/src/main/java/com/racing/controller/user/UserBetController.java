package com.racing.controller.user;

import com.racing.service.manager.UserDayCountIncomeService;
import com.racing.service.manager.UserRacingIncomeService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

@RestController
@RequestMapping("/user")
public class UserBetController {
    @Autowired
    private UserDayCountIncomeService userDayCountIncomeService;
    @Autowired
    private UserRacingIncomeService userRacingIncomeService;

    @ApiOperation("分盘-押注报表-分盘押注报表-按日期")
    @RequestMapping(value = "/{userId}/bat/day", method = RequestMethod.GET)
    public Object selectByDate(@PathVariable Integer userId,
                               @RequestParam(required = false) Date startDate,
                               @RequestParam(required = false) Date endDate,
                               @RequestParam(required = false) Integer page) {
        return userDayCountIncomeService.selectByDate(userId, startDate, endDate, page);
    }

    @ApiOperation("分盘-押注报表-分盘押注报表-按期号")
    @RequestMapping(value = "/{userId}/bat/racing", method = RequestMethod.GET)
    public Object selectByRacing(@PathVariable Integer userId,
                                 @RequestParam(required = false) Date startDate,
                                 @RequestParam(required = false) Date endDate,
                                 @RequestParam(required = false) String racingNum,
                                 @RequestParam(required = false) Integer page) {
        return userRacingIncomeService.selectByRacingNum(userId, startDate, endDate, racingNum, page);
    }
}
