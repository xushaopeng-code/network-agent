package com.example.networkagent;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.core.model.DashScopeChatModel;

/**
 * AgentScope Java Demo：一个带网络诊断工具的助手智能体。
 *
 * 运行前设置环境变量：
 *   export DASHSCOPE_API_KEY=sk-xxxx
 * 然后执行：
 *   mvn compile exec:java
 */
public class NetworkAgentDemo {

    public static void main(String[] args) {
        String apiKey = System.getenv("DASHSCOPE_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            System.err.println("请先设置环境变量 DASHSCOPE_API_KEY");
            System.exit(1);
        }

        Toolkit toolkit = new Toolkit();
        toolkit.registerTool(new NetworkTools());

        ReActAgent agent = ReActAgent.builder()
                .name("NetHelper")
                .sysPrompt("你是一个网络运维助手，可以使用工具排查网络连通性问题，"
                        + "回答时请说明工具检测到的结果并给出简要建议。")
                .model(DashScopeChatModel.builder()
                        .apiKey(apiKey)
                        .modelName("qwen-max")
                        .build())
                .toolkit(toolkit)
                .build();

        Msg msg = Msg.builder()
                .textContent("帮我检查一下 www.aliyun.com 是否能 ping 通，"
                        + "以及它的 443 端口是否开放，顺便告诉我现在几点了。")
                .build();

        Msg response = agent.call(msg).block();
        System.out.println(response.getTextContent());
    }
}

class NetworkTools {

    @Tool(name = "ping_host", description = "检测目标主机网络连通性（ICMP/TCP 探测）")
    public String pingHost(
            @ToolParam(name = "host", description = "主机名或 IP 地址，例如 www.aliyun.com") String host) {
        try {
            long start = System.currentTimeMillis();
            boolean reachable = java.net.InetAddress.getByName(host).isReachable(3000);
            long cost = System.currentTimeMillis() - start;
            return reachable
                    ? String.format("主机 %s 可达，耗时 %d ms", host, cost)
                    : String.format("主机 %s 不可达（3 秒超时）", host);
        } catch (Exception e) {
            return "探测失败：" + e.getMessage();
        }
    }

    @Tool(name = "check_port", description = "检测目标主机的 TCP 端口是否开放")
    public String checkPort(
            @ToolParam(name = "host", description = "主机名或 IP 地址") String host,
            @ToolParam(name = "port", description = "端口号，例如 443") int port) {
        try (java.net.Socket socket = new java.net.Socket()) {
            long start = System.currentTimeMillis();
            socket.connect(new java.net.InetSocketAddress(host, port), 3000);
            long cost = System.currentTimeMillis() - start;
            return String.format("%s:%d 端口开放，连接耗时 %d ms", host, port, cost);
        } catch (Exception e) {
            return String.format("%s:%d 端口不可连接：%s", host, port, e.getMessage());
        }
    }

    @Tool(name = "get_time", description = "获取当前本地时间")
    public String getTime() {
        return java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}
