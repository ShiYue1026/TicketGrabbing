# 支持高并发的抢票系统

<img width="658" alt="image" src="https://github.com/user-attachments/assets/3b833332-f880-4c9b-849d-c911d94023fd" />

<img width="941" alt="image" src="https://github.com/user-attachments/assets/061a4068-6e14-4fc4-bf6a-d8d925b211be" />

<img width="800" alt="60acf347e1fac647f21847696375c52" src="https://github.com/user-attachments/assets/635742e9-4cee-4935-bd00-00ac80600274" />


## 技术选型
**SpringBoot、SpringCloud、Nacos、Sentinel、Mysql、MybatisPlus、Shadingsphere、Redis、Caffeine、Redisson、Elasticsearch、Kafka**

## 项目描述


### 解决方案与亮点

#### 架构设计
- 采用**微服务架构**，按照业务维度拆分成多个服务，针对购票场景中的高并发及大数据存储问题，设计了多种优化方案。

#### 数据存储与分库分表
- 使用 **ShardingSphere** 进行分库分表，解决海量数据存储问题。
- 订单表采用**基因法分片算法**，避免全路由查询，优化查询性能。

#### 并发性能优化
- 引入多种锁机制（**本地锁、分布式锁、读写锁、分段锁、双重检测、细化锁粒度**）提升系统并发能力。
- 使用 **Lua + Redis** 原子性操作实现余票数量更新，实现**无锁化设计**，大幅减轻Redis网络通信时耗导致的性能瓶颈。

#### 业务逻辑优化
- 使用**模板模式和策略模式**封装项目初始化行为，统一管理执行动作与顺序，提升代码复用性。
- 基于**组合模式和树形结构**设计验证组件，实现复杂业务验证逻辑的统一管理。

#### 用户注册场景优化
- 通过 **Lua + Redis** 结合并发量动态判断是否需要图形验证码。
- 使用**计数器**和**布隆过滤器**解决缓存穿透问题，减少无效请求对系统的影响。

#### 缓存优化
- Redis采用**一主一从一哨兵**的模式，在读写分离的同时保证了高可用性。
- 使用**多级缓存结构**：
  - **Redis** 缓存热点数据（字符串、哈希表、有序集合）。
  - **Caffeine** 作为高效本地缓存，进一步提高查询效率。

#### 消息队列与异步化
- 使用 **Kafka** 实现异步创建订单功能，解耦用户下单与订单创建流程。
- 确保余票一致性下最大化用户抢票并发能力。

## 参考来源
https://gitee.com/java-up-up/damai
