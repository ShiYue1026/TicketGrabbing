# Ticket Grabbing
## 自定义线程绑定工具

**BaseParameterHolder**

在业务服务中通过需要一些公共参数，比如分布式链路traceId、用户id、平台code等，按道理说这些这些参数是由Gateway网关传递而来，放到了request请求头中，业务服务需要的时候直接去request中获取就可以了。

但由于request的范围是`ThreadLocal`，是和线程绑定的，在使用线程池的情况下，request会丢失，所以有了线程池组件、Hystrix组件。

在线程池组件和Hystrix组件中，对公共配置中的线程绑定工具`BaseParameterHolder`做了适配，这个工具其实是`ThreadLocal`，但线程池的组件和Hystrix的组件对此工具做了增强，<u>只要将数据放到`BaseParameterHolder`里，就会正常的拿到</u>。



## 过滤器

**BaseParameterFilter**

- 继承`OncePerRequestFilter`，确保每次请求在任何情况下只经过过滤器一次
- 用于从request请求头中获取一些公共参数，比如分布式链路traceId、用户id、平台code等，存入自定义的`BaseParameterHolder`（ThreadLocal）中
- 同时将这些公共参数信息放入MDC中用于日志记录



**RequestWrapperFilter**

- 继承`OncePerRequestFilter`，确保每次请求在任何情况下只经过过滤器一次
- 为了解决通过getInputStream和getReader获取request请求体输入流只能读取一次的问题
- 借助自定义的`CustomizeRequstWrapper`使用装饰者模式对request进行包装，重写getInputStream()，getReader()方法缓存request请求体输入流的内容



-- 来源：https://gitee.com/java-up-up/damai
