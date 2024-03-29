package com.dudg.apidoc.entity;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;

/**
 * 文档接口信息
 */
public class ApidocAction implements Serializable{

    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    private Integer id;
    /**
     * 名称
     */
    private String name;
    /**
     * 接口定义信息
     */
    private String apiDefine;
    /**
     * 模块id
     */
    private Integer moduleId;
    /**
     * 排序
     */
    private Integer order;
    /**
     * 方法的唯一标示符，方法名-形参类型,形参类型
     * 为了区别java方法的重载
     */
    private String methodUUID;

    /**
     * 接口描述
     */
    private String description;

    /**
     * 请求参数描述
     */
    private String requestDescription;

    /**
     * 响应参数描述
     */
    private String responseDescription;

    /**
     * 参数描述
     */
    private Map<String,String> paramMap;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getApiDefine() {
        return apiDefine;
    }

    public void setApiDefine(String apiDefine) {
        this.apiDefine = apiDefine;
    }

    public Integer getModuleId() {
        return moduleId;
    }

    public void setModuleId(Integer moduleId) {
        this.moduleId = moduleId;
    }

    public Integer getOrder() {
        return order;
    }

    public void setOrder(Integer order) {
        this.order = order;
    }

    public String getMethodUUID() {
        return methodUUID;
    }

    public void setMethodUUID(String methodUUID) {
        this.methodUUID = methodUUID;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getRequestDescription() {
        return requestDescription;
    }

    public void setRequestDescription(String requestDescription) {
        this.requestDescription = requestDescription;
    }

    public String getResponseDescription() {
        return responseDescription;
    }

    public void setResponseDescription(String responseDescription) {
        this.responseDescription = responseDescription;
    }

    public Map<String, String> getParamMap() {
        return paramMap;
    }

    public void setParamMap(Map<String, String> paramMap) {
        this.paramMap = paramMap;
    }

    @Override
    public String toString() {
        return "ApidocAction{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", moduleId=" + moduleId +
                ", order=" + order +
                ", methodUUID='" + methodUUID + '\'' +
                ", description='" + description + '\'' +
                ", requestDescription='" + requestDescription + '\'' +
                ", responseDescription='" + responseDescription + '\'' +
                '}';
    }

    /**
     * 重写equals和hashCode算法，只要methodUUID一致则认为两个action对象相同
     *
     * @param o
     * @return
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ApidocAction)) return false;
        ApidocAction that = (ApidocAction) o;
        return Objects.equals(getMethodUUID(), that.getMethodUUID());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getMethodUUID());
    }
}
