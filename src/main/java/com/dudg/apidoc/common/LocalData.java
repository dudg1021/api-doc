package com.dudg.apidoc.common;

import com.dudg.apidoc.entity.ApidocAction;
import com.dudg.apidoc.entity.ApidocModule;
import com.dudg.apidoc.entity.ApidocParam;

import java.util.HashMap;
import java.util.Map;

public class LocalData {

    /**
     * 模块名称,集合map
     */
    public static Map<String, ApidocModule> moduleMap_Name = new HashMap<>();

    /**
     * 模块id,集合map
     */
    public static Map<Integer, ApidocModule> moduleMap_Id = new HashMap<>();

    /**
     * 接口id,信息map
     */
    public static Map<Integer, ApidocAction> actionMap_Id = new HashMap<>();

    /**
     * 接口id,参数集合map
     */
    public static Map<Integer, ApidocParam> paramMap_Id = new HashMap<>();

}
