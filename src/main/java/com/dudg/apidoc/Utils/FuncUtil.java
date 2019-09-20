package com.dudg.apidoc.Utils;

import cn.hutool.core.map.MapUtil;

import java.util.*;

/**
 * @class: FuncUtil
 * @description:
 * @author: dudg
 * @create: 2019-09-03 15:03
 */
public class FuncUtil {

    /**
     *@Description: 返回map中最大key值
     *@Param: [map]
     *@return: java.lang.Integer
     *@Author: dudg
    */
    public static Integer getSetMax(Set<Integer> set) {
        if (set == null || set.isEmpty()){
            return 1;
        }
        Object[] obj = set.toArray();
        Arrays.sort(obj);
        return Integer.parseInt(obj[obj.length - 1].toString())+1;
    }

    /**
     * @description: map对象拷贝
     * @param paramMap
     * @param resultMap
     * @return: void
     * @author: dudg
     * @date: 2019/9/20 16:35
    */
    public static Map mapCopy(Map paramMap){
        if(MapUtil.isEmpty(paramMap)) return null;
        Map resultMap = MapUtil.newHashMap();

        Iterator it = paramMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            Object key = entry.getKey();
            resultMap.put(key, paramMap.get(key) != null ? paramMap.get(key) : "");
        }

        return resultMap;
    }
}
