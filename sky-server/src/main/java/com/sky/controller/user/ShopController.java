package com.sky.controller.user;

import com.sky.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

//因为admin中也有ShopController所以Bean的名称冲突 这里是为Bean取别名
@RestController("userShopController")
@Api(tags = "店铺营业状态接口")
@RequestMapping("user/shop")
@Slf4j
public class ShopController {

    private static final String KEY = "SHOP_STATUS";

    @Autowired
    private RedisTemplate redisTemplate;


    /**
     * 获取店铺营业状态
     * @return
     */
    @GetMapping("status")
    @ApiOperation("获取店铺营业状态")
    public Result<Integer> getStatus(){
        Integer status = (Integer) redisTemplate.opsForValue().get(KEY);
        log.info("店铺当前营业状态为：{}", status);
        return Result.success(status);
    }

}
