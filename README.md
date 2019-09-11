# api-doc
接口文档生成辅助工具


使用方式：

1.下载代码打包生成jar

2.在本地项目引入jar

3.增加包名扫描  com.dudg.apidoc
  springmvc  spring-dispatcher.xml <context:component-scan base-package="com.dudg.apidoc" />
  springboot 启动类  @ComponentScan(basepackage="com.dudg.apidoc")

4.在需要生成的类/接口 加上注解
  @Api(name = "功能模块描述")
  
5.启动项目，根据项目设置的端口地址(此处暂以8080端口)

包名：需要生成文档的类/接口所属的包名

http://localhost:8080/index.html?packagename=com.demo


注：如果项目中需要生成文档的接口引用了其它项目实体，需要在被引用的项目打源码jar

maven 方式在pom中加以下插件
<plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-source-plugin</artifactId>
        <version>3.0.0</version>
        <!-- 绑定source插件到Maven的生命周期,并在生命周期后执行绑定的source的goal -->
        <executions>
            <execution>
                <!-- 绑定source插件到Maven的生命周期 -->
                <phase>compile</phase>
                <!--在生命周期后执行绑定的source插件的goals -->
                <goals>
                    <goal>jar-no-fork</goal>
                </goals>
            </execution>
        </executions>
</plugin>



