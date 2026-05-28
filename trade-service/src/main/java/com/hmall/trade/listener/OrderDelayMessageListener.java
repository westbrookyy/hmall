package com.hmall.trade.listener;

import com.hmall.api.client.PayClient;
import com.hmall.api.domain.dto.PayOrderDTO;
import com.hmall.trade.constants.MqConstants;
import com.hmall.trade.domain.po.Order;
import com.hmall.trade.service.IOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderDelayMessageListener {

    private final IOrderService orderService;
    private final PayClient payClient;

    //收到支付延迟消息后的处理逻辑，保证订单表和支付流水表的支付状态一致性
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = MqConstants.DELAY_ORDER_QUEUE_NAME, durable = "true"),
            exchange = @Exchange(name = MqConstants.DELAY_EXCHANGE_NAME),
            key = MqConstants.DELAY_ORDER_KEY
    ))
    public void listenOrderDelayMessage(Long orderId) {
        //1.根据订单id在订单表查询订单
        Order order = orderService.getById(orderId);
        //2.检查订单状态
        if (order == null || order.getStatus() != 1){
            //如果订单不存在或者已支付，则返回
            return;
        }
        //3.未支付，查询支付流水单pay_order
        PayOrderDTO payOrderDTO = payClient.queryPayOrderByBizOrderNo(orderId);
        //4.判断是否支付
        if (payOrderDTO == null || payOrderDTO.getStatus() == 3){
            //已支付，订单状态标记为已支付
            orderService.markOrderPaySuccess(orderId);
        } else {
            //未支付，取消订单，商品退库
            orderService.cancelOrder(orderId);
        }
    }
}
