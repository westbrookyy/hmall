package com.hmall.gateway.route;

import cn.hutool.json.JSONUtil;
import com.alibaba.cloud.nacos.NacosConfigManager;
import com.alibaba.nacos.api.config.listener.Listener;
import com.hmall.common.utils.CollUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionWriter;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;

@Slf4j
@Component
@RequiredArgsConstructor
public class DynamicRouteLoader {

    private final RouteDefinitionWriter writer;
    private  final NacosConfigManager nacosConfigManager;

    //路由配置文件和分组
    private static final String dataId = "gateway-routes.json";
    private static final String group = "DEFAULT_GROUP";

    //保存更新过的路由id
    private static final Set<String> routeIds = new HashSet<>();

    @PostConstruct
     public void initRouteConfigListener() throws Exception {
         //路由配置更新监听
         String configInfo =  nacosConfigManager.getConfigService()
                 .getConfigAndSignListener(dataId, group, 5000, new Listener() {
             @Override
             public Executor getExecutor() {
                 return null;
             }

             @Override
             public void receiveConfigInfo(String configInfo) {
                //收到配置变更更新路由表
                 updateConfigInfo(configInfo);
             }
         });
         //首次启动，更新配置
         updateConfigInfo(configInfo);
    }
    private void updateConfigInfo(String configInfo) {
         log.info("更新路由配置：{}", configInfo);
         //1.反序列化
        List<RouteDefinition> routeDefinitions = JSONUtil.toList(configInfo, RouteDefinition.class);
        //2.先清空旧路由
          //2.1.清除旧路由（Mono响应式编程容器，需要订阅）
        for (String routeId : routeIds) {
            writer.delete(Mono.just(routeId)).subscribe();
        }
        routeIds.clear();
          //2.2.判断是否有新的路由配置需要更新
        if (CollUtils.isEmpty(routeDefinitions)) {
            return;
        }
         //3.更新路由
        routeDefinitions.forEach(routeDefinition -> {
            writer.save(Mono.just(routeDefinition)).subscribe();
            routeIds.add(routeDefinition.getId());
        });
    }
}
