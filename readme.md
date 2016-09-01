#Android 第三方 SDK 分析系统 网页端

## 简介
这是 Android 第三方 SDK 分析系统的网页端, 可供用户用于上传APK文件以及展示分析结果.

## 前提
作为服务器的电脑需要安装有以下内容

* jdk
* nodeJs
* npm
* mongodb

## 构建方法
1. 将项目克隆到本地
2. 在 server 目录下终端运行 `npm install` 安装必要的依赖
3. 将 android-platforms 文件夹拷贝至 newSoot 文件夹下

## 使用方法
1. 终端运行 `mongod` 以启动服务器
2. 在 server 目录下终端运行 `npm start`, 则服务器会在本地的3000端口上启动(http://localhost:3000/),  之后便可以上传apk并进行分析