package com.sky.controller.admin;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("admin/dish")
@Slf4j
@Api(tags = "菜品相关接口")
public class DishController {

    @Autowired
    private DishService dishService;
    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 新增菜品
     * @param dishDTO
     * @return
     */
    @PostMapping
    @ApiOperation("新增菜品")
    public Result<DishDTO> save(@RequestBody DishDTO dishDTO){
        log.info("新增菜品：", dishDTO);
        dishService.saveWithFlavor(dishDTO);
        //清理缓存数据
        String key = "dish_" + dishDTO.getCategoryId();
        cleanCache(key);

        return Result.success();
    }

    /**
     * 菜品分页查询
     * @return
     */
    @GetMapping("page")
    @ApiOperation("菜品分页查询")
    public Result<PageResult> pageQuery(DishPageQueryDTO dishPageQueryDTO){//不需要@RequestBody 请求参数是query
        log.info("前端获取的查询菜品条件:{}", dishPageQueryDTO);
        PageResult pageResult = dishService.pageQuery(dishPageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 菜品批量删除
     * @param ids
     * @return
     */
    @DeleteMapping
    @ApiOperation("菜品批量删除")
    public Result delete(@RequestParam List<Long> ids){
        log.info("菜品批量删除:{}", ids);
        dishService.deleteBatch(ids);
        //清理缓存数据
        cleanCache("dish_*");
        return Result.success();
    }

    /**
     * 根据ID查询菜品
     * @param id
     * @return
     */
    @GetMapping("{id}")
    @ApiOperation("根据ID查询菜品")
    public Result<DishVO> getById(@PathVariable Long id){
        log.info("根据ID查询菜品:{}",id);
        DishVO dishVO = dishService.getByIdWithFlavor(id);
        return Result.success(dishVO);
    }

    /**
     * 更新菜品
     * @param dishDTO
     * @return
     */
    @PutMapping
    @ApiOperation("更新菜品")
    public Result update(@RequestBody DishDTO dishDTO){
        log.info("更新菜品:{}", dishDTO);
        dishService.updateWithFlavor(dishDTO);
        //清理缓存数据
        cleanCache("dish_*");
        return Result.success();
    }

    /**
     * 根据分类ID查询菜品
     * @param categoryId
     * @return
     */
    @GetMapping("list")
    @ApiOperation("根据分类ID查询菜品")
    public Result<List<Dish>> getDishByCategoryId(Long categoryId){
        log.info("根据分类ID查询菜品", categoryId);
        List<Dish> dishList = dishService.getDishByCategoryId(categoryId);
        return Result.success(dishList);
    }

    /**
     * 菜品起售停售
     * @param status
     * @param id
     * @return
     */
    @PostMapping("/status/{status}")
    @ApiOperation("菜品起售停售")
    public Result startOrStop(@PathVariable Integer status, Long id) {
        dishService.startOrStop(status, id);

        //将所有的菜品缓存数据清理掉，所有以dish_开头的key
        cleanCache("dish_*");

        return Result.success();
    }

    /**
     * 清理缓存
     * 需要改造的方法：
     * ● 新增菜品
     * ● 修改菜品
     * ● 批量删除菜品
     * ● 起售、停售菜品
     * @param pattern
     */
    public void cleanCache(String pattern){
        String keys = redisTemplate.keys(pattern).toString();
        redisTemplate.delete(keys);
    }
}
