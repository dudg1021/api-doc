package com.dudg.apidoc.service;

import cn.hutool.core.util.ReflectUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.dudg.apidoc.Utils.ClassScanUtil;
import com.dudg.apidoc.Utils.FuncUtil;
import com.dudg.apidoc.Utils.SpringUtil;
import com.dudg.apidoc.annotation.Api;
import com.dudg.apidoc.common.Const;
import com.dudg.apidoc.common.LocalData;
import com.dudg.apidoc.entity.ApidocAction;
import com.dudg.apidoc.entity.ApidocInfo;
import com.dudg.apidoc.entity.ApidocModule;
import com.dudg.apidoc.entity.ApidocParam;
import com.dudg.apidoc.entity.bean.Detail;
import com.dudg.apidoc.entity.bean.Params;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.core.io.InputStreamSource;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.multipart.MultipartFile;
import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;

/**
 *@Description: 生成api文档工具类
 *@Author: dudg
 *@date: 2019/8/8 18:30
*/
@Service
public class ApiDocService {

    /**
     * 封装基本类型和参数类型的对应关系
     */
    private static final Map<Class, String> typeMap = new HashMap<>();

    //初始化
    static {
        typeMap.put(byte.class, Const.ByteStr);
        typeMap.put(short.class, Const.ShortStr);
        typeMap.put(int.class, Const.IntegerStr);
        typeMap.put(long.class, Const.LongStr);
        typeMap.put(float.class, Const.FloatStr);
        typeMap.put(double.class, Const.DoubleStr);
        typeMap.put(char.class, Const.string);
        typeMap.put(boolean.class, Const.booleann);
    }

    /**
     *@Description: 获取文档基本信息
     *@Param: [packageName]
     *@return: com.dudg.apidoc.entity.ApidocInfo
     *@Author: dudg
     *@date: 2019/8/8 18:33
    */
    public ApidocInfo getInfo(String packageName) {
        ApidocInfo info = new ApidocInfo();
        info.setPackageName(packageName);
        return info;
    }

    /**
     *@Description: 获取模块信息
     * 先查询内存是否存在
     * 扫描package下的所有class得到模块，如果该模块数据库已存在则组合成list返回前端
     * 否则则解析代码并保存信息到数据库然后组合成list返回前端
     *@Param: [packageName]
     *@return: java.util.List<com.dudg.apidoc.entity.ApidocModule>
     *@Author: dudg
     *@date: 2019/8/9 10:31
    */
    public List<ApidocModule> getModules(String packageName) {
        //把list转为HashMap，方便快速查询
        final Map<String, ApidocModule> moduleMap = new HashMap<>();
        //返回前端的集合
        Set<ApidocModule> modules4front = new HashSet<>();
        //扫描包得到类
        Set<Class> classSet = ClassScanUtil.getClass4Annotation(packageName, Api.class);

        if (classSet.isEmpty()) {
            throw new RuntimeException("该包下没有class文件： " + packageName);
        } else {
            for (Class claszz : classSet) {
                //获取元数据
                Api api = (Api) claszz.getAnnotation(Api.class);
                //没有写明模块名称时 默认取类名全称 com.xxx.xxx
                String name = (api ==null || StringUtils.isEmpty(api.value())) ? claszz.getName():api.value();
                String className = claszz.getName();
                //判断数据库是否已经存在
                ApidocModule apidocModule = LocalData.moduleMap_Name.get(packageName);
                //模块已存在时，判断class是否已经存在模块信息中
                if (apidocModule != null) {
                    String classListStr = apidocModule.getClassList();
                    if (!classListStr.contains(className)) {
                        apidocModule.setClassList(apidocModule.getClassList() + "," + className);
                    }
                } else {//模块不存在时，新增
                    apidocModule = new ApidocModule();
                    apidocModule.setId(FuncUtil.getSetMax(LocalData.moduleMap_Id.keySet()));
                    apidocModule.setName(name);
                    apidocModule.setOrder(System.currentTimeMillis());
                    apidocModule.setPackageName(packageName);
                    apidocModule.setClassList(className);
                }

                //添加到返回集合
                modules4front.add(apidocModule);
                moduleMap.put(apidocModule.getPackageName(), apidocModule);
                LocalData.moduleMap_Id.put(apidocModule.getId(),apidocModule);
            }
        }
        //排序 并返回前端 默认按名称排序 如果存在order则按order排序
        List<ApidocModule> moduleList = new ArrayList<>(modules4front);
        moduleList.sort(Comparator.comparing(ApidocModule::getName));
        moduleList.sort(Comparator.comparing(ApidocModule::getOrder));

        LocalData.moduleMap_Name = moduleMap;
        return moduleList;
    }

