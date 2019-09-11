package com.dudg.apidoc.Utils;

import java.util.Arrays;
import java.util.Set;

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
}
