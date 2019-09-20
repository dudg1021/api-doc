package com.dudg.apidoc.Utils;

import cn.hutool.core.util.ReUtil;
import com.dudg.apidoc.common.Const;
import com.dudg.apidoc.entity.ApidocAction;
import org.springframework.util.StringUtils;

import java.io.*;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.text.MessageFormat;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 扫描包获得class对象
 */
public class ClassScanUtil {

    /**
     * 扫描指定包路径下所有包含指定注解的类
     * @param packageName 包名
     * @param apiClass    指定的注解
     * @return Set
     */
    public static Set<Class> getClass4Annotation(String packageName, Class<?> apiClass) {
        Set<Class> classSet = new HashSet<>();
        // 是否循环迭代
        boolean recursive = true;
        // 获取包的名字 并进行替换
        String packageDirName = packageName.replace('.', '/');
        // 定义一个枚举的集合 并进行循环来处理这个目录下的things
        Enumeration<URL> dirs;
        try {
            dirs = Thread.currentThread().getContextClassLoader().getResources(packageDirName);
            // 循环迭代下去
            while (dirs.hasMoreElements()) {
                // 获取下一个元素
                URL url = dirs.nextElement();
                // 得到协议的名称
                String protocol = url.getProtocol();
                // 如果是以文件的形式保存在服务器上
                if ("file".equals(protocol)) {
                    // 获取包的物理路径
                    String filePath = URLDecoder.decode(url.getFile(), "UTF-8");
                    // 以文件的方式扫描整个包下的文件 并添加到集合中
                    File dir = new File(filePath);
                    List<File> fileList = new ArrayList<File>();
                    fetchFileList(dir, fileList);
                    for (File f : fileList) {
                        String fileName = f.getAbsolutePath();
                        if (fileName.endsWith(".class")) {
                            String noSuffixFileName = fileName.substring(8 + fileName.lastIndexOf("classes"), fileName.indexOf(".class"));
                            String regex = "\\\\";
                            if (!"\\".equals(File.separator)) {
                                regex = File.separator;
                            }
                            String filePackage = noSuffixFileName.replaceAll(regex, ".");
                            Class clazz = Class.forName(filePackage);
                            if (null != clazz.getAnnotation(apiClass)) {
                                classSet.add(clazz);
                            }
                        }
                    }
                } else if ("jar".equals(protocol)) {
                    // 如果是jar包文件
                    // 定义一个JarFile
                    JarFile jar;
                    try {
                        // 获取jar
                        System.out.println(url);
                        JarURLConnection urlConnection = (JarURLConnection) url.openConnection();
                        jar = urlConnection.getJarFile();
                        // 从此jar包 得到一个枚举类
                        Enumeration<JarEntry> entries = jar.entries();
                        // 同样的进行循环迭代
                        while (entries.hasMoreElements()) {
                            // 获取jar里的一个实体 可以是目录 和一些jar包里的其他文件 如META-INF等文件
                            JarEntry entry = entries.nextElement();
                            String name = entry.getName();
                            // 如果是以/开头的
                            if (name.charAt(0) == '/') {
                                // 获取后面的字符串
                                name = name.substring(1);
                            }
                            // 如果前半部分和定义的包名相同
                            if (name.startsWith(packageDirName)) {
                                int idx = name.lastIndexOf('/');
                                // 如果以"/"结尾 是一个包
                                if (idx != -1) {
                                    // 获取包名 把"/"替换成"."
                                    packageName = name.substring(0, idx).replace('/', '.');
                                }
                                // 如果可以迭代下去 并且是一个包
                                // 如果是一个.class文件 而且不是目录
                                if (name.endsWith(".class") && !entry.isDirectory()) {
                                    // 去掉后面的".class" 获取真正的类名
                                    String className = name.substring(packageName.length() + 1, name.length() - 6);
                                    try {
                                        Class aClass = Class.forName(packageName + '.' + className);
                                        if (null != aClass.getAnnotation(apiClass)) {
                                            classSet.add(aClass);
                                        }
                                    } catch (ClassNotFoundException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return classSet;
    }


    /**
     * 查找所有的文件
     * @param dir      路径
     * @param fileList 文件集合
     */
    private static void fetchFileList(File dir, List<File> fileList) {
        if (dir.isDirectory()) {
            for (File f : Objects.requireNonNull(dir.listFiles())) {
                fetchFileList(f, fileList);
            }
        } else {
            fileList.add(dir);
        }
    }


    /**
     * 得到java类文件的字段和注释
     * @param className 类全名
     * @return Map
     */
    public synchronized static Map<String, String> getFieldsNotes(String className) {
        String path = getClassPath(className);
        if (!new File(path).exists()) {
            String sourceJarPath = getJarPath(className);
            if(StringUtils.isEmpty(sourceJarPath)){
                return null;
            }
            path = sourceJarPath;
        }

        //1.行注释 //
        //2.多行注释/*  */
        //3.文档注释 /**  */
        //文档注释可以 合并为多行注释
        try (BufferedReader bufferedReader = new BufferedReader(
                new InputStreamReader(getInputStream(path), Const.charSet), Const.bufferSize)
        ) {
            //存放字段的注释 key为字段名 value为注释
            Map<String, String> noteMap = new HashMap<>();
            String line;
            StringBuilder sb = new StringBuilder();
            String valueUp = null;
            while ((line = bufferedReader.readLine()) != null) {
                ////读取行注释
                //正则解释： 任意多个任意字符//任意多个任意字符
                boolean match = ReUtil.isMatch(".*//.*", line);
                if (line.contains("*")) {
                    valueUp = null;
                }
                if (match) {
                    //正则解释： 任意多个任意字符//
                    valueUp = line.replaceAll(".*//", "").trim();
                    //行注释只能是在代码行的上边或右边
                    //正则解释： //任意多个任意字符
                    String str = line.replaceAll("//.*", "");
                    //行注释位于代码行右边
                    if (!StringUtils.isEmpty(str.trim())) {
                        //正则解释： 空白字符
                        String[] split = str.split("\\s");
                        if (split.length > 0) {
                            //key
                            String key = split[split.length - 1].replace(";", "");
                            //value
                            //正则解释： 任意多个任意字符//
                            String value = line.replaceAll(".*//", "");
                            noteMap.put(key, value);
                        }
                    }
                } else {//行注释位于代码行上方
                    if (!StringUtils.isEmpty(valueUp) && line.contains(";") && line.contains("private")) {
                        //key
                        String[] split = line.split("\\s");
                        String key = split[split.length - 1].replace(";", "");
                        String value = valueUp;
                        valueUp = null;
                        noteMap.put(key, value);
                    }
                }
                sb.append(line);
            }
            //java文件的内容
            String str = sb.toString();
            ////读取多行注释
            //正则解释 /+1或2个* +任意个除{之外的字符非贪婪式匹配 +*/+人一个空白字符+private+任意个任意字符+;
            Pattern p = Pattern.compile("/\\*{1,2}[^{]*?\\*/\\s*private.+?;");
            Matcher m = p.matcher(str);
            while (m.find()) {
                String group = m.group();
                Pattern pkey = Pattern.compile("/\\*{1,2}.*?\\*/");
                Matcher mkey = pkey.matcher(group);
                mkey.find();
                String value = mkey.group();
                value = value.replaceAll("/|\\*", "").trim();

                Pattern pvalue = Pattern.compile("private.+?;");
                Matcher mvalue = pvalue.matcher(group);
                mvalue.find();
                String key = mvalue.group();
                String[] keys = key.split("\\s");
                key = keys[keys.length - 1].replaceAll(";", "");

                noteMap.put(key, value);
            }
            return noteMap;
        } catch (IOException e) {
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    /**
     * 获得类所有public方法的注释
     *
     * @param className 类的全名
     */
    public static Map<String, String> getMethodNotes(String className) {
        String path = getClassPath(className);
        //兼容class文件 和源代码
        if (!new File(path).exists()) {
            String sourceJarPath = getJarPath(className);
            if(StringUtils.isEmpty(sourceJarPath)){
                return new HashMap<>();
            }
            path = sourceJarPath;
        }
        //1.行注释 //
        //2.多行注释/*  */
        //3.文档注释 /**  */
        //文档注释可以合并为多行注释
        try (BufferedReader bufferedReader = new BufferedReader(
                new InputStreamReader(getInputStream(path), Const.charSet), Const.bufferSize)
        ) {
            // Variable Name 1
            String re1="(Description)";
            Pattern p1 = Pattern.compile(re1,Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
            //存放字段的注释 key为字段名 value为注释
            Map<String, String> noteMap = new HashMap<>();
            String line;
            StringBuilder sb = new StringBuilder();
            String valueUp = null;
            while ((line = bufferedReader.readLine()) != null) {
                ////读取行注释
                //行注释只能是在代码行的上边
                //正则解释： 任意多个空白字符//任意多个任意字符
                boolean match = ReUtil.isMatch("\\s*//.*", line);
                if (match) {
                    //正则解释： 任意多个任意字符//
                    valueUp = line.replaceAll(".*//", "").trim();
                } else {
                    if (!StringUtils.isEmpty(valueUp) && line.contains("public") && line.contains("(")) {
                        Pattern pvalue = Pattern.compile("public.+?\\(");
                        Matcher mvalue = pvalue.matcher(line);
                        mvalue.find();
                        String key = mvalue.group();
                        String[] keys = key.split("\\s");
                        key = keys[keys.length - 1].replaceAll("\\(|\\)", "");
                        String value = valueUp;
                        valueUp = null;
                        noteMap.put(className + "-" + key, value);
                    }
                }

                System.out.println("read every line content:"+line);
                Matcher m1 = p1.matcher(line);
                if (m1.find())
                {
                    String var1=m1.group();

                    System.out.print("("+var1+")"+"\n");
                }
                sb.append(line);
            }
            //java文件的内容
            String str = sb.toString();
            ////读取多行注释
            Pattern p = Pattern.compile("/\\*{1,2}[^/]*?\\*/[^**]+?public[^{]+?\\(");
            Matcher m = p.matcher(str);
            while (m.find()) {
                String group = m.group();
//                System.out.println(group);
                Pattern pkey = Pattern.compile("/\\*{1,2}.*?\\*/");
                Matcher mkey = pkey.matcher(group);
                mkey.find();
                String value = mkey.group();
                value = value.replaceAll("/|\\*", "").trim();
                value = value.split("\\s")[0];

                Pattern pvalue = Pattern.compile("public.+?\\(");
                Matcher mvalue = pvalue.matcher(group);
                mvalue.find();
                String key = mvalue.group();
                String[] keys = key.split("\\s");
                key = keys[keys.length - 1].replaceAll("\\(|\\)", "");

                noteMap.put(className + "-" + key, value);
            }

            return noteMap;
        } catch (IOException e) {
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    /**
     * 获得类所有public方法的注释
     * 方法注释模板：
     *  * @description
     *  * @param paramName1 描述
     *  * @param paramName2 描述
     *
     * @param className 类的全名
     */
    public static Map<String, ApidocAction> getMethodNotes2(String className, List<Method> methodList) {
        String path = getClassPath(className);
        //兼容class文件 和源代码
        if (!new File(path).exists()) {
            String sourceJarPath = getJarPath(className);
            if(StringUtils.isEmpty(sourceJarPath)){
                return new HashMap<>();
            }
            path = sourceJarPath;
        }

        //1.行注释 //
        //2.多行注释/*  */
        //3.文档注释 /**  */
        //文档注释可以合并为多行注释
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(getInputStream(path), Const.charSet), Const.bufferSize))
        {
            //存放字段的注释 key为字段名 value为注释
            Map<String, ApidocAction> noteMap = new HashMap<>();
            String line;
            String desc = null;
            Map<String,String> paramMap = new HashMap<>();
            while ((line = bufferedReader.readLine()) != null) {
                System.out.println(line);
                if (line.trim().startsWith("*") && !line.trim().endsWith("*/")) {
                    Pattern pattern = Pattern.compile("\\*\\s?@",Pattern.CASE_INSENSITIVE);
                    Pattern patternDesc = Pattern.compile("\\*\\s?@description(：|:)?\\s?",Pattern.CASE_INSENSITIVE);
                    Pattern patternParam = Pattern.compile("\\*\\s?@param(：|:)?\\s?",Pattern.CASE_INSENSITIVE);
                    if ((pattern.matcher(line)).find()) {
                        //1.匹配接口描述
                        Matcher matcherDesc = patternDesc.matcher(line);
                        Matcher matcherParam = patternParam.matcher(line);
                        if (matcherDesc.find()) {
                            desc = matcherDesc.replaceAll("").trim();
                            System.out.println("匹配到的 描述信息："+desc);
                        }
                        //2.匹配参数描述
                        else if(matcherParam.find()){
                            String paramDesc = matcherParam.replaceAll("").trim();
                            String[] paramKv = paramDesc.split(" ");
                            if(paramKv.length >1){
                                if(!paramMap.containsKey(paramKv[0].trim())){
                                    paramMap.put(paramKv[0].trim(),paramKv[1].trim());
                                }
                            }
                        }
                    }
                }


                String tmp = line;
                String tmpDesc = desc;
                //如果包含该方法 插入map
                methodList.stream().filter(item -> tmp.contains(item.getName().concat("("))).forEach(item -> {
                    String key = className + "-" + item.getName() + "-";
                    if(tmp.indexOf("(")<0 || tmp.indexOf(")")<0){
                        return;
                    }
                    String paramStr=tmp.substring(tmp.indexOf("(")+1,tmp.indexOf(")"));
                    int num = 0;
                    if (item.getParameters() != null) {
                        for (Parameter parameter : item.getParameters()) {
                            if (paramStr.contains(parameter.getType().getSimpleName())) {
                                key += parameter.getName() + "-";
                                num++;
                            }
                        }

                        if (num != item.getParameters().length) {
                            return;
                        }
                    }

                    if(StringUtils.isEmpty(noteMap.get(key.substring(0, key.length() - 1)))){
                        ApidocAction actionTmp = new ApidocAction();
                        actionTmp.setDescription(tmpDesc);
                        actionTmp.setApiDefine(tmp.replace("{",""));
                        if(!paramMap.isEmpty()){
                            actionTmp.setParamMap(FuncUtil.mapCopy(paramMap));
                        }
                        noteMap.put(key.substring(0, key.length() - 1), actionTmp);
                        System.out.println(MessageFormat.format("function name:{0},desc:{1}",key.substring(0, key.length() - 1),tmpDesc));
                        paramMap.clear();
                    }
                });
            }

            return noteMap;
        } catch (IOException e) {
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 根据类名获得java源文件的路径
     * @param className
     * @return
     */
    private static String getClassPath(String className) {
        String path = className.replace(".", File.separator) + ".java";
        path = Const.codePath + path;
        System.out.println(path);
        return path;
    }

    /**
     * 根据类命获得jar路径
     * @param className
    */
    public static String getJarPath(String className){
        String sourceJarPath = "";
        try {
            Class<?> aClass = Class.forName(className);
            String pkgDirName = aClass.getPackage().getName().replace('.', '/');
            Enumeration<URL> dirs = Thread.currentThread().getContextClassLoader().getResources(pkgDirName);
            // 循环迭代下去
            while (dirs.hasMoreElements()) {
                // 获取下一个元素
                URL url = dirs.nextElement();
                // 得到协议的名称
                String protocol = url.getProtocol();
                // 如果是以文件的形式保存在服务器上
                if ("jar".equals(protocol)) {
                    sourceJarPath = "jar:"+url.getPath().replace(".jar","-sources.jar")+"/"+aClass.getSimpleName()+".java";
                    break;
                }else{
                    sourceJarPath = url.getPath().replace("target/classes","src/main/java")+"/"+aClass.getSimpleName()+".java";
                }
            }
        }
        catch (Exception ex){
            sourceJarPath = null;
        }

        return sourceJarPath;
    }

    public static InputStream getInputStream(String filePath)
            throws Exception {
        if(!StringUtils.isEmpty(filePath) && filePath.startsWith("jar:file:")){
            URL url = new URL(filePath);
            JarURLConnection jarConnection = (JarURLConnection) url
                    .openConnection();
            return jarConnection.getInputStream();
        }
        else{
            return new FileInputStream(filePath);
        }
    }
}