    /**  获取接口列表信息
     *@Description: 获取模块下 所有public方法 组成的信息
     *@Param: [moduleId]
     *@return: java.util.List<com.dudg.apidoc.entity.ApidocAction>
     *@Author: dudg
     *@date: 2019/8/9 11:20
    */
//    @Transactional
    public List<ApidocAction> getActions(Integer moduleId) {
        //查询模块有几个class组成
        String classListStr = null;
        ApidocModule apidocModule = LocalData.moduleMap_Id.get(moduleId);
        classListStr = apidocModule != null?apidocModule.getClassList():"";
        if (StringUtils.isEmpty(classListStr)) {
            return null;
        }
        String[] classList = classListStr.split(",");
        if (classList.length > 0) {
            //获得该模块下数据库中所有的接口
            List<ApidocAction> actions4db = getActionListByModuleId(moduleId);
            //为了方便快速查询，list转map
            Map<String, ApidocAction> actionMap = new HashMap<>();
            actions4db.forEach(m -> actionMap.put(m.getMethodUUID(), m));
            //返回前台的list
            List<ApidocAction> actions4front = new ArrayList<>();

            for (int i = 0; i < classList.length; i++) {
                String className = classList[i];
                Class claszz = getClassByName(className);
                //获得本类的所有public方法
                if (claszz != null) {
                    Method[] methods = claszz.getMethods();
                    List<Method> methodList = new LinkedList<>(Arrays.asList(methods));
                    Class superclass = claszz.getSuperclass();
                    //移除父类中方法
                    if(superclass != null){
                        Method[] superclassMethods = superclass.getMethods();
                        List<Method> superclassMethodsList = new LinkedList<>(Arrays.asList(superclassMethods));

                        methodList.removeAll(superclassMethodsList);
                    }

                    //获得该类文件的所有public方法的注释
                    //StringUtil.getMethodNotes(className);
                    Map<String, ApidocAction> methodNotes = ClassScanUtil.getMethodNotes2(className,methodList);

                    //获得该类的public方法信息
                    if (methodList.size() > 0) {
                        List<ApidocAction> actions = new ArrayList<>(methods.length);
                        for (Method method : methodList) {
                            //todo 暂时没处理方法重载
                            //方法名 格式：全类名-方法名
                            String methodUUID = claszz.getName() + "-" + method.getName()+"-";

                            for (Parameter parameter:method.getParameters()) {
                                methodUUID += parameter.getName()+"-";
                            }

                            methodUUID = methodUUID.substring(0,methodUUID.length()-1);
                            //返回前端的数据
                            ApidocAction action = actionMap.get(methodUUID);
                            //判断数据库中是否已经存在该接口信息,不存在时添加
                            if (action == null) {
                                action = new ApidocAction();
                                action.setId(FuncUtil.getSetMax(LocalData.actionMap_Id.keySet()));
                                action.setMethodUUID(methodUUID);
                                //得到方法的注释信息
                                ApidocAction desc = methodNotes.get(methodUUID);
                                //有注释时用注释 没有时默认方法的名称
                                action.setName(method.getName());
                                action.setOrder(Integer.MAX_VALUE);
                                action.setModuleId(moduleId);

                                if(desc != null){
                                    action.setDescription(desc.getDescription());
                                    action.setApiDefine(desc.getApiDefine());
                                }

                                //存储
                                LocalData.actionMap_Id.put(action.getId(),action);
                            }
                            //添加到list
                            actions.add(action);
                        }
                        //添加到总actions
                        actions4front.addAll(actions);
                        actions.forEach(m -> actionMap.put(m.getMethodUUID(), m));
                    }
                }
            }

            //排序 默认按名称排序 存在order时,order优先排序
            actions4front.sort(Comparator.comparing(ApidocAction::getName));
            actions4front.sort(Comparator.comparing(ApidocAction::getOrder));
            return actions4front;
        }
        return null;
    }


