package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.*;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.utils.HttpClientUtil;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import com.sky.websocket.WebSocketServer;
import io.swagger.util.Json;
import lombok.extern.java.Log;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.beans.factory.annotation.Value;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OrderServiceImpl implements OrderService {

    @Value("${sky.shop.address}")
    private String shopAddress;

    @Value("${sky.baidu.ak}")
    private String ak;

    @Autowired
    private AddressBookMapper addressBookMapper;
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private WeChatPayUtil weChatPayUtil;
    @Autowired
    private WebSocketServer webSocketServer;

    /**
     * 用户下单
     * @param ordersSubmitDTO
     * @return
     */
    @Override
    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO) {
        //异常情况处理：收货地址为空，购物车为空
        //查询收获地址是否为空
        Long addressBookId = ordersSubmitDTO.getAddressBookId();
        AddressBook addressBook = addressBookMapper.getAddressById(addressBookId);
        if (addressBook == null){
            throw  new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }

        //检查用户收货地址是否超出配送范围
        //checkOutOfRange(addressBook.getCityName() + addressBook.getDistrictName() + addressBook.getDetail());

        //查询购物车数据是否为空 OrdersSubmitDTO中没有相关属性 通过当前用户ID查找
        Long userId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = new ShoppingCart();
        shoppingCart.setUserId(userId);
        List<ShoppingCart> shoppingCartlist = shoppingCartMapper.getShoppingCartlist(shoppingCart);

        if (shoppingCartlist == null && shoppingCartlist.size() == 0){
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }

        //构造订单数据
        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO, orders);
        orders.setOrderTime(LocalDateTime.now());
        orders.setAddress(addressBook.getDetail());
        orders.setPhone(addressBook.getPhone());
        orders.setConsignee(addressBook.getConsignee());//收货人
        orders.setUserId(userId);
        orders.setNumber(String.valueOf(System.currentTimeMillis()));//订单号 使用时间戳 转成String类型
        orders.setPayStatus(Orders.UN_PAID);
        orders.setStatus(Orders.PENDING_PAYMENT);

        //向订单表中插入一条订单数据 数据库操作中需要返回订单的ID
        orderMapper.insert(orders);

        //向订单明细表中插入购物车中的数据n条
        List<OrderDetail> orderDetailList = new ArrayList<>();
        for (ShoppingCart cart: shoppingCartlist){//遍历购物车的所有数据
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(cart, orderDetail);
            orderDetail.setOrderId(orders.getId());//设置订单ID
            orderDetailList.add(orderDetail);
        }
        //向明细表中批量插入n条数据
        orderDetailMapper.insertBatch(orderDetailList);

        //插入成功后删除购物车中的数据
        shoppingCartMapper.cleanShoppingCart(userId);

        //封装结果对象返回
        OrderSubmitVO orderSubmitVO = OrderSubmitVO.builder()
                .id(orders.getId())
                .orderAmount(orders.getAmount())
                .orderNumber(orders.getNumber())
                .orderTime(orders.getOrderTime())
                .build();

        return orderSubmitVO;
    }

    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    @Override
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO){
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.getByOpenid(String.valueOf(userId));

        //获取订单 用于后续的语音播报
        Orders orders = orderMapper.getByNumberAndUserId(ordersPaymentDTO.getOrderNumber(), userId);

        //调用微信支付接口，生成预支付交易单
//        JSONObject jsonObject = weChatPayUtil.pay(
//                ordersPaymentDTO.getOrderNumber(), //商户订单号
//                new BigDecimal(0.01), //支付金额，单位 元
//                "苍穹外卖订单", //商品描述
//                user.getOpenid() //微信用户的openid
//        );
//
//        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
//            throw new OrderBusinessException("该订单已支付");
//        }

        JSONObject jsonObject = new JSONObject();

        jsonObject.put("code", "ORDERPAID");
        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);

        vo.setPackageStr(jsonObject.getString("package"));
        //为替代微信支付成功后的数据库订单状态更新，多定义一个方法进行修改

        Integer OrderPaidStatus = Orders.PAID; //支付状态，已支付

        Integer OrderStatus = Orders.TO_BE_CONFIRMED;  //订单状态，待接单

        //发现没有将支付时间 check_out属性赋值，所以在这里更新

        LocalDateTime check_out_time = LocalDateTime.now();

        orderMapper.updateStatus(OrderStatus, OrderPaidStatus, check_out_time, ordersPaymentDTO.getOrderNumber());

        //////////////////////////////////////////////
        Map map = new HashMap();
        map.put("type", 1);//消息类型，1表示来单提醒
        map.put("orderId", orders.getId());
        map.put("content", "订单号：" + ordersPaymentDTO.getOrderNumber());

        //通过WebSocket实现来单提醒，向客户端浏览器推送消息
        webSocketServer.sendToAllClient(JSON.toJSONString(map));
        ///////////////////////////////////////////////////


        return vo;
    }


    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
    @Override
    public void paySuccess(String outTradeNo) {
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();

        // 根据订单号查询当前用户的订单
        Orders ordersDB = orderMapper.getByNumberAndUserId(outTradeNo, userId);

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        orderMapper.update(orders);

        /////////////////////////////
//        Map map = new HashMap();
//        map.put("type", 1);
//        map.put("orderId", ordersDB.getId());
//        map.put("content", "订单号" + outTradeNo);
//
//        String jsonString = JSON.toJSONString(map);
//        webSocketServer.sendToAllClient(jsonString);

    }

    /**
     * 分页查询历史订单
     * @param page
     * @param pagesize
     * @param status
     * @return
     */
    @Override
    public PageResult page(int page, int pagesize, Integer status) {
        //设置分页
        PageHelper.startPage(page, pagesize);

        OrdersPageQueryDTO ordersPageQueryDTO = new OrdersPageQueryDTO();
        ordersPageQueryDTO.setUserId(BaseContext.getCurrentId());
        ordersPageQueryDTO.setStatus(status);

        //分页查询条件 返回的是Order类型数据
        Page<Orders> ordersPage = orderMapper.pageQuery(ordersPageQueryDTO);

        List<OrderVO> list = new ArrayList<>();
        //查询出订单明细，并封装入OrderVO进行响应
        if (ordersPage != null && ordersPage.getTotal() > 0){
            for (Orders orders : ordersPage){
                Long id = orders.getId();//订单ID

                //查询订单明细
                List<OrderDetail> orderDetail = orderDetailMapper.getByOrderId(id);

                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders, orderVO);
                orderVO.setOrderDetailList(orderDetail);

                list.add(orderVO);
            }
        }

        return new PageResult(ordersPage.getTotal(), list);
    }

    /**
     * 查询订单详情
     * @param id
     * @return
     */
    @Override
    public OrderVO details(Long id) {
        //根据订单ID获取订单
        Orders orders = orderMapper.getById(id);

        //获取订单详情
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orders.getId());

        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(orders, orderVO);
        orderVO.setOrderDetailList(orderDetailList);

        return orderVO;
    }

    /**
     * 取消订单
     * @param id
     */
    @Override
    public void userCancel(Long id) {
        //根据ID查询订单
        Orders ordersDB = orderMapper.getById(id);
        //校验当前订单是否存在
        if (ordersDB == null){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        //判断当前订单状态是否可以退款
        //1待付款 2待接单 3已接单 4派送中 5已完成 6已取消
        if (ordersDB.getStatus() > 2){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        //修改订单状态
        Orders orders = new Orders();
        orders.setId(ordersDB.getId());

        // 订单处于待接单状态下取消，需要进行退款
        if (ordersDB.getStatus().equals(Orders.TO_BE_CONFIRMED)){
            //调用微信支付退款接口
            //略

            //支付状态修改为 退款
            orders.setPayStatus(Orders.REFUND);
        }

        //修改订单状态、取消原因、取消时间
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelReason("用户取消");
        orders.setCancelTime(LocalDateTime.now());

        orderMapper.update(orders);

    }

    /**
     * 再来一单
     * 再来一单就是将原订单中的商品重新加入到购物车中
     * @param id
     */
    @Override
    public void repetition(Long id) {
        //查询当前用户ID
        Long userId = BaseContext.getCurrentId();
        //根据订单ID获取订单详情
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(id);

        //将订单详情转换为购物车对象
        List<ShoppingCart> shoppingCartList = orderDetailList.stream().map(x ->{
            ShoppingCart shoppingCart = new ShoppingCart();
            //将原订单详情里面的菜品信息重新复制到购物车对象中
            BeanUtils.copyProperties(x, shoppingCart, "id");//忽略掉ID
            shoppingCart.setUserId(userId);
            shoppingCart.setCreateTime(LocalDateTime.now());

            return shoppingCart;

        }).collect(Collectors.toList());

        //将购物车对象批量添加到数据库
        shoppingCartMapper.insertBatch(shoppingCartList);

    }

    /**
     * 订单搜索与分页查询
     * @param ordersPageQueryDTO
     * @return
     */
    @Override
    public PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO) {
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());

        Page<Orders> ordersPage = orderMapper.pageQuery(ordersPageQueryDTO);

        //部分订单状态，需要额外返回订单菜品信息，将Orders转化为OrderVO
        List<OrderVO> orderVOList = getOrderVOList(ordersPage);

        return new PageResult(ordersPage.getTotal(), orderVOList);
    }


    // 需要返回订单菜品信息，自定义OrderVO响应结果
    private List<OrderVO> getOrderVOList(Page<Orders> ordersPage) {
        List<OrderVO> orderVOList = new ArrayList<>();
        //全部订单列表
        List<Orders> ordersList = ordersPage.getResult();
        if (!CollectionUtils.isEmpty(ordersList)){
            for (Orders orders : ordersList){
                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders, orderVO);

                String orderDishes = getOrderDishesStr(orders);

                //将订单菜品信息封装到orderVO中，并添加到orderVOList
                orderVO.setOrderDishes(orderDishes);
                orderVOList.add(orderVO);
            }
        }
        return orderVOList;

    }

    /**
     * 根据订单id获取菜品信息字符串
     * @param orders
     * @return
     */
    private String getOrderDishesStr(Orders orders) {
        // 查询订单菜品详情信息（订单中的菜品和数量）
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orders.getId());

        // 将每一条订单菜品信息拼接为字符串（格式：宫保鸡丁*3；）
        List<String> orderDishList = orderDetailList.stream().map(x ->{
            String orderDish = x.getName() + "*" + x.getNumber() + ";";
            return orderDish;
        }).collect(Collectors.toList());

        return String.join("", orderDishList);
    }

    /**
     * 各个状态的订单数量统计
     * @return
     */
    @Override
    public OrderStatisticsVO statistics() {
        // 根据状态，分别查询出待接单、待派送、派送中的订单数量
        Integer toBeConfirmed = orderMapper.countStatus(Orders.TO_BE_CONFIRMED);
        Integer confirmed = orderMapper.countStatus(Orders.CONFIRMED);
        Integer deliveryInProgress = orderMapper.countStatus(Orders.DELIVERY_IN_PROGRESS);

        // 将查询出的数据封装到orderStatisticsVO中响应
        OrderStatisticsVO orderStatisticsVO = new OrderStatisticsVO();
        orderStatisticsVO.setToBeConfirmed(toBeConfirmed);
        orderStatisticsVO.setConfirmed(confirmed);
        orderStatisticsVO.setDeliveryInProgress(deliveryInProgress);

        return orderStatisticsVO;
    }

    @Override
    public void confirm(OrdersConfirmDTO ordersConfirmDTO) {
        Orders orders = Orders.builder()
                .id(ordersConfirmDTO.getId())
                .status(Orders.CONFIRMED)
                .build();

        orderMapper.update(orders);
    }

    /**
     * 拒单
     * - 商家拒单其实就是将订单状态修改为“已取消”
     * - 只有订单处于“待接单”状态时可以执行拒单操作
     * - 商家拒单时需要指定拒单原因
     * - 商家拒单时，如果用户已经完成了支付，需要为用户退款
     * @param ordersRejectionDTO
     */
    @Override
    public void rejection(OrdersRejectionDTO ordersRejectionDTO) {
        //获取订单ID
        Long id = ordersRejectionDTO.getId();
        //根据ID查询订单
        Orders orders = orderMapper.getById(id);

        //只有订单只有存在且状态为2（待接单）才可以拒单
        if (orders == null && !orders.getStatus().equals(Orders.TO_BE_CONFIRMED)){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        //如果已经付款要退款
        Integer payStatus = orders.getPayStatus();
        if (payStatus == Orders.PAID){
            //用户已支付，需要退款
            log.info("申请退款人：{}", orders.getUserId());
        }

        // 拒单需要退款，根据订单id更新订单状态、拒单原因、取消时间
        Orders orders1 = new Orders();
        orders1.setId(orders.getId());
        orders1.setStatus(Orders.CANCELLED);
        orders1.setCancelReason(ordersRejectionDTO.getRejectionReason());//拒绝原因
        orders1.setCancelTime(LocalDateTime.now());

        orderMapper.update(orders1);

    }

    /**
     * 取消订单 订单状态修改为“已取消”
     * @param ordersCancelDTO
     */
    @Override
    public void cancel(OrdersCancelDTO ordersCancelDTO) {
        Long id = ordersCancelDTO.getId();
        Orders orderDB = orderMapper.getById(id);

        Integer payStatus = orderDB.getPayStatus();
        if (payStatus == 1){
            //已支付 退款
            log.info("退款人：{}", orderDB.getUserId());
        }

        Orders orders = new Orders();
        orders.setId(orderDB.getId());
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelReason(ordersCancelDTO.getCancelReason());
        orders.setCancelTime(LocalDateTime.now());

        orderMapper.update(orders);
    }

    /**
     * 派送
     * 只有状态为“待派送”的订单可以执行派送订单操作
     * @param id
     */
    @Override
    public void delivery(Long id) {
        Orders orderDB = orderMapper.getById(id);

        Integer status = orderDB.getStatus();
        //校验订单是否存在，并且状态为3
        if (orderDB == null && status.equals(Orders.CONFIRMED)){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders orders = new Orders();
        orders.setId(orderDB.getId());
        orders.setStatus(Orders.DELIVERY_IN_PROGRESS);

        orderMapper.update(orders);

    }

    /**
     * 完成订单
     * @param id
     */
    @Override
    public void complete(Long id) {
        Orders orderDB = orderMapper.getById(id);

        // 校验订单是否存在，并且状态为4
        if (orderDB == null && !orderDB.getStatus().equals(Orders.DELIVERY_IN_PROGRESS)){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders orders = new Orders();
        orders.setId(orderDB.getId());
        // 更新订单状态,状态转为完成
        orders.setStatus(Orders.COMPLETED);
        orders.setDeliveryTime(LocalDateTime.now());

        orderMapper.update(orders);
    }


    /**
     * 检查客户的收货地址是否超出配送范围
     * @param address
     */
//    private void checkOutOfRange(String address) {
//        Map map = new HashMap();
//        map.put("address",shopAddress);
//        map.put("output","json");
//        map.put("ak",ak);
//
//        //获取店铺的经纬度坐标
//        String shopCoordinate = HttpClientUtil.doGet("https://api.map.baidu.com/geocoding/v3", map);
//
//        JSONObject jsonObject = JSON.parseObject(shopCoordinate);
//        if(!jsonObject.getString("status").equals("0")){
//            throw new OrderBusinessException("店铺地址解析失败");
//        }
//
//        //数据解析
//        JSONObject location = jsonObject.getJSONObject("result").getJSONObject("location");
//        String lat = location.getString("lat");
//        String lng = location.getString("lng");
//        //店铺经纬度坐标
//        String shopLngLat = lat + "," + lng;
//
//        map.put("address",address);
//        //获取用户收货地址的经纬度坐标
//        String userCoordinate = HttpClientUtil.doGet("https://api.map.baidu.com/geocoding/v3", map);
//
//        jsonObject = JSON.parseObject(userCoordinate);
//        if(!jsonObject.getString("status").equals("0")){
//            throw new OrderBusinessException("收货地址解析失败");
//        }
//
//        //数据解析
//        location = jsonObject.getJSONObject("result").getJSONObject("location");
//        lat = location.getString("lat");
//        lng = location.getString("lng");
//        //用户收货地址经纬度坐标
//        String userLngLat = lat + "," + lng;
//
//        map.put("origin",shopLngLat);
//        map.put("destination",userLngLat);
//        map.put("steps_info","0");
//
//        //路线规划
//        String json = HttpClientUtil.doGet("https://api.map.baidu.com/directionlite/v1/driving", map);
//
//        jsonObject = JSON.parseObject(json);
//        if(!jsonObject.getString("status").equals("0")){
//            throw new OrderBusinessException("配送路线规划失败");
//        }
//
//        //数据解析
//        JSONObject result = jsonObject.getJSONObject("result");
//        JSONArray jsonArray = (JSONArray) result.get("routes");
//        Integer distance = (Integer) ((JSONObject) jsonArray.get(0)).get("distance");
//
//        if(distance > 5000){
//            //配送距离超过5000米
//            throw new OrderBusinessException("超出配送范围");
//        }
//    }

}
