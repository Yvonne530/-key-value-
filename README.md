# 网络key-value数据库  

## 一、项目简介  
本项目实现一个基于Java Socket的多线程服务器，支持多客户端连接，提供如下功能：  

- 字符串类型的 key-value 存储（set/get/del）  
- 双向链表类型支持左右端操作（lpush/rpush/lpop/rpop/range/len/ldel）  
- 哈希类型 key-field-value 存储（hset/hget/hdel）  
- 其它指令：ping、help  
- 数据持久化（set/del 指令持久化文件存储）  
- 读取配置文件（监听端口、日志路径、数据存储路径）  
- 日志功能（访问IP、异常记录）  
- 异常处理，保证服务器稳定运行  

---  

## 二、技术与语言特性  
- Java 基础：I/O流、异常处理、多线程、Socket编程  
- 集合框架：HashMap，LinkedList，Concurrent相关类  
- 日志工具：java.util.logging或Log4j（可选）  
- 配置文件读取：Properties类  
- 数据持久化：文件读取写入操作  

---  

## 三、功能设计  

### 3.1 服务器端设计  
- 启动时读取配置文件，设置监听端口、日志文件路径、数据存储路径  
- 使用 ServerSocket 监听端口  
- 为每个客户端连接开启一个线程，实现多客户端并发处理  
- 维护三个主要数据结构：  
  - Map<String, String> 字符串类型  
  - Map<String, LinkedList<String>> 双向链表类型  
  - Map<String, Map<String, String>> 哈希类型  
- 支持持久化，将set和del指令内容写入文件，程序启动时读取  
- 日志功能记录客户端连接、断开、异常信息  

### 3.2 客户端设计  
- 使用Socket连接服务器指定端口  
- 允许控制台输入命令，发送指令给服务器  
- 接收服务器响应并打印显示  

---  

## 四、指令说明  

| 指令       | 作用描述                                      | 参数                             |  
|------------|----------------------------------------------|---------------------------------|  
| set        | 存储 key 对应字符串 value                      | `set [key] [value]`              |  
| get        | 获取 key 对应的 value                          | `get [key]`                     |  
| del        | 删除 key 对应的 value                          | `del [key]`                     |  
| lpush      | 在双向链表左端插入数据                          | `lpush [key] [value]`            |  
| rpush      | 在双向链表右端插入数据                          | `rpush [key] [value]`            |  
| range      | 返回 key 对应链表 start 到 end 的数据           | `range [key] [start] [end]`      |  
| len        | 获取 key 对应数据个数                           | `len [key]`                     |  
| lpop       | 获取并删除 key 链表左端数据                      | `lpop [key]`                    |  
| rpop       | 获取并删除 key 链表右端数据                      | `rpop [key]`                    |  
| ldel       | 删除 key 对应整个链表数据                        | `ldel [key]`                    |  
| hset       | 存储哈希类型 key 中 field 的 value              | `hset [key] [field] [value]`     |  
| hget       | 获取 hset 中 field 的 value                      | `hget [key] [field]`             |  
| hdel       | 删除哈希类型 key 中 field 或者全部数据            | `hdel [key] [field]` 或 `hdel [key]` |  
| ping       | 心跳检测，返回 pong                             | `ping`                         |  
| help       | 查看所有指令使用说明                             | `help` 或 `help [command]`       |  

---  

