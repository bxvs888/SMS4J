package org.dromara.sms4j.javase.config;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.io.resource.ClassPathResource;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.dromara.sms4j.aliyun.config.AlibabaConfig;
import org.dromara.sms4j.api.SmsBlend;
import org.dromara.sms4j.api.universal.SupplierConfig;
import org.dromara.sms4j.cloopen.config.CloopenConfig;
import org.dromara.sms4j.comm.config.SmsConfig;
import org.dromara.sms4j.comm.constant.Constant;
import org.dromara.sms4j.comm.exception.SmsBlendException;
import org.dromara.sms4j.comm.factory.BeanFactory;
import org.dromara.sms4j.comm.utils.SmsUtil;
import org.dromara.sms4j.core.factory.SmsFactory;
import org.dromara.sms4j.emay.config.EmayConfig;
import org.dromara.sms4j.huawei.config.HuaweiConfig;
import org.dromara.sms4j.javase.util.YamlUtil;
import org.dromara.sms4j.jdcloud.config.JdCloudConfig;
import org.dromara.sms4j.netease.config.NeteaseConfig;
import org.dromara.sms4j.provider.config.BaseConfig;
import org.dromara.sms4j.provider.factory.BaseProviderFactory;
import org.dromara.sms4j.provider.factory.ProviderFactoryHolder;
import org.dromara.sms4j.tencent.config.TencentConfig;
import org.dromara.sms4j.unisms.config.UniConfig;
import org.dromara.sms4j.yunpian.config.YunpianConfig;
import org.dromara.sms4j.zhutong.config.ZhutongConfig;

import java.util.Map;

/**
 * 初始化类
 */
@Slf4j
public class SEInitializer {

    private static final SEInitializer INSTANCE = new SEInitializer();

    public static SEInitializer initializer() {
        return INSTANCE;
    }

    /**
     * 初始化短信公共配置
     *
     * @param smsConfig 短信公共配置
     * @return 当前初始化类实例
     */
    public SEInitializer initSmsConfig(SmsConfig smsConfig) {
        BeanUtil.copyProperties(smsConfig, BeanFactory.getSmsConfig());
        return this;
    }

    /**
     * 默认从sms-aggregation.yml文件中读取配置
     *
     * @return
     */
    public void fromYaml() {
        ClassPathResource yamlResouce = new ClassPathResource("sms4j.yml");
        this.fromYaml(yamlResouce.readUtf8Str());
    }

    /**
     * 从yaml中读取配置
     *
     * @param yaml yaml配置字符串
     */
    public void fromYaml(String yaml) {
        InitConfig config = YamlUtil.toBean(yaml, InitConfig.class);
        this.initConfig(config);
    }

    /**
     * 从json中读取配置
     *
     * @param json json配置字符串
     */
    public void fromJson(String json) {
        InitConfig config = JSONUtil.toBean(json, InitConfig.class);
        this.initConfig(config);
    }

    private void initConfig(InitConfig config) {
        if (config == null) {
            log.error("初始化配置失败");
            throw new SmsBlendException("初始化配置失败");
        }
        InitSmsConfig smsConfig = config.getSms();
        if (smsConfig == null) {
            log.error("初始化配置失败");
            throw new SmsBlendException("初始化配置失败");
        }

        this.initSmsConfig(smsConfig);
        // 解析供应商配置
        Map<String, Map<String, String>> blends = smsConfig.getBlends();
        for(String configId : blends.keySet()) {
            Map<String, String> configMap = blends.get(configId);
            String supplier = configMap.get(Constant.SUPPLIER_KEY);
            supplier = StrUtil.isEmpty(supplier) ? configId : supplier;
            BaseProviderFactory<SmsBlend, SupplierConfig> providerFactory = (BaseProviderFactory<SmsBlend, SupplierConfig>) ProviderFactoryHolder.requireForSupplier(supplier);
            if(providerFactory == null) {
                log.warn("创建\"{}\"的短信服务失败，未找到供应商为\"{}\"的服务", configId, supplier);
            }
            SmsUtil.replaceKeysSeperator(configMap, "-", "_");
            JSONObject configJson = new JSONObject(configMap);
            SupplierConfig supplierConfig = JSONUtil.toBean(configJson, providerFactory.getConfigClass());
            providerFactory.createSms(supplierConfig);
        }
    }

    /**
     * 初始化配置bean
     */
    @Data
    @EqualsAndHashCode
    @ToString
    public static class InitConfig {
        private InitSmsConfig sms;
    }

    /**
     * 初始化短信配置bean
     */
    @Data
    @EqualsAndHashCode(callSuper = true)
    @ToString(callSuper = true)
    public static class InitSmsConfig extends SmsConfig {
        private Map<String, Map<String, String>> blends;
    }

}
