package com.scmcloud.common.rest.config;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * Sentinel RestClient 闆嗘垚閰嶇疆
 * <p>鏇夸唬 OpenFeign 锟絊entinel 闆嗘垚</p>
 *
 * <p>鍔熻兘锟?
 * <ul>
 *   <li>閰嶇疆 @SentinelResource 闄愭祦瑙勫垯</li>
 *   <li>閰嶇疆鐔旀柇闄嶇骇瑙勫垯</li>
 *   <li>涓嶄緷锟紽eign锛屼娇鐢ㄩ€氱敤锟紷SentinelResource 娉ㄨВ</li>
 * </ul>
 *
 * <p>娉ㄦ剰锛氱敓浜х幆澧冨缓璁粠 Nacos 鍔ㄦ€佸姞杞借鍒欙紝鏈厤缃粎浣滀负榛樿瑙勫垯</p>
 *
 * @author Claude
 * @since 2025-12-29
 */
@Slf4j
@Configuration
public class SentinelRestClientConfiguration {

    /**
     * 鍒濆锟絊entinel 瑙勫垯
     */
    @PostConstruct
    public void initSentinelRules() {
        initFlowRules();
        initDegradeRules();
        log.info("Sentinel rules initialized for HTTP Exchange clients");
    }

    /**
     * 鍒濆鍖栭檺娴佽锟?
     * <p>涓烘瘡锟紿TTP Exchange 璧勬簮閰嶇疆 QPS 闄愭祦</p>
     */
    private void initFlowRules() {
        List<FlowRule> rules = new ArrayList<>();

        // 鐢ㄦ埛鏈嶅姟 - 鏇存柊鐧诲綍淇℃伅锛堟瘡绉掓渶锟?000 娆★級
        FlowRule userLoginRule = new FlowRule();
        userLoginRule.setResource("user-service:updateLastLogin");
        userLoginRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        userLoginRule.setCount(1000);
        userLoginRule.setLimitApp("default");
        rules.add(userLoginRule);

        // 璁よ瘉鏈嶅姟 - 寮哄埗鐧诲嚭锛堟瘡绉掓渶锟?00 娆★級
        FlowRule authLogoutRule = new FlowRule();
        authLogoutRule.setResource("auth-service:forceLogout");
        authLogoutRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        authLogoutRule.setCount(100);
        authLogoutRule.setLimitApp("default");
        rules.add(authLogoutRule);

        // 鏉冮檺鏈嶅姟 - 閫氳繃 URL 鏌ユ壘鏉冮檺锛堟瘡绉掓渶锟?00 娆★級
        FlowRule permByUrlRule = new FlowRule();
        permByUrlRule.setResource("permission-service:findByUrl");
        permByUrlRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        permByUrlRule.setCount(500);
        permByUrlRule.setLimitApp("default");
        rules.add(permByUrlRule);

        // 鏉冮檺鏈嶅姟 - 鏌ヨ鐢ㄦ埛鏉冮檺锛堟瘡绉掓渶锟?000 娆★級
        FlowRule permByUserRule = new FlowRule();
        permByUserRule.setResource("permission-service:getUserPermissions");
        permByUserRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        permByUserRule.setCount(1000);
        permByUserRule.setLimitApp("default");
        rules.add(permByUserRule);

        // 鏉冮檺鏈嶅姟 - 鏌ヨ鏉冮檺鏍戯紙姣忕鏈€锟?00 娆★級
        FlowRule permTreeRule = new FlowRule();
        permTreeRule.setResource("permission-service:getTree");
        permTreeRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        permTreeRule.setCount(200);
        permTreeRule.setLimitApp("default");
        rules.add(permTreeRule);

        // 鏉冮檺鏈嶅姟 - 鏌ヨ API 鏉冮檺锛堟瘡绉掓渶锟?00 娆★級
        FlowRule permApiRule = new FlowRule();
        permApiRule.setResource("permission-service:getApiPermissions");
        permApiRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        permApiRule.setCount(200);
        permApiRule.setLimitApp("default");
        rules.add(permApiRule);

        FlowRuleManager.loadRules(rules);
        log.info("Sentinel flow rules loaded: {} rules", rules.size());
    }

