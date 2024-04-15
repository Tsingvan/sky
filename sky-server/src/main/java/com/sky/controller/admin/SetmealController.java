package com.sky.controller.admin;

import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Setmeal;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.SetmealService;
import com.sky.vo.SetmealVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("admin/setmeal")
@RestController
@Api(tags = "套餐相关接口")
@Slf4j
public class SetmealController {

    @Autowired
    private SetmealService setmealService;

    /**
     * 套餐新增菜品
     * @param setmealDTO
     * @return
     */
    @PostMapping
    @ApiOperation("新增套餐信息")
    @CacheEvict(cacheNames = "setmealCache", key = "#setmealDTO.categoryId")
    public Result insert(@RequestBody SetmealDTO setmealDTO){
        log.info("新增套餐信息:{}", setmealDTO);
        setmealService.insertWithDish(setmealDTO);
        return Result.success();
    }

    @GetMapping("page")
    @ApiOperation("分页查询套餐数据")
    public Result<PageResult> pageQuery(SetmealPageQueryDTO setmealPageQueryDTO){
        log.info("分页查询套餐数据:{}", setmealPageQueryDTO);
        PageResult pageResult = setmealService.pageQuery(setmealPageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 套餐删除
     * @param ids
     * @return
     */
    @DeleteMapping
    @ApiOperation("套餐删除")
    @CacheEvict(cacheNames = "setmealCache", allEntries = true)
    public Result delete(@RequestParam List<Long> ids){
        log.info("套餐删除:{}", ids);
        setmealService.deleteBatch(ids);
        return Result.success();
    }

    /**
     * 根据ID查询套餐，用于回显数据
     * @param id
     * @return
     */
    @GetMapping("{id}")
    @ApiOperation("根据ID查询套餐，用于回显数据")
    public Result<SetmealVO> getById(@PathVariable Long id){
        log.info("根据ID查询套餐，用于回显数据:{}", id);
        SetmealVO setmealVO = setmealService.getByIdWithDish(id);
        return Result.success(setmealVO);
    }


    /**
     * 更新套餐信息
     * @param setmealDTO
     * @return
     */
    @PutMapping
    @ApiOperation("更新套餐信息")
    @CacheEvict(cacheNames = "setmealCache", allEntries = true)
    public Result update(@RequestBody SetmealDTO setmealDTO){
        log.info("更新套餐信息:{}", setmealDTO);

        setmealService.updateSetmealWithDish(setmealDTO);
        return Result.success();
    }

    /**
     * 起售停售套餐
     * @param status
     * @return
     */
    @PostMapping("status/{status}")
    @ApiOperation("起售停售套餐")
    @CacheEvict(cacheNames = "setmealCache", allEntries = true)
    public Result beginOrStopSal(@PathVariable Integer status, Long id){
        log.info("起售停售套餐:{}", id);
        setmealService.beginOrStopSal(status, id);
        return Result.success();
    }

}
