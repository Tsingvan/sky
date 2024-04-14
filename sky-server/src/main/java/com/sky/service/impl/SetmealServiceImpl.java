package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.exception.SetmealEnableFailedException;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.SetmealService;
import com.sky.vo.DishItemVO;
import com.sky.vo.SetmealVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SetmealServiceImpl implements SetmealService {

    @Autowired
    private SetmealMapper setmealMapper;

    @Autowired
    private SetmealDishMapper setmealDishMapper;


    /**
     * 新增套餐信息
     * @param setmealDTO
     */
    @Override
    @Transactional
    public void insertWithDish(SetmealDTO setmealDTO) {

        /**
         * 新增套餐，同时需要保存套餐和菜品的关联关系
         * 插入套餐数据后根据套餐ID绑定套餐菜品
         * @param setmealDTO
         */
        //是否起售状态默认设置为禁售 需要单独设置
        setmealDTO.setStatus(StatusConstant.DISABLE);

        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);
        setmealMapper.insert(setmeal);

        //根据套餐ID关联相关菜品
        Long id = setmeal.getId();
        //获取到关联菜品 给他们赋值套餐ID
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        setmealDishes.forEach(setmealDish -> {
            setmealDish.setSetmealId(setmeal.getId());
        });
        //将这些菜品批量插入到套餐菜品表中
        setmealDishMapper.insertBatch(setmealDishes);

    }

    @Override
    public PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO) {

        PageHelper.startPage(setmealPageQueryDTO.getPage(),
                setmealPageQueryDTO.getPageSize());

        Page<SetmealVO> page = setmealMapper.query(setmealPageQueryDTO);

        return new PageResult(page.getTotal(), page.getResult());
    }

    /**
     * 套餐删除
     * 批量删除 要判断套餐是否在售 相对应的菜品也要根据套餐ID从stemeal_dish中删除
     * @param ids
     */
    @Override
    public void deleteBatch(List<Long> ids) {
        //先判断是否在售
        for (Long id: ids){
            //先根据ID查询数据 获取状态
            Setmeal setmeal = setmealMapper.getById(id);
            if (setmeal.getStatus() == StatusConstant.ENABLE){//起售中不能删除
                throw new DeletionNotAllowedException(MessageConstant.SETMEAL_ON_SALE);
            }
        }
        //删除套餐
        setmealMapper.deleteByIds(ids);

        //根据套餐ID删除相关联的菜品
        setmealDishMapper.deleteDishByIds(ids);
    }

    /**
     * 根据ID查询套餐，用于回显数据
     * @param id
     * @return
     */
    @Override
    public SetmealVO getByIdWithDish(Long id) {
        //根据ID查询套餐
        Setmeal setmeal = setmealMapper.getById(id);
        //根据ID查询套餐相关联的菜品
        List<SetmealDish> setmealDishes = setmealDishMapper.getDishBySetmealId(id);

        SetmealVO setmealVO = new SetmealVO();

        BeanUtils.copyProperties(setmeal, setmealVO);
        setmealVO.setSetmealDishes(setmealDishes);

        return setmealVO;
    }

    /**
     * 更新套餐信息
     * 先修改setmeal表的信息 再更新菜品 分两步
     * @param setmealDTO
     */
    @Override
    public void updateSetmealWithDish(SetmealDTO setmealDTO) {
        //将与套餐有关的信息先更新
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);
        //更新套餐相关信息
        setmealMapper.updateSetmeal(setmeal);

        //套餐关联的菜品 单独更新
        // 先删除原来和ID相关的菜品 再获取新的菜品更新
        setmealDishMapper.deleteDishById(setmealDTO.getId());

        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        //给获取的新菜品设置当前套餐的ID
        setmealDishes.forEach(setmealDish -> {
            setmealDish.setSetmealId(setmealDTO.getId());
        });

        //更新套餐关联的新菜品
        setmealDishMapper.insertBatch(setmealDishes);

    }

    /**
     * 起售停售套餐
     * 套餐中含有禁售菜品不能起售
     * @param status
     * @param id
     */
    @Override
    public void beginOrStopSal(Integer status, Long id) {
        //改成起售状态 起售时判断菜品是否禁售
        if(status == StatusConstant.ENABLE){
            //根据套餐ID查询菜品
            List<Dish> dishes = setmealMapper.getDishBySetmealId(id);

            //判断是否还有禁售菜品 有就抛出错误
            if (dishes != null && dishes.size() > 0){
                dishes.forEach(dish -> {
                    if (dish.getStatus() == StatusConstant.DISABLE){
                        throw new SetmealEnableFailedException(MessageConstant.SETMEAL_ENABLE_FAILED);
                    }
                });
            }
        }

        //更新状态
        Setmeal setmeal = Setmeal.builder()
                .id(id)
                .status(status)
                .build();
        setmealMapper.updateSetmeal(setmeal);
    }

    /**
     * 条件查询 根据类型ID和状态
     * @param setmeal
     * @return
     */
    @Override
    public List<Setmeal> getSetmealList(Setmeal setmeal) {
        List<Setmeal> setmeals = setmealMapper.getSetmealList(setmeal);
        return setmeals;
    }

    @Override
    public List<DishItemVO> getDishById(Long id) {

        return setmealMapper.getDishItemById(id);

    }
}
