package com.sky.mapper;

import com.sky.annotation.AutoFill;
import com.sky.entity.SetmealDish;
import com.sky.enumeration.OperationType;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SetmealDishMapper {
    List<Long> getSetmealIdByDishId(List<Long> ids);

    void insertBatch(List<SetmealDish> setmealDishes);

    /**
     * 根据ID集合删除套餐相关联的菜品
     * @param stemealIds
     */
    void deleteDishByIds(List<Long> stemealIds);

    @Select("select * from setmeal_dish where setmeal_id = #{id}")
    List<SetmealDish> getDishBySetmealId(Long id);

    @Delete("delete from setmeal_dish where setmeal_id = #{id}")
    void deleteDishById(Long id);
}
