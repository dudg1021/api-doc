package com.dudg.apidoc.controller;

import com.dudg.apidoc.entity.ApidocAction;
import com.dudg.apidoc.entity.ApidocInfo;
import com.dudg.apidoc.entity.ApidocModule;
import com.dudg.apidoc.entity.bean.Detail;
import com.dudg.apidoc.service.ApiDocService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/apidoc")
public class ApiDocController {

    @Autowired
    private ApiDocService apiDocService;

    /**
     * 获取文档基本信息
     */
    @GetMapping("/info")
    public ApidocInfo info(String packageName) {
        return apiDocService.getInfo(packageName);
    }


    /**
     * 获取模块信息
     */
    @GetMapping("/modules")
    public List<ApidocModule> modules(String packageName) {
        return apiDocService.getModules(packageName);
    }

    /**
     * 获取接口列表信息
     * 也就是获取public修饰的方法 信息
     * 根据模块id获取该模块下所有类的public 的方法 信息
     */
    @GetMapping("/actions/{moduleId}")
    public List<ApidocAction> actions(@PathVariable Integer moduleId) {
        return apiDocService.getActions(moduleId);
    }

    /**
     * 获取接口详情
     */
    @GetMapping("/detail/{id}/{methodUUID}")
    public Detail detail(@PathVariable Integer id,@PathVariable String methodUUID) {
        return apiDocService.getDetail(id, methodUUID);
    }

}
