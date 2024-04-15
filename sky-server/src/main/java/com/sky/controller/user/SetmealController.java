package com.sky.controller.user;

import com.sky.constant.StatusConstant;
import com.sky.entity.Setmeal;
import com.sky.result.Result;
import com.sky.service.SetmealService;
import com.sky.vo.DishItemVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController("userSetmealController")
@Api(tags = "C端-套餐浏览接口")
@RequestMapping("user/setmeal")
@Slf4j
public class SetmealController {

    @Autowired
    private SetmealService setmealService;

    /**
     * 根据分类ID查询套餐
     * @param categoryId
     * @return
     */
    @GetMapping("list")
    @ApiOperation("根据分类ID查询套餐")
    @Cacheable(cacheNames = "setmealCache", key = "#categoryId")
    public Result<List<Setmeal>> getSetmealList(Long categoryId){
        Setmeal setmeal = new Setmeal();
        setmeal.setId(categoryId);
        setmeal.setStatus(StatusConstant.ENABLE);

        List<Setmeal> setmeals = setmealService.getSetmealList(setmeal);

        return Result.success(setmeals);
    }

    /**
     * 根据套餐ID获取相应的菜品列表
     * @param id
     * @return
     */
    @GetMapping("dish/{id}")
    @ApiOperation("根据套餐ID获取相应的菜品列表")
    public Result<List<DishItemVO>> getDishList(@PathVariable Long id){
        List<DishItemVO> dishItemVOList = setmealService.getDishById(id);

        return Result.success(dishItemVOList);
    }

}
