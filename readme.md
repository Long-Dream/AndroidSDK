#Android 第三方 SDK 分析系统 网页端 Demo

## 简介
这是 Android 第三方 SDK 分析系统的网页端, 可供用户用于上传APK文件以及展示分析结果.

## 前提
作为服务器的电脑需要安装有以下内容

* jdk
* nodeJs
* npm
* mongodb

## 使用方法
将项目克隆到本地后, 先在终端运行`mongod`以打开数据库

再先在 server 目录下终端运行 `npm install` 安装必要的依赖

再将 android-platforms 文件夹拷贝至 newSoot 文件夹下

之后在 server 目录下终端运行 `npm start`, 则服务器会在本地的3000端口上启动(http://localhost:3000/),  之后便可以上传apk并进行分析

## 注意事项

* 需要定时清空 temp 文件夹