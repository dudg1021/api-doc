package com.dudg.apidoc.entity;

import java.io.Serializable;

/**
 * 文件基本信息
 */
public class ApidocInfo implements Serializable{

    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    private Integer id;
    /**
     * 文档标题
     */
    private String title = "文档标题";
    /**
     * 文档描述
     */
    private String description = "暂无描述";
    /**
     * 版本信息 如1.0.0
     */
    private String version = "1.0.0";
    /**
     * 包名，用于区别一个项目中的多个文档
     */
    private String packageName;


    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    @Override
    public String toString() {
        return "ApidocInfo{" +
                ", id=" + id +
                ", title=" + title +
                ", description=" + description +
                ", version=" + version +
                ", packageName=" + packageName +
                "}";
    }
}
