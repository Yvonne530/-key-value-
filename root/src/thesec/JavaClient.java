package thesec;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;//用于网络通信
import java.util.Scanner;
public class JavaClient {
    private static final String SERVER_ADDRESS = "127.0.0.1"; // 本地地址闭环
    private static final int SERVER_PORT = 3000; // 端口

    public static void main(String[] args) {
    try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);//创建Socket并连接到服务端
    BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));//创建BufferedReader
    PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)) {

    System.out.println("连接到服务端，输入命令进行交互...");
    try (Scanner scanner = new Scanner(System.in)) {
    while (true) {
    // 获取用户输入的命令
    System.out.print("请输入命令: ");
    String command = scanner.nextLine();
    writer.println(command);//传输
    String response = reader.readLine();//接收
    System.out.println("服务端响应: " + response);
    if (command.equalsIgnoreCase("exit")) {
    System.out.println("退出客户端...");
break;
}                                                 
}
}
} catch (IOException e) {
    e.printStackTrace();//打印异常
}
}
}
