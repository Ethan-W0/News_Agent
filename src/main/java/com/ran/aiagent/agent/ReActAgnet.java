package com.ran.aiagent.agent;

import com.ran.aiagent.agent.model.AgentState;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;


@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
/**
 * 继承自BaseAgent，并且将 step 方法分解为 think 和 act 两个抽象方法。
 */
public abstract class ReActAgnet extends BaseAgent{
    // 是否需要发起行动，true 表示需要执行，false表示不需要执行
    // 同步版本（兼容 run() 方法）
    public abstract boolean think();
    // 流式版本：将 token 实时推送到 SseEmitter（子类按需覆盖）
    public boolean thinkStream(SseEmitter sseEmitter) {
        // 默认退化为同步 think，子类可覆盖以实现真正的流式推送
        return think();
    }
    // 执行决定的行动
    public abstract String act();

    @Override
    public String step() {
        try{
            // 先思考
            boolean shouldAct = think();
            if(!shouldAct){
                setState(AgentState.FINISHED);
                // 如果子类保存了最终汇总答案，直接返回给调用方（BaseAgent 会推送到前端）
                if (this instanceof ToolCallAgent toolCallAgent) {
                    String finalAnswer = toolCallAgent.getLastFinalAnswer();
                    if (finalAnswer != null && !finalAnswer.isEmpty()) {
                        return finalAnswer;
                    }
                }
                return "思考完成，不需要执行";
            }
            return act();
        }catch (Exception e)
        {
            setState(AgentState.ERROR);
            e.printStackTrace();
            return "步骤执行失败" + e.getMessage();
        }
    }
}
