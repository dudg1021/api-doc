package com.dudg.apidoc.entity.bean;


/**
 * 接口详情 bean
 */
public class Detail {
    private String mapping;//接口地址映射 url
    private String requestMethod;//请求方式
    private String description = ""; //接口描述
    private Params requestParam;//请求参数
    private Params responseParam;//响应参数参数
    private String requestExample;//请求示例
    private String responseExample;//返回示例

    public String getMapping() {
        return mapping;
    }

    public void setMapping(String mapping) {
        this.mapping = mapping;
    }

    public String getRequestMethod() {
        return requestMethod;
    }

    public void setRequestMethod(String requestMethod) {
        this.requestMethod = requestMethod;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Params getRequestParam() {
        return requestParam;
    }

    public void setRequestParam(Params requestParam) {
        this.requestParam = requestParam;
    }

    public Params getResponseParam() {
        return responseParam;
    }

    public void setResponseParam(Params responseParam) {
        this.responseParam = responseParam;
    }

    public String getRequestExample() {
        return requestExample;
    }

    public void setRequestExample(String requestExample) {
        this.requestExample = requestExample;
    }

    public String getResponseExample() {
        return responseExample;
    }

    public void setResponseExample(String responseExample) {
        this.responseExample = responseExample;
    }
}
