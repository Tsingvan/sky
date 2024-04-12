package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class DishServiceImpl implements DishService {

    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private DishFlavorMapper dishFlavorMapper;
    @Autowired
    private SetmealDishMapper setmealDishMapper;

    /**
     * 新增菜品 和对应的口味
     * @param dishDTO
     * @return
     */
    @Override
    @Transactional
    public void saveWithFlavor(DishDTO dishDTO) {
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);

        //向菜品里插入一条数据
        dishMapper.insert(dish);
        //插入数据后会有菜品主键值 后续插入口味时，口味和菜品ID匹配 xml中insert有对应的语法
        Long dishId = dish.getId();

        //向口味表里插入n条数据
        List<DishFlavor> flavors = dishDTO.getFlavors();//获取口味
        if (flavors != null && flavors.size() != 0){
            flavors.forEach(dishFlavor -> {
                dishFlavor.setDishId(dishId);//为所有的口味设置菜品ID
            });
            dishFlavorMapper.insertBatch(flavors);//批量插入
        }
    }

    /**
     * 菜品分页查询
     * @param dishPageQueryDTO
     * @return
     */
    @Override
    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO) {
        log.info("开始分页", dishPageQueryDTO);
        PageHelper.startPage(dishPageQueryDTO.getPage(), dishPageQueryDTO.getPageSize());

        //有个专门保存返回结果的类
        Page<DishVO> page = dishMapper.pageQuery(dishPageQueryDTO);

        return new PageResult(page.getTotal(), page.getResult());
    }

    /**
     * 菜品批量删除
     * @param ids
     * @return
     */
    @Override
    @Transactional
    public void deleteBatch(List<Long> ids) {
        //判断当前菜品是否能够删除——是否是起售菜品
        for (Long id: ids){
            Dish dish = dishMapper.getById(id);
            if (dish.getStatus() == StatusConstant.ENABLE){//在售菜品 不能删除
                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
            }
        }
        //判断当前菜品是否能够删除——是否和套餐关联
        List<Long> setmealId = setmealDishMapper.getSetmealIdByDishId(ids);
        if (setmealId != null && setmealId.size() != 0){// 有关联套餐
            throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
        }

        //删除菜品
//        for (Long id: ids){
//            dishMapper.deleteById(id);
//            //删除可删除菜品相关联的口味
//            dishFlavorMapper.deleteByDishId(id);
//        }

        //delete from dish where id in (?,?,?)
        //根据ID集合删除菜品
        dishMapper.deleteByIds(ids);

        //delete from dish_flavor where dish_id in (?,?,?)
        //根据ID集合删除菜品相关联的口味
        dishFlavorMapper.deleteByDishIds(ids);
    }

    /**
     * 根据ID查询菜品和口味
     * @param id
     * @return
     */
    @Override
    public DishVO getByIdWithFlavor(Long id) {
        //根据ID查菜品信息
        Dish dish = dishMapper.getById(id);
        //根据菜品ID查询口味
        List<DishFlavor> dishFlavors = dishFlavorMapper.getByDishId(id);
        //封装菜品信息和口味信息到DishVO
        DishVO dishVO = new DishVO();
        BeanUtils.copyProperties(dish, dishVO);
        dishVO.setFlavors(dishFlavors);

        return dishVO;
    }

    /**
     * 更新菜品基本信息和口味信息
     * @param dishDTO
     */
    @Override
    public void updateWithFlavor(DishDTO dishDTO) {
        //dishDTO包含口味属性 可以不用dishDTO使用dish更好
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);

        //修改菜品的基本信息
        dishMapper.update(dish);

        //修改口味
        //先根据菜品ID将原来的口味数据删除 再重新赋值
        dishFlavorMapper.deleteByDishId(dishDTO.getId());
        //重新插入口味值
        List<DishFlavor> dishFlavors = dishDTO.getFlavors();
        if (dishFlavors != null && dishFlavors.size() != 0){
            dishFlavors.forEach(dishFlavor -> {
                dishFlavor.setDishId(dishDTO.getId());//循环遍历每个口味列表 为每个口味列表设置相应的菜品ID
            });
            dishFlavorMapper.insertBatch(dishFlavors);
        }
    }

    /**
     * 根据分类ID查询菜品
     * @param categoryId
     * @return
     */
    @Override
    public List<Dish> getDishByCategoryId(Long categoryId) {
        //要确保菜品是在售的
        Dish dish = Dish.builder()
                .categoryId(categoryId)
                .status(StatusConstant.ENABLE)
                .build();
        List<Dish> dishes = dishMapper.getDish(dish);
        return dishes;
    }



}
