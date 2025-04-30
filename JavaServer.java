package thesec;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;//用于加载配置文件
import java.util.concurrent.ExecutorService;//用于多线程
import java.util.concurrent.Executors;//用于创建线程池
import java.util.logging.*;

public class JavaServer {
    private static final CustomerServiceSubscription db = new CustomerServiceSubscription();
    private static final Logger logger = Logger.getLogger(JavaServer.class.getName());
    private static final int THREAD_POOL_SIZE = 15 ; //定义线程池大小
    private static final int DEFAULT_PORT = 3000; // 定义默认端口
    private static final int MAX_PORT_RETRIES = 15 ;//定义最大端口重试次数
    static {
        try {
            new File("logs").mkdirs() ;//创目录
            FileHandler fileHandler = new FileHandler("logs/server.log", true); //创建文件管理器，追加模式
            fileHandler.setFormatter(new SimpleFormatter());
            logger.addHandler(fileHandler);
        } catch (IOException e) {
            System.err.println("无法初始化日志文件: " + e.getMessage()); 
        }
    }
    public static void main(String[] args) {
    int port = findAvailablePort() ;//查找可用端口
    if (port == -1) {
    logger.severe("找不到可用端口，服务器启动失败");
    return;
}
ExecutorService threadPool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);//创建静态线程池
try (ServerSocket serverSocket = new ServerSocket(port)) {//创建ServerSocket并监听指定窗口
    logger.info("服务器启动成功，监听端口: " + port);//记录成功日志
    System.out.println("服务器已启动，监听端口：" + port);
    while (true) {
        try {
            Socket clientSocket = serverSocket.accept() ;//接受客户端连接
            logger.info("客户端连接: " + clientSocket.getRemoteSocketAddress());
            threadPool.execute(() -> handleClient(clientSocket));//调用线程池处理客户端
        } catch (IOException e) {
            logger.warning("客户端连接异常: " + e.getMessage());
        }
    }
} catch (IOException e) {
    logger.severe("服务器启动失败: " + e.getMessage());
} finally {
    threadPool.shutdown();//关闭线程池
}
}
//查找可用端口
private static int findAvailablePort() {
    int port = loadPortFromConfig() ;
    int retries = 0;//初始化
    while (retries < MAX_PORT_RETRIES) {
        try (ServerSocket testSocket = new  ServerSocket(port)) {
            return port;
        } catch (IOException e) {
            logger.warning("端口 " + port + " 被占用，尝试下一个端口");
            port++;//下一个
            retries++;
        }
    }
    return -1;//空集处理
}
private static int loadPortFromConfig() {
    Properties props = new Properties();
    File configFile = new File("config.properties");//创建文件
    if (!configFile.exists()) {
        try (OutputStream out = new FileOutputStream(configFile)) {//创建输出流
            props.setProperty("server.port", String.valueOf(DEFAULT_PORT));
            props.store(out, "Server Configuration") ; //创建配置文件 
            logger.info("创建默认配置文件");
        } catch (IOException e) {
            logger.warning("无法创建配置文件: " + e.getMessage());
        }
    }
    try (InputStream in = new FileInputStream(configFile)) {//创建输入流
        props.load(in) ;//加载
        return Integer.parseInt(props.getProperty("server.port", String.valueOf(DEFAULT_PORT)));
    } catch (IOException | NumberFormatException e) {
        logger.warning("使用默认端口 " + DEFAULT_PORT + ": " + e.getMessage());
        return DEFAULT_PORT;//返默认端口
    }
}
private static void handleClient(Socket clientSocket) {
    try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
    PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

        String inputLine ; //用于存储输入
        while ((inputLine = in.readLine()) != null) {
            String response = db.processRequest(inputLine);//处理请求获取回应
            out.println(response);//应答客户端
            if ("exit".equalsIgnoreCase(inputLine.trim())) {//exit退出指令
                break;
            }
        }
    } catch (IOException e) {
        logger.severe("客户端处理错误: " + e.getMessage());
    } finally {
        try {
            clientSocket.close();
        } catch (IOException e) {
            logger.warning("关闭客户端连接时出错: " + e.getMessage());
        }
    }
}
}