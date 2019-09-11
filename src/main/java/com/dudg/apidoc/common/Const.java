package com.dudg.apidoc.common;

import java.io.File;

public class Const {
    //项目路径
    public static String projectPath;
    //代码的存放路径
    public static String codePath;

    static {
//        projectPath = new File("").getAbsolutePath() + File.separator;
        projectPath = Thread.currentThread().getContextClassLoader().getResource("").getPath().replace("/target/classes","");
        if(projectPath.startsWith("/")){
            projectPath = projectPath.substring(1);
        }
        codePath = projectPath + "src" + File.separator + "main" + File.separator + "java" + File.separator;
    }
    //字符集
    public static final String charSet = "utf-8";
    //521k
    public static final int bufferSize = 512 * 1024;

    //---类型常量---
    // 字符串string 数字number 自定义对象 数组（普通数组，对象数组） 泛型（list map） 文件 boolean 日期Data
    public static final String string = "string";
    public static final String number = "number";
    public static final String FloatStr = "Float";
    public static final String LongStr = "Long";
    public static final String DoubleStr = "Double";
    public static final String IntegerStr = "Integer";
    public static final String DecimalStr= "Integer";
    public static final String ShortStr = "Short";
    public static final String ByteStr = "Byte";
    public static final String object = "object 对象: ";
    public static final String array = "array 数组: ";
    public static final String booleann = "boolean 布尔类型（是/否）";
    public static final String file = "file 文件";
    public static final String date = "date 日期时间";

    //----HTTP 动作-------
    public static final String GET = "GET";
    public static final String PUT = "PUT";
    public static final String POST = "POST";
    public static final String DELETE = "DELETE";


    //-----请求或响应方式（类型）--------
    public static final String JSON = "JSON类型数据";
    public static final String URL = "URL拼接参数 (示例: ?a=XX&&b=XX)";
    public static final String URI = "URI占位符 (示例: /XXX/{id}/{name})";
    public static final String FROM = "FROM表单数据";
    public static final String BLOB = "BLOB二进制流";
}
