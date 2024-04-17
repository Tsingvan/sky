package com.sky.task;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

/**
 * 自定义定时任务类
 */
@Component
@Slf4j
public class OrderTask {
    @Autowired
    private OrderMapper orderMapper;

    private static final String reason = "支付超时，自动取消";

    /**
     *  处理支付超时订单
     */
    @Scheduled(cron = "0 * * * * ? ")
//    @Scheduled(cron = "0/5 * * * * ?")
    public void processTimeoutOrder(){
        log.info("处理支付超时订单：{}", new Date());

        // select * from orders where status = 1 and order_time < 当前时间-15分钟
        LocalDateTime time = LocalDateTime.now().plusMinutes(-15);//超时十五分钟
        List<Orders> ordersList = orderMapper.getByStatusAndOrdertimeLT(Orders.PENDING_PAYMENT, time);

        if (ordersList != null && ordersList.size() > 0){
            for (Orders order : ordersList) {
                order.setStatus(Orders.CANCELLED);
                order.setCancelTime(LocalDateTime.now());
                order.setCancelReason(reason);

                orderMapper.update(order);
            }
        }
    }

    /**
     * 处理“派送中”状态的订单
     */
    @Scheduled(cron = "0 0 1 * * ? ")
//    @Scheduled(cron = "0/10 * * * * ?")
    public void processDeliveryOrder(){
        log.info("处理“派送中”状态的订单：{}", new Date());

        // select * from orders where status = 1 and order_time < 当前时间-60分钟
        LocalDateTime time = LocalDateTime.now().plusMinutes(-60);//凌晨1点之前的所有订单

        List<Orders> ordersList = orderMapper.getByStatusAndOrdertimeLT(Orders.DELIVERY_IN_PROGRESS, time);
        if (ordersList != null && ordersList.size() > 0){
            for (Orders order : ordersList) {
                order.setStatus(Orders.COMPLETED);

                orderMapper.update(order);
            }
        }

    }

}
