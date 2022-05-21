### 目录结构
src

- http 用于http和https代理，无认证等过程
  - HttpClient 客户端
  - HttpParser 请求解析工具
  - HttpRelay http代理类
  - ServerRelay https代理类
- *.java 实现了SOCKS5协议，可用于http代理，https暂不支持
  - FlowDirection 相当于enum，确定流的方向
  - SocketAcceptor 服务端Socket接收器
  - Socks5Client 客户端
  - Socks5Dispatcher 将clientSocket调度给Socks5Handler
  - Socks5Handler 处理器
  - Socks5Relay 中继转发请求
  - Socks5Server 服务端启动器
  - SSLHttpsTest 测试





TODO:

- [ ] SOCKS5加密请求

- [ x ] 支持https

 