    private List<ApidocAction> getActionListByModuleId(Integer moduleId){
        List<ApidocAction> result = new ArrayList<>();
        for (Integer item:LocalData.actionMap_Id.keySet()) {
            if(LocalData.actionMap_Id.get(item).getModuleId().equals(moduleId)){
                result.add(LocalData.actionMap_Id.get(item));
            }
        }

        return result;
    }


    /**
     *@Description: 根据方法名唯一标识 获取该方法的文档信息
     *@Param: [id, methodUUID]
     *@return: com.dudg.apidoc.entity.bean.Detail
     *@Author: dudg
     *@date: 2019/8/9 18:28
    */
    public Detail getDetail(Integer id, String methodUUID) {
        //拆分类名和方法名
        String[] split = methodUUID.split("-");
        if (split.length >= 2) {
            String className = split[0];
            Class claszz = getClassByName(className);
            if (claszz != null) {
                String methodName = split[1];
                //todo 方法可能存在重载，后期处理
                // 即：重名相同但是参数列表不同（参数的类型 数量 顺序不同）
                Method method = getMethod(claszz, methodName);
                if (method != null) {
                    //String mapping = SpringUtil.getMapping(claszz) + SpringUtil.getMapping(method);//url 映射 mapping 为类上的mapping+方法上的mapping

                    String mapping = "/"+ claszz.getSimpleName() + "/"+methodName;
                    //请求方式
                    String requestMethod = SpringUtil.getRequestMethod(method);
                    //请求参数和响应参数
                    Params requestParams = getParams(id, method);
                    Params responseParams = getReturn(id, method);

                    //返回前端数据
                    Detail detail = new Detail();
                    detail.setMapping(mapping);
                    detail.setRequestMethod(requestMethod);

                    ApidocAction action = LocalData.actionMap_Id.get(id);
                    detail.setDescription(action.getDescription());
                    detail.setRequestMethod(action.getApiDefine());
                    detail.setRequestParam(requestParams);
                    detail.setResponseParam(responseParams);


                    List<ApidocParam> paramListByActionId = getParamListByActionId(id, false);
                    if(!CollectionUtils.isEmpty(paramListByActionId)){
                        detail.setRequestExample(buildParams(paramListByActionId.get(0),"").replace(",@",""));
                    }

//                    tmpss = "";
//                    paramListByActionId = getParamListByActionId(id, true);
//                    if(!CollectionUtils.isEmpty(paramListByActionId)) {
//                        detail.setResponseExample(buildParams(paramListByActionId.get(0), ""));
//                    }

                    //取返回参数实例结构
                    detail.setResponseExample(OutPutReturnParamToJson(method));

                    return detail;
                }
            }
        }
        return null;
    }

    String tmpss = "";
    public String buildParams(ApidocParam apidocParam,String result){
        System.out.println(JSON.toJSONString(apidocParam));
        if(apidocParam == null || StringUtils.isEmpty(apidocParam.getDataType())){
            return result;
        }

        if(apidocParam.getDataType().contains("array")){
            if(StringUtils.isEmpty(result)){
                result = "\n[@\n]";
            }
            else{
                result = result.replace("@","[@\n]");
            }
        }
        else if(apidocParam.getDataType().contains("object")){
            if(StringUtils.isEmpty(result)){
                result = "\n{@\n}";
            }
            else{
                result = result.replace("@","\n{@\n}");
            }
        }
        else{
            if(StringUtils.isEmpty(result)){
                result = "\n{\n&nbsp;&nbsp;\"" + apidocParam.getName()+"\":&nbsp;&nbsp;"+apidocParam.getDefaultValue()+",@\n}";
            }
            else{
                result = result.replace("@","\n&nbsp;&nbsp;\"" + apidocParam.getName()+"\":&nbsp;&nbsp;"+apidocParam.getDefaultValue()+",@");
            }
        }
        tmpss = result;
        if(!CollectionUtils.isEmpty(apidocParam.getList())){
            for (int i = 0; i < apidocParam.getList().size(); i++) {
                buildParams(apidocParam.getList().get(i),tmpss);
            }

            tmpss = tmpss.replace(",@","");
        }

        return tmpss;
    }

