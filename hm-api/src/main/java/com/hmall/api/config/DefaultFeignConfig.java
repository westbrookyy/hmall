package com.hmall.api.config;

import com.hmall.api.client.fallback.ItemClientFallback;
import com.hmall.api.client.fallback.PayClientFallback;
import com.hmall.common.utils.UserContext;
import feign.Logger;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.context.annotation.Bean;

public class DefaultFeignConfig {
    @Bean
    public Logger.Level feignLogLevel(){
        return Logger.Level.FULL;
    }

    /**
     * 添加用户信息到Feign请求头中,保证下游的微服务都能获取到userId
     * @return
     */
    @Bean
    public RequestInterceptor userInfoRequestInterceptor(){
        return new RequestInterceptor() {
            @Override
            public void apply(RequestTemplate template) {
                // 获取登录用户
                Long userId = UserContext.getUser();
                if(userId == null) {
                    // 如果为空则直接跳过
                    return;
                }
                // 如果不为空则放入请求头中，传递给下游微服务
                template.header("user-info", userId.toString());
            }
        };
    }
    /**
     * 添加商品服务熔断处理类
     * @return
     */
    @Bean
    public ItemClientFallback itemClientFallback(){
        return new ItemClientFallback();
    }

    /**
     * 添加支付服务消息处理类
     * @return
     */
    @Bean
    public PayClientFallback payClientFallback(){
        return new PayClientFallback();
    }
}