    /**
     * 鍒濆鍖栫啍鏂檷绾ц锟?
     * <p>鍩轰簬寮傚父姣斾緥杩涜鐔旀柇</p>
     */
    private void initDegradeRules() {
        List<DegradeRule> rules = new ArrayList<>();

        // 鏉冮檺鏈嶅姟 - 閫氳繃 URL 鏌ユ壘鏉冮檺锛堝紓甯告瘮锟?0% 瑙﹀彂鐔旀柇锟?
        DegradeRule permByUrlDegrade = new DegradeRule();
        permByUrlDegrade.setResource("permission-service:findByUrl");
        permByUrlDegrade.setGrade(RuleConstant.DEGRADE_GRADE_EXCEPTION_RATIO);
        permByUrlDegrade.setCount(0.5);  // 50% 寮傚父锟?
        permByUrlDegrade.setTimeWindow(10);  // 鐔旀柇鏃堕暱 10 锟?
        permByUrlDegrade.setMinRequestAmount(5);  // 鏈€灏忚姹傛暟 5
        permByUrlDegrade.setStatIntervalMs(1000);  // 缁熻鏃堕暱 1 锟?
        rules.add(permByUrlDegrade);

        // 鏉冮檺鏈嶅姟 - 鏌ヨ鐢ㄦ埛鏉冮檺锛堝紓甯告瘮锟?0% 瑙﹀彂鐔旀柇锟?
        DegradeRule permByUserDegrade = new DegradeRule();
        permByUserDegrade.setResource("permission-service:getUserPermissions");
        permByUserDegrade.setGrade(RuleConstant.DEGRADE_GRADE_EXCEPTION_RATIO);
        permByUserDegrade.setCount(0.5);  // 50% 寮傚父锟?
        permByUserDegrade.setTimeWindow(10);  // 鐔旀柇鏃堕暱 10 锟?
        permByUserDegrade.setMinRequestAmount(5);  // 鏈€灏忚姹傛暟 5
        permByUserDegrade.setStatIntervalMs(1000);  // 缁熻鏃堕暱 1 锟?
        rules.add(permByUserDegrade);

        // 鐢ㄦ埛鏈嶅姟 - 鏇存柊鐧诲綍淇℃伅锛堝紓甯告瘮锟?0% 瑙﹀彂鐔旀柇锟?
        DegradeRule userLoginDegrade = new DegradeRule();
        userLoginDegrade.setResource("user-service:updateLastLogin");
        userLoginDegrade.setGrade(RuleConstant.DEGRADE_GRADE_EXCEPTION_RATIO);
        userLoginDegrade.setCount(0.6);  // 60% 寮傚父锟?
        userLoginDegrade.setTimeWindow(15);  // 鐔旀柇鏃堕暱 15 锟?
        userLoginDegrade.setMinRequestAmount(10);  // 鏈€灏忚姹傛暟 10
        userLoginDegrade.setStatIntervalMs(1000);  // 缁熻鏃堕暱 1 锟?
        rules.add(userLoginDegrade);

        // 璁よ瘉鏈嶅姟 - 寮哄埗鐧诲嚭锛堝紓甯告瘮锟?0% 瑙﹀彂鐔旀柇锟?
        DegradeRule authLogoutDegrade = new DegradeRule();
        authLogoutDegrade.setResource("auth-service:forceLogout");
        authLogoutDegrade.setGrade(RuleConstant.DEGRADE_GRADE_EXCEPTION_RATIO);
        authLogoutDegrade.setCount(0.5);  // 50% 寮傚父锟?
        authLogoutDegrade.setTimeWindow(10);  // 鐔旀柇鏃堕暱 10 锟?
        authLogoutDegrade.setMinRequestAmount(5);  // 鏈€灏忚姹傛暟 5
        authLogoutDegrade.setStatIntervalMs(1000);  // 缁熻鏃堕暱 1 锟?
        rules.add(authLogoutDegrade);

        DegradeRuleManager.loadRules(rules);
        log.info("Sentinel degrade rules loaded: {} rules", rules.size());
    }
}
