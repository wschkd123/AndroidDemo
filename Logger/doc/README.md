# Logger 组件说明

## 组件名称

Logger

## 组件说明

基于 XLog 封装的高性能日志组件，具备超高的写入效率，自带加密的特性。方便各业务记录核心日志

## 更新日志

2019.12.26 抽取 Logger 组件

## 组件核心包名和类

```java
com.qq.reader.component.logger
com.tencent.mars.xlog
```

## 组件配置说明（混淆、权限、so 库、assets 资源等）

* 混淆配置

```java
    -keep class com.qq.reader.component.logger.Logger {public <methods>;}
```

* 权限

    * 无
    
* so 库
    * libc++_shared.so
    * libmarsxlog.so

## 组件使用简单说明和其他说明（可以独立文件说明）

* XLog 相关参考 xlog 目录下的使用指南

## 组件额外文件清单
* xlog 目录下解密文件