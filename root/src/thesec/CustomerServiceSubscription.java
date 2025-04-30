package thesec;
import java.io.*;
import java.util.*;//导入集合框架
import java.util.logging.*;//导入日志
//日志记录器
public class CustomerServiceSubscription {

    private static final Logger logger = Logger.getLogger(CustomerServiceSubscription.class.getName());
    private final Map<String, LinkedList<String>> dataTable = new HashMap<>();//dataable存储列表数据
    private final Map<String, Map<String, String>> hashTable = new HashMap<>();//hashTable存储哈希表数据
    private final String persistPath = "subscriptions.txt";//用于持久化列表中的操作记录
    private final String hashPath = "hashes.txt";//持久报错
    private final String logPath = "server.log";//持久服务内容
    public CustomerServiceSubscription() {
        setupLog();//初始化日志
        loadData();//载入列表
        loadHash();//载入哈希表
    }

//初始化日志

    private void setupLog(){
        try {
            new File("logs").mkdirs();//创建目录
            FileHandler fileHandler = new FileHandler("logs/" + logPath, true);//创建日志
            fileHandler.setFormatter(new SimpleFormatter());//设置日志格式为简单格式
            logger.addHandler(fileHandler);//添加文件处理器添加到日志记录器
            logger.setLevel(Level.ALL);//设置级别为所有级别
        } catch (IOException e) {
            System.err.println("无法初始化日志文件: " + e.getMessage());
        }//执行失败异常捕捉
    }

//持久化列表

    private void loadData() {
        File file = new File(persistPath);//创建文件列表
        if (!file.exists()) {//为空则创建新文件
            try {
                file.createNewFile();
                logger.info("创建成功: " + file.getAbsolutePath());
            } catch (IOException e) {
                logger.warning("创建失败: " + e.getMessage());
                return;
            }
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {//使用BufferedReadeer读取文件
            String line ;//用于存储每一行 
            while ((line = reader.readLine()) != null) {//设置读取文件格式
                String[] entry = line.split(" ", 2);
                if (entry.length == 2) {
                    String key = entry[0].trim();
                    String[] values = entry[1].split(",");
                    LinkedList<String> list = new LinkedList<>(Arrays.asList(values));
                    dataTable.put(key, list);
                }
            }
            logger.info("成功加载列表");
        } catch (IOException e) {
            logger.severe("读取持久化文件失败: " + e.getMessage());
        }
    }

//从持久化文件读取哈希表数据

    private void loadHash() {
        File file = new File(hashPath);//创建文件
        if (!file.exists()) return;//排空
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null)  {//循环读取每一行
                String[] keyField = line.split("\\|", 2) ;//按竖线分割
                if (keyField.length != 2) continue ; //分割失败跳下个
                String[] fieldVal = keyField[1].split("=", 2);
                if (fieldVal.length != 2) continue ;
                hashTable.computeIfAbsent(keyField[0], k -> new HashMap<>()).put(fieldVal[0], fieldVal[1]);//存储字段和值
            }
            logger.info("读取成功");
        } catch (IOException e) {
            logger.severe("读取失败: " + e.getMessage());
        }
    }

//转存列表到文件

    private void saveData() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(persistPath))) {
            for (Map.Entry<String, LinkedList<String>> entry : dataTable.entrySet()) {
                writer.write(entry.getKey() + "=" + String.join(",", entry.getValue()));
                writer.newLine();
            }
            logger.fine("列表保存成功");//记录保存成功的日志
        } catch (IOException e) {
            logger.severe("列表保存失败: " + e.getMessage()) ;//类上
        }
    }

//转存哈希表到文件

    private void saveHash() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(hashPath))) {
            for (Map.Entry<String, Map<String, String>> entry : hashTable.entrySet()) {//遍历HashTable
                String key = entry.getKey();//遍历HashTable 
                for (Map.Entry<String, String> fieldEntry : entry.getValue().entrySet()) {
                    writer.write(key + "|" + fieldEntry.getKey() + "=" + fieldEntry.getValue());
                    writer.newLine();
                }
            }
            logger.fine("哈希数据保存成功");
        } catch (IOException e) {
            logger.severe("哈希数据保存失败: " + e.getMessage());
        }
    }
