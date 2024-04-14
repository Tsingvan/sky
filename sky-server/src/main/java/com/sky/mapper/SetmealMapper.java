package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.annotation.AutoFill;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.enumeration.OperationType;
import com.sky.vo.DishItemVO;
import com.sky.vo.SetmealVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SetmealMapper {

    /**
     * 新增套餐信息
     * @param setmeal
     */
    @AutoFill(OperationType.INSERT)
    void insert(Setmeal setmeal);

    /**
     * 分页查询 还要查询出关联的套餐分类，需要回显
     * @param setmealPageQueryDTO
     * @return
     */
    Page<SetmealVO> query(SetmealPageQueryDTO setmealPageQueryDTO);

    /**
     * 根据ID查询套餐
     * @param id
     * @return
     */
    @Select("select * from setmeal where id = #{id}")
    Setmeal getById(Long id);

    /**
     * 根据ID集合删除套餐
     * @param ids
     */
    void deleteByIds(List<Long> ids);

    void updateSetmeal(Setmeal setmeal);

    @Select("select * from dish d left join setmeal_dish sd on sd.dish_id = d.id where sd.setmeal_id = #{id}")
    List<Dish> getDishBySetmealId(Long id);


    List<Setmeal> getSetmealList(Setmeal setmeal);


    List<DishItemVO> getDishItemById(Long id);
}
