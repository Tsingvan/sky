package com.sky.controller.admin;

import com.sky.dto.CategoryDTO;
import com.sky.dto.CategoryPageQueryDTO;
import com.sky.entity.Category;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.CategoryService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 分类管理
 */
@RequestMapping("admin/category")
@Api(tags = "分类接口相关")
@RestController
@Slf4j
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    /**
     * 新增分类
     * @param categoryDTO
     * @return
     */
    @PostMapping
    @ApiOperation("新增分类")
    public Result save(@RequestBody CategoryDTO categoryDTO){
        categoryService.save(categoryDTO);
        return Result.success();
    }

    /**
     * 分类分页查询
     * @return
     */
    @GetMapping("page")
    @ApiOperation("分类分页查询")
    public Result<PageResult> page(CategoryPageQueryDTO categoryPageQueryDTO){
        PageResult pageResult = categoryService.pageQuery(categoryPageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 根据ID删除分类
     * @param id
     * @return
     */
    @DeleteMapping
    @ApiOperation("根据ID删除分类")
    public Result deleteById(Long id){
        categoryService.deleteById(id);
        return Result.success();
    }

    /**
     * 修改分类
     * @param categoryDTO
     * @return
     */
    @PutMapping
    @ApiOperation("修改分类")
    public Result update(@RequestBody CategoryDTO categoryDTO){
        categoryService.update(categoryDTO);
        return Result.success();
    }

    /**
     * 启用、禁用分类
     * @param status
     * @return
     */
    @PostMapping("status/{status}")
    @ApiOperation("启用、禁用分类")
    public Result startOrStop(@PathVariable Integer status, Long id){
        categoryService.startOrStop(status, id);
        return Result.success();
    }

    /**
     * 根据类型查询
     * @param type
     * @return
     */
    @GetMapping("list")
    @ApiOperation("根据类型查询")
    public Result<List<Category>> list(Integer type){
         List<Category> list = categoryService.list(type);
         return Result.success(list);
    }

}
