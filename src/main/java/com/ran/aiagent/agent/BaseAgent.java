package com.ran.aiagent.agent;

import cn.hutool.core.util.StrUtil;
import com.ran.aiagent.agent.model.AgentState;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 定义流程
 * 抽象的基础管理类，用于管理代理状态和执行流程
 * 提供状态转换，内存管理和步骤的执行循环的基础功能
 * 子类必须实现step方法
 *
 */
@Data
@Slf4j
public abstract class BaseAgent {
    // 核心属性
    private String name;

    // 提示词
    private String systemPrompt;
    private String nextStepPrompt;

    // 代理状态
    private AgentState state = AgentState.IDLE;

    // 执行步骤控制
    private int currentStep = 0;
    private int maxStep = 10;

    // LLM大模型
    private ChatClient chatClient;

    // Memory
    List<Message> messageList = new ArrayList<>();


    // 运行代理
    public String run(String userPrompt){
        // 基础校验
        if (StrUtil.isBlank(userPrompt)){
            throw new RuntimeException("userPrompt is not empty");
        }
        if (this.state!= AgentState.IDLE){
            throw new RuntimeException("state is not IDLE");
        }
        // 执行，需要将运行状态更改
        state = AgentState.RUNNING;
        // 记录消息的上下文
        messageList.add(new UserMessage(userPrompt));
        // 添加结果集
        List<String> results = new ArrayList<>();
        try {
            //执行循环
            for (int i = 0; i < maxStep && state!= AgentState.FINISHED; i++) {
                int stepNumber = i + 1;
                currentStep = stepNumber;
                log.info("Executing step {}/{}",currentStep,maxStep);
                // 单步执行
                String stepResulut = step();
                String result = "Step " + stepNumber + ":"+stepResulut;
                results.add(result);
            }
            if (currentStep>=maxStep){
                state = AgentState.FINISHED;
                results.add("Reach max step");
            }
            return String.join("\n",results);
        }catch (Exception e){
            log.error("executing error");
            return "执行错误"+e.getMessage();
        }finally {
            this.cleanUp();
        }
    }

    // SSeEmitter输出
    public SseEmitter runStream(String userPrompt){
        // 设置超时时间
        SseEmitter sseEmitter = new SseEmitter(600000L);
        // 使用线程池，异步处理，避免阻塞
        CompletableFuture.runAsync(
                ()->{
                    try{
                        // 基础校验
                        if (StrUtil.isBlank(userPrompt)){
                            sseEmitter.send("错误，不能使用空提示词运行代理");
                            sseEmitter.complete();
                            return;
                        }
                        if (this.state!= AgentState.IDLE){
                            sseEmitter.send("错误，无法从状态运行代理"+this.state);
                            sseEmitter.complete();
                            return;
                        }
                        // 执行，需要将运行状态更改
                        state = AgentState.RUNNING;
                        // 记录消息的上下文
                        messageList.add(new UserMessage(userPrompt));
                        try {
                            //执行循环
                            for (int i = 0; i < maxStep && state!= AgentState.FINISHED; i++) {
                                int stepNumber = i + 1;
                                currentStep = stepNumber;
                                log.info("Executing step {}/{}",currentStep,maxStep);
                                // 执行单步（think + act）
                                String stepResult = step();

                                // 工具调用阶段：推送简短进度提示，让用户知道 Agent 正在工作
                                // 最终汇总阶段（FINISHED）：推送完整摘要
                                if (state == AgentState.FINISHED) {
                                    if (stepResult != null && !stepResult.isEmpty()
                                            && !"思考完成，不需要执行".equals(stepResult)) {
                                        sseEmitter.send(stepResult);
                                    }
                                } else if (stepResult != null && !stepResult.isEmpty()
                                        && !stepResult.startsWith("工具")) {
                                    // 非工具原始结果的中间输出（如错误信息）也推送
                                    sseEmitter.send("[进度] " + stepResult.substring(0, Math.min(80, stepResult.length())));
                                }
                            }
                            if (currentStep>=maxStep){
                                state = AgentState.FINISHED;
                                sseEmitter.send("执行结束，达到最大步骤");
                            }
                            // 正常完成
                            sseEmitter.complete();

                        }catch (Exception e){
                            state = AgentState.ERROR;
                            log.error("执行智能体失败", e);
                            try {
                                sseEmitter.send("执行错误: " + e.getMessage());
                                sseEmitter.complete();
                            } catch (Exception ex) {
                                sseEmitter.completeWithError(ex);
                            }
                        }finally {
                            this.cleanUp();
                        }
                    }catch (Exception e){
                        sseEmitter.completeWithError(e);
                    }
                });

        // 设置超时时间
        sseEmitter.onTimeout(()->{
            this.state = AgentState.ERROR;
            this.cleanUp();
            log.warn("SSE Connect ERROR");
        });
        // 完成
        sseEmitter.onCompletion(()->{
            if (this.state == AgentState.RUNNING){
                this.state = AgentState.FINISHED;
            }
            this.cleanUp();
            log.info("SSE connection completed");
        });
        return sseEmitter;
    }


    // 定义单个步骤
    public abstract String step();


    // 清理资源
    protected void cleanUp(){

    }
}
