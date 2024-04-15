package com.sky.service.impl;

import com.sky.context.BaseContext;
import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.ShoppingCart;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.service.ShoppingCartService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ShoppingCartServiceImpl implements ShoppingCartService {

    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private SetmealMapper setmealMapper;

    /**
     * 添加购物车
     * @param shoppingCartDTO
     */
    @Override
    public void addShoppingCart(ShoppingCartDTO shoppingCartDTO) {
        ShoppingCart shoppingCart = new ShoppingCart();
        BeanUtils.copyProperties(shoppingCartDTO, shoppingCart);
        //只能查询当前用户的购物车数据 从token中获取到当前点餐用户的ID
        shoppingCart.setUserId(BaseContext.getCurrentId());

        //判断当前加入到购物车的商品或套餐是否已经存在
        //购物车中可以有很多数据 但是单个菜品或者套餐只有一个
        //这时查出来的是前端传进来的dishID或者setmealID和用户ID 来查出dish或setmeal
        //其实只有一条数据 和后面复用才用List
        List<ShoppingCart> shoppingCartlist = shoppingCartMapper.getShoppingCartlist(shoppingCart);

        if (shoppingCartlist != null && shoppingCartlist.size() > 0){
            //如果存在，只需要将数量+1
            //小程序端点击添加操作时，传的是dishID或者setmealID和用户ID
            shoppingCart = shoppingCartlist.get(0);// 取唯一的一条数据
            shoppingCart.setNumber(shoppingCart.getNumber() + 1);
            shoppingCartMapper.updateNumberById(shoppingCart);

        }else {
            //如果不存在，需要插入一条购物数据。先判断当前添加的是菜品还是套餐
            Long dishId = shoppingCartDTO.getDishId();
            if (dishId != null){//如果是菜品
                //先根据dishID获取当当前菜品，再根据shopping_cart需要的数据插入
                Dish dish = dishMapper.getById(dishId);
                shoppingCart.setName(dish.getName());
                shoppingCart.setImage(dish.getImage());
                shoppingCart.setAmount(dish.getPrice());
            }else {
                Long setmealId = shoppingCartDTO.getSetmealId();
                Setmeal setmeal = setmealMapper.getById(setmealId);
                shoppingCart.setName(setmeal.getName());
                shoppingCart.setImage(setmeal.getImage());
                shoppingCart.setAmount(setmeal.getPrice());
            }
            //通用的添加内容
            shoppingCart.setNumber(1);
            shoppingCart.setCreateTime(LocalDateTime.now());
            //插入数据
            shoppingCartMapper.insert(shoppingCart);

        }

    }

    /**
     * 查看购物车
     * @return
     */
    @Override
    public List<ShoppingCart> showShoppingCart() {
        //获取当前用户ID 根据用户ID查询购物车内容
        Long userId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = ShoppingCart.builder()
                .userId(userId)
                .build();
        List<ShoppingCart> list = shoppingCartMapper.getShoppingCartlist(shoppingCart);
        return list;
    }
}