    /**
     *@Description: 输出返回参数json结构
     *@Param: [method]
     *@return: java.lang.String
     *@Author: dudg
     *@date: 2019/9/11 15:21
    */
    private String OutPutReturnParamToJson(Method method){
        Type genericReturnType = method.getGenericReturnType();
        System.out.println(genericReturnType);
        String res = null;
        //获取返回值的泛型参数
        if(genericReturnType instanceof ParameterizedType){
            Type[] actualTypeArguments = ((ParameterizedType)genericReturnType).getActualTypeArguments();

            res = emptyClassToJson(actualTypeArguments[0].getTypeName());
            //判断是否是集合
            if(Collection.class.isAssignableFrom(((ParameterizedTypeImpl) genericReturnType).getRawType())){
                res = "[\n"+res+"\n]";
            }
        }
        else{
            res = emptyClassToJson(genericReturnType.getTypeName());
        }
        return res;
    }

    private String emptyClassToJson(String className){
        Class aClass = getClassByName(className);
        String result = "";
        if(aClass != null) {
            try {
                result = JSONObject.toJSONString(aClass.newInstance(),
                        SerializerFeature.PrettyFormat,
                        SerializerFeature.WriteNullStringAsEmpty,
                        SerializerFeature.WriteMapNullValue,
                        SerializerFeature.WriteNullBooleanAsFalse,
                        SerializerFeature.WriteNullNumberAsZero,
                        SerializerFeature.WriteNullListAsEmpty);
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
            System.out.println(result);
        }
        return result;
    }

    /**
     *@Description: 根据类全名获得类对象
     *@Param: [className]
     *@return: java.lang.Class
     *@Author: dudg
     *@date: 2019/8/12 15:35
    */
    private Class getClassByName(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     *@Description: 获得方法的参数列表
     *@Param: [id, method]
     *@return: com.dudg.apidoc.entity.bean.Params
     *@Author: dudg
     *@date: 2019/8/12 17:36
    */
    private Params getParams(Integer id, Method method) {
        String type = getType(method);
        String description = LocalData.actionMap_Id.get(id).getRequestDescription();
        List<ApidocParam> apidocParams = getParams(method, id);
        System.err.println("转化前的请求参数： " + JSON.toJSONString(apidocParams));
        List<ApidocParam> apidocParamList = list2Tree(apidocParams);
        System.err.println("转化完成的请求参数： " + JSON.toJSONString(apidocParamList));

        Params params = new Params();
        params.setType(type);
        params.setDescription(description);
        params.setParams(apidocParamList);
        return params;
    }

    /**
     * 将list数据转换为tree结构数据
     */
    private List<ApidocParam> list2Tree(List<ApidocParam> params) {
        if (null == params || params.size() == 0) {
            return null;
        }
        List<ApidocParam> trees = new ArrayList<>();
        for (ApidocParam treeNode : params) {
            if (0 == treeNode.getPid()) {
                trees.add(treeNode);
                if(!CollectionUtils.isEmpty(treeNode.getList())){
                    treeNode.getList().clear();
                }
            }
            for (ApidocParam it : params) {
                if (it.getPid().equals(treeNode.getId())) {
                    if (treeNode.getList() == null) {
                        treeNode.setList(new ArrayList<>());
                    }
                    if(!CollectionUtils.isEmpty(it.getList())) {
                        it.getList().clear();
                    }
                    treeNode.getList().add(it);
                }
            }
        }
        //如果参数个数为一个 且是对象类型且拥有子参数 去掉第一个参数 =》符合spring的参数规范
        if (trees.size() == 1 && trees.get(0).getList() != null && trees.get(0).getDataType().contains("object")) {
            return trees.get(0).getList();
        }
        return trees;
    }

    /**
     * 获取参数列表
     */
    private List<ApidocParam> getParams(Method method, Integer actionId) {
        //查询存储，参数如果存在则直接返回 否则解析代码生成
        List<ApidocParam> list =  getParamListByActionId(actionId,false);
        if (list.isEmpty()) {
            return generateParams(method, actionId);
        }
        return list;
    }


    private List<ApidocParam> getParamListByActionId(Integer actionId,Boolean isReturn){
        List<ApidocParam> result = new ArrayList<>();
        for (Integer item:LocalData.paramMap_Id.keySet()) {
            ApidocParam apidocParam = LocalData.paramMap_Id.get(item);
            if(apidocParam.getActionId().equals(actionId) && apidocParam.getReturnd().booleanValue() == isReturn.booleanValue()){
                result.add(apidocParam);
            }
        }

        return result;
    }

    /**
     * 解析java代码 构建请求参数
     */
    private List<ApidocParam> generateParams(Method method, Integer actionId) {
        List<ApidocParam> list = new ArrayList<>();
        //1.得到参数名
        String[] paramNames = new LocalVariableTableParameterNameDiscoverer().getParameterNames(method);
        //Parameter[] paramNames = method.getParameters();

        //2.得到参数类型
        Class<?>[] parameterTypes = method.getParameterTypes();
        //3.得到参数的通用类型
        Type[] genericParameterTypes = method.getGenericParameterTypes();
        //遍历所有参数，解析每个参数
        for (int i = 0; i < parameterTypes.length; i++) {
            //请求参数的类型 只能是 字符串string 数字number 自定义对象 数组（普通数组，对象数组） list map 文件
//            isType(list, actionId, paramNames[i].getName(), parameterTypes[i], genericParameterTypes[i], null, 0, true, false);
            isType(list, actionId, paramNames[i], parameterTypes[i], genericParameterTypes[i], null, 0, true, false);
        }
        System.err.println(JSON.toJSONString(list));
        return list;
    }


    private Class class4data = null;
    /**
     * 判断参数的类型
     * 请求参数的类型 只能是 字符串string 数字number 自定义对象 数组（普通数组，对象数组） 泛型（list map） 文件 boolean 日期Data
     * <p>
     *
     * @param list      参数项列表
     * @param actionId  接口详情id
     * @param paramName 参数名
     * @param tclass    参数的Class
     * @param genType   参数的通用类型
     * @param pclass    父class
     * @param pid       父id 父对象名称
     * @param isSelf    是否是对象的自嵌套，用于判断对象的自嵌套
     * @param isReturn  是否是返回值，用于标注参数属于返回值
     */
    private synchronized void isType(List<ApidocParam> list, Integer actionId, String paramName, Class tclass, Type genType,
                                     Class pclass, Integer pid, boolean isSelf, boolean isReturn) {
        ApidocParam item = new ApidocParam();
        item.setId(FuncUtil.getSetMax(LocalData.paramMap_Id.keySet()));
        item.setPid(pid);
        //基本类型可能取不到class的名称 这里toString一下
        if (StringUtils.isEmpty(paramName)) {
            paramName = tclass.toString();
        }
        item.setName(paramName);
        //item.setDefaultValue(getObjectDefaultValue(tclass) + "");
        //item.setRequired(true);
        item.setActionId(actionId);
        //如果是返回值的参数需要标注
        if (isReturn) {
            item.setReturnd(true);
        }
        else{
            item.setReturnd(false);
        }
        //设置所属类名
        if (pclass != null) {
            String pclassName = pclass.getName();
            item.setPclassName(pclassName);
            Map<String, String> fieldsNotes = ClassScanUtil.getFieldsNotes(pclassName);
            if (fieldsNotes != null) {
                String description = fieldsNotes.get(paramName);
                if (!StringUtils.isEmpty(description)) {
                    item.setDescription(description);
                    item.setPclassName(pclassName);
                } else {
                    item.setDescription(paramName);
                    item.setPclassName("0");
                }
            }
        } else {
            item.setDescription(paramName);
            item.setPclassName("0");
        }

        LocalData.paramMap_Id.put(item.getId(),item);

        //设置参数类型
        //数组 或者多维数组
        if (tclass.isArray()) {
            //获得数组类型
            Class typeClass = tclass.getComponentType();
            String shortName = typeClass.getSimpleName();
            item.setDataType(Const.array + shortName);
            LocalData.paramMap_Id.put(item.getId(),item);

            //添加到list
            list.add(item);
            //处理多维数组
            isType(list, actionId, typeClass.getSimpleName().toLowerCase(), typeClass, null, tclass, item.getId(), isSelf, isReturn);
        }

        //泛型 或泛型中嵌套泛型
        if (genType instanceof ParameterizedType) {
            ParameterizedType aType = (ParameterizedType) genType;
            Type[] parameterArgTypes = aType.getActualTypeArguments();
            //todo 先支持collection和map类型 后期有需要再加
            if (Collection.class.isAssignableFrom(tclass)) {
                Type type = parameterArgTypes[0];
                String[] split = type.getTypeName().split("<");
                if (split.length > 0) {
                    Class typeClass = null;
                    try {
                        typeClass = Class.forName(split[0]);
                        item.setDataType(Const.array + typeClass.getSimpleName());
                        LocalData.paramMap_Id.put(item.getId(),item);

                        list.add(item);
                        isType(list, actionId, typeClass.getSimpleName().toLowerCase(), typeClass, type, tclass, item.getId(), isSelf, isReturn);
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                }
                // 是 Map map比较特殊，只能运行时得到值
            } else if (Map.class.isAssignableFrom(tclass)) {
                item.setDataType(Const.object + "Map");
                LocalData.paramMap_Id.put(item.getId(),item);

                list.add(item);
                // 针对自定义类型 Result<T> 特殊处理
            } else if (parameterArgTypes.length == 1 && tclass.getName().contains("Result")) {
                //保存对象中属性名为data的类型
                class4data = (Class) parameterArgTypes[0];
            }
        }

        //基本类型 ：字符串，数字，文件，时间日期类型
        //数字
        if(Number.class.isAssignableFrom(tclass)){
            item.setDataType(tclass.getSimpleName());
            LocalData.paramMap_Id.put(item.getId(),item);

            list.add(item);
        }
        //字节类型
        if(byte.class.isAssignableFrom(tclass)){
            item.setDataType(tclass.getSimpleName());
            LocalData.paramMap_Id.put(item.getId(),item);

            list.add(item);
        }
        //字符串
        if (CharSequence.class.isAssignableFrom(tclass) || Character.class.isAssignableFrom(tclass)) {
            item.setDataType(tclass.getSimpleName());
            LocalData.paramMap_Id.put(item.getId(),item);

            list.add(item);
        }
        //boolean
        if (Boolean.class.isAssignableFrom(tclass) || Const.booleann.equals(typeMap.get(tclass))) {
            item.setDataType(tclass.getSimpleName());
            LocalData.paramMap_Id.put(item.getId(),item);

            list.add(item);
        }

        //文件 MultipartFile
        if (InputStreamSource.class.isAssignableFrom(tclass)) {
            item.setDataType(Const.file);
            LocalData.paramMap_Id.put(item.getId(),item);

            list.add(item);
        }
        //文件 MultipartFile
        else if (Date.class.isAssignableFrom(tclass)) {
            item.setDataType(Const.date);
            LocalData.paramMap_Id.put(item.getId(),item);

            list.add(item);
        }

        //自定义对象类型
        if (isMyClass(tclass)) {
            //自定义对象类型为对象的名称
            item.setDataType(Const.object + tclass.getSimpleName());
            LocalData.paramMap_Id.put(item.getId(),item);

            list.add(item);
            //获得对象的所有字段 包括继承的所有父类的属性
            Field[] fields = ReflectUtil.getFieldsDirectly(tclass, true);
            //排除static和final修饰的属性
            List<Field> fieldList = removeStaticAndFinal(fields);
            if (fieldList.size() > 0) {
                for (Field field : fieldList) {
                    Class<?> typeClass = field.getType();
                    String fieldName = field.getName();
                    //针对自定义类型 Result<T> 特殊处理
                    if (class4data != null) {
                        typeClass = class4data;
                        class4data = null;
                    }
                    //考虑对象的字段可能是对象  可能存在 自嵌套 互相嵌套的类
                    //自嵌套  只走一次
                    if (typeClass == tclass && isSelf) {
                        isType(list, actionId, fieldName, typeClass, null, tclass, item.getId(), false, isReturn);
                    }
                    if (typeClass != tclass) {
                        isType(list, actionId, fieldName, typeClass, null, tclass, item.getId(), isSelf, isReturn);
                    }
                }
            }
        }//isObject end
    }


    /**
     *@Description: 获取类型默认值
     *@Param: [tclass]
     *@return: java.lang.Object
     *@Author: dudg
     *@date: 2019/9/10 18:40
    */
    private Object getObjectDefaultValue(Class tclass) {
        Object defaultValue = null;
        //数字类型 默认值 0
        if (Number.class.isAssignableFrom(tclass)) {
            defaultValue = 0;
        }
        //字符串类型 默认 ""
        else if (CharSequence.class.isAssignableFrom(tclass)) {
            defaultValue = "";
        }
//        //文件类型 默认"file"
//        else if (MultipartFile.class.isAssignableFrom(tclass)) {
//            defaultValue = "file";
//        }
        return defaultValue;
    }


    /**
     *@Description: 去除static的final修饰的字段
     *@Param: [fields]
     *@return: java.util.List<java.lang.reflect.Field>
     *@Author: dudg
     *@date: 2019/8/12 18:42
    */
    private List<Field> removeStaticAndFinal(Field[] fields) {
        List<Field> fieldList = new ArrayList<>();
        if (fields.length > 0) {
            for (Field field : fields) {
                String modifier = Modifier.toString(field.getModifiers());
                if (modifier.contains("static") || modifier.contains("final")) {
                    //舍弃
                } else {
                    fieldList.add(field);
                }
            }
        }
        return fieldList;
    }

    /**
     * 是否是自定义类型
     */
    private boolean isMyClass(Class<?> clz) {
        if (clz == null) {
            return false;
        }
        //排除 spring的文件类型
        if (MultipartFile.class.isAssignableFrom(clz)) {
            return false;
        }
        //排除数组
        if (clz.isArray()) {
            return false;
        }
        //Object 类型特殊处理
        if (clz == Object.class) {
            return true;
        }
        //只能是jdk的根加载器
        return clz.getClassLoader() != null;
    }

    /**
     * 获得指定method的请求方式
     */
    private String getType(Method method) {
        //获得参数类型
        //get: url ,path
        //post: from,json
        //put: json
        //delete: path
        String type = Const.JSON;
        String requestMethod = SpringUtil.getRequestMethod(method);
        switch (requestMethod) {
            case Const.GET:
            case Const.DELETE:
                if (containsPathVariableAnnotation(method.getParameterAnnotations())) {
                    type = Const.URI;
                } else {
                    type = Const.URL;
                }
                break;
            case Const.PUT:
            case Const.POST:
                if (containsRequestBodyAnnotation(method.getParameterAnnotations())) {
                    type = Const.JSON;
                } else {
                    type = Const.FROM;
                }
                break;
        }
        return type;
    }

    private boolean containsRequestBodyAnnotation(Annotation[][] parameterAnnotations) {
        for (Annotation[] annotations : parameterAnnotations) {
            for (Annotation annotation : annotations) {
                if (annotation instanceof RequestBody) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean containsPathVariableAnnotation(Annotation[][] parameterAnnotations) {
        for (Annotation[] annotations : parameterAnnotations) {
            for (Annotation annotation : annotations) {
                if (annotation instanceof PathVariable) {
                    return true;
                }
            }
        }
        return false;
    }


    /**
     * 根据名称获得方法对象
     */
    private Method getMethod(Class<?> claszz, String name) {
        Method[] methods = claszz.getMethods();
        for (Method m : methods) {
            if (name.equals(m.getName())) {
                return m;
            }
        }
        return null;
    }

    /**
     *@Description: 得到返回参数类型 组成的参数列表
     *@Param: [actionId, method]
     *@return: com.dudg.apidoc.entity.bean.Params
     *@Author: dudg
     *@date: 2019/9/10 18:44
    */
    private Params getReturn(Integer actionId, Method method) {
        //1.封装响应数据
        Params params = new Params();
        //获得方法的返回值
        Class<?> rclass = method.getReturnType();
        //2.设置请求或响应类型
        if (rclass.getTypeName().equals(void.class.getTypeName())) {
            params.setType(Const.BLOB);
        }
        else if(rclass.getName().equals(int.class.getTypeName())){
            params.setType(Const.IntegerStr);
        }
        else if(rclass.getName().equals(boolean.class.getTypeName())){
            params.setType(Const.booleann);
        }
        else {
            params.setType(Const.JSON);
        }
        //3.设置描述
        params.setDescription(LocalData.actionMap_Id.get(actionId).getResponseDescription());
        //查询数据库，存在返回参数之间返回，否则解析代码生成写入数据库并返回
        List<ApidocParam> list = getParamListByActionId(actionId, true);
        if (null == list || list.isEmpty()) {
            list = new ArrayList<>();
            //得到通用类型
            Type genericParameterTypes = method.getGenericReturnType();
            isType(list, actionId, rclass.getSimpleName().toLowerCase(), rclass, genericParameterTypes, null, 0, true, true);
        }
        System.err.println(JSON.toJSONString(list));
        //转tree结构
        List<ApidocParam> paramItemList = list2Tree(list);
        System.err.println("转换后的  " + JSON.toJSONString(paramItemList));
        //4.设置参数列表
        params.setParams(paramItemList);
        return params;
    }
}