//客户端命令处理
    public String processRequest(String command) {
        if (command == null || command.trim().isEmpty()) return "无效命令";//去空
        String[] parts = command.trim().split(" ");
        String action = parts[0].toLowerCase();//获取命令动作部分并转成小写
        try {
            switch (action) {
                case "ping": return "pong";//心跳指令
                case "help": return (parts.length == 1) ? getGeneralHelp() : getCommandHelp(parts[1]);//获助帮助
                case "lpush": case "rpush":
                    if (parts.length < 3) return "命令格式错误: " + action + " key value [value ...]";//查参数个数
                    return pushMulti(parts[1], Arrays.copyOfRange(parts, 2, parts.length), action.equals("lpush"));
                case "lpop": case "rpop"://左右出列
                    if (parts.length != 2) return "命令格式错误: " + action + " key";
                    return pop(parts[1], action.equals("lpop"));
                case "range"://检查参数数量
                    if (parts.length != 4) return "命令格式错误: range key start end";
                    return getRange(parts[1], Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
                case "len":
                    if (parts.length != 2) return "命令格式错误: len key";
                    return getLength(parts[1]);
                case "ldel":
                    if (parts.length != 2) return "命令格式错误: ldel key";
                    return deleteKey(parts[1]);
                case "hset":
                    if (parts.length != 4) return "命令格式错误: hset key field value";
                    return hset(parts[1], parts[2], parts[3]);
                case "hget":
                    if (parts.length != 3) return "命令格式错误: hget key field";
                    return hget(parts[1], parts[2]);
                case "hdel":
                    if (parts.length == 2) return hdelAll(parts[1]);
                    if (parts.length == 3) return hdelField(parts[1], parts[2]);
                    return "命令格式错误: hdel key [field]";
                case "exit": return "退出客户端...";
                default: return "未知命令: " + action;//未知处理
            }
        } catch (Exception e) {
            logger.severe("命令执行错误: " + command + " - " + e.getMessage());
            return "命令执行出错: " + e.getMessage() ;
        }
    }

    private String pushMulti(String key, String[] values, boolean left) {//从左往右插入数据
        LinkedList<String> list = dataTable.computeIfAbsent(key, k -> new LinkedList<>());
        for (String value : values) {
            if (left) list.addFirst(value);//从左边插值
            else list.addLast(value);//从右边插值
            logger.info((left ? "lpush" : "rpush") + ": " + key + " = " + value);
        }
        saveData();
        return "成功" + (left ? "左推" : "右推") + ": " + key + " = " + String.join(", ", values);
    }
    //从左弹出一个值
    private String pop(String key, boolean left) {
        LinkedList<String> list = dataTable.get(key);//获列表
        if (list == null || list.isEmpty()) return "没有元素可弹出: " + key;//查空集
        String value = left ? list.pollFirst() : list.pollLast();//弹值
        saveData();
        logger.info((left ? "lpop" : "rpop") + ": " + value);
        return "成功" + (left ? "左" : "右") + "弹出: " + value;
    }

    private String getRange(String key, int start, int end) {
        LinkedList<String> list = dataTable.get(key);
        if (list == null) return "key不存在: " + key ;//检查key是否存在
        int size = list.size();//获取列表大小
        start = Math.max(0, start);//明确start>=0的有效性
        end = Math.min(size - 1, end);//类上
        if (start > end) return "无效范围";//检查范围有效性
        List<String> sub = list.subList(start, end + 1);//获取子列表
        return "数据: " + String.join(", ", sub);//返回范围内的数据
    }

//获取列表长度

    private String getLength(String key) {//获取列表长度
        LinkedList<String> list = dataTable.get(key);
        return (list == null) ? "key不存在: " + key : "长度: " + list.size();//返回长度或不存在信息
    }

    //删除列表

    private String deleteKey(String key) {
        if (dataTable.remove(key) != null) {
            saveData();
            logger.info("删除列表数据: " + key);
            return "已删除: " + key;
        }
        return "key不存在: " + key;
    }
//设置哈希表字段值
    private String hset(String key, String field, String value) {
        Map<String, String> map = hashTable.computeIfAbsent(key, k -> new HashMap<>());
        map.put(field, value);//设置字段值
        saveHash();//保存
        logger.info("hset: " + key + "[" + field + "] = " + value);//记录设置操作日志
        return "设置成功: " + field + " = " + value;
    }

    private String hget(String key, String field) {
        Map<String, String> map = hashTable.get(key);//获取
        if (map == null) return "key不存在: " + key ;//检查key是否存在
        String val = map.get(field);
        return (val != null) ? "值: " + val : "field不存在: " + field ;//空集处理
    }
//剔除字段
    private String hdelField(String key, String field) {
        Map<String, String> map = hashTable.get(key);//获取哈希表
        if (map != null && map.remove(field) != null) {//删除字段
            if (map.isEmpty()) hashTable.remove(key);
            saveHash() ;//保存哈希文件
            logger.info("hdel field: " + key + "." + field);//记录
            return "字段已删除: " + field;
        }
        return "key或field不存在";
    }
//删除key
    private String hdelAll(String key) {
        if (hashTable.remove(key) != null) {
            saveHash();//保存
            logger.info("hdel all: 删除key成功: " + key) ;//记录删除操作日志
            return "key已删除: " + key ;
        }
        return "key不存在";
    }

    private String getGeneralHelp() {
        return String.join("\n",
            "命令列表：",
            "ping                       -> 心跳检测",
            "help                       -> 查看所有命令",
            "help [command]             -> 查看单个命令说明",
            "lpush [key] [val...]       -> 从左插入一个或多个值",
            "rpush [key] [val...]       -> 从右插入一个或多个值",
            "lpop [key]                 -> 从左弹出",
            "rpop [key]                 -> 从右弹出",
            "range [key] [start] [end]  -> 查看某个范围的值",
            "len [key]                  -> 查看列表长度",
            "ldel [key]                 -> 删除整个列表",
            "hset [key] [field] [val]   -> 设置哈希字段",
            "hget [key] [field]         -> 获取哈希字段值",
            "hdel [key] [field]         -> 删除字段",
            "hdel [key]                 -> 删除整个哈希键"
        );
    }
//获取特定命令的帮助信息
    private String getCommandHelp(String cmd) {
        switch (cmd) {
            case "ping": return "ping -> 心跳检测，返回 pong";
            case "lpush": return "lpush [key] [val...] -> 从左插入一个或多个值到列表";
            case "rpush": return "rpush [key] [val...] -> 从右插入一个或多个值到列表";
            case "lpop": return "lpop [key] -> 从左弹出值";
            case "rpop": return "rpop [key] -> 从右弹出值";
            case "range": return "range [key] [start] [end] -> 获取指定范围的值";
            case "len": return "len [key] -> 获取列表长度";
            case "ldel": return "ldel [key] -> 删除整个列表";
            case "hset": return "hset [key] [field] [val] -> 设置哈希字段";
            case "hget": return "hget [key] [field] -> 获取哈希字段值";
            case "hdel": return "hdel [key] [field] 或 hdel [key] -> 删除字段或整个键";
            case "help": return "help 或 help [command] -> 查看帮助";
            default: return "未知命令: " + cmd;
        }
    }
}
