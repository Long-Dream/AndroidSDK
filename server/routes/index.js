"use strict";

var express    = require('express');
var formidable = require('formidable');
var fs         = require('fs');
var crypto     = require('crypto');
var router     = express.Router();
var cp         = require('child_process');

var db         = require('../database.js')

var resultpath = "../result/";
var apkTempPath = "../apkTemp/"

/* GET home page. */
router.get('/', function(req, res, next) {
    res.render('index', { title: 'Express' });
});

/**
 * 实现文件的上传功能----后端实现
 */
router.post('/uploadAPK', function(req, res, next){

    // 事先清空所有 result 文件
    fs.writeFile(resultpath + "analysis_api.txt", "", function(err){if(err) throw err; })
    fs.writeFile(resultpath + "analysis_order.txt", "", function(err){if(err) throw err; })
    fs.writeFile(resultpath + "analysis_permission.txt", "", function(err){if(err) throw err; })
    fs.writeFile(resultpath + "analysis_sdk.txt", "", function(err){if(err) throw err; })


    var form = new formidable.IncomingForm();
    form.uploadDir = apkTempPath;
    form.parse(req, function(error, fields, files) {

        //将文件名以.分隔，取得数组最后一项作为文件后缀名。
        var types = files.apkFile.name.split('.'); 

        if(types[types.length - 1] !== "apk"){
            return res.send(`上传的文件格式错误!请重试!`)
        }

        // 上传时间的毫秒数
        var ms = new Date().getTime(); 
        var apkMD5 = md5(String(ms + Math.random()));
        fs.renameSync(files.apkFile.path, `${apkTempPath}${apkMD5}.apk`);

        // 存入数据库
        db.collection("apkDetail").insert({
            apkName : fields.apkFileName,
            apkMD5  : apkMD5,
            state   : 1
        }, function(err){
            if(err) throw err;
            res.send(apkMD5)
            doAnalyse(req, res, apkMD5);
        })
    }); 
})

/**
 * 定时检查分析结果----后端实现
 */
router.post('/checkAnalyse', function(req, res, next){

    var apkMD5 = req.body.apkMD5;

    db.collection("apkDetail").findOne({apkMD5 : apkMD5}, function(err, result){
        if(err) throw err;

        if(!result) return res.send({
            code    : 404,
            message : "数据未找到, 请重试!"
        })

        // 如果尚未分析完成, 则返回 -1
        if(result.state === 1) return res.send("-1");

        // 如果分析已经完成, 则返回分析结果
        if(result.state === 2){
            var obj = {
                code                : result.code,
                analysis_api        : result.analysis_api,
                analysis_order      : result.analysis_order,
                analysis_permission : result.analysis_permission,
                analysis_sdk        : result.analysis_sdk,
                error               : result.error
            };
            return res.send(obj);
        }
    })


})

/**
 * 计算给定值的 md5 值
 * @param  {string} text 给定值
 * @return {string}      md5
 */
function md5 (text) {
    return crypto.createHash('md5').update(text).digest('hex');
};

/**
 * 进行分析!
 */
function doAnalyse(req, res, apkMD5){

    var textErr = "";
    var javaErr = "";
        
    db.collection("apkDetail").findOne({
        apkMD5 : apkMD5
    }, function(err, result){
        if(err) throw err;

        var javaThread = cp.exec(`java -jar ../newSoot/newSoot.jar ${apkTempPath}${apkMD5}.apk`, {
            maxBuffer: 50000 * 1024
        },function(err, stdout, stderr){
            if(err) {

                result.state   = 2;
                result.code    = 500;
                result.javaErr = html_encode(err.toString());

                db.collection("apkDetail").update({ apkMD5 : apkMD5 }, result, function(err){
                    if(err) throw err;
                })
            }

            // console.log("stdout " + stdout)
            // console.log("stderr " + stderr)

            if(stderr){
                textErr = stderr.replace(/\n/g, "<br />");
            }
        })

        javaThread.on("exit", function(state){

            // 如果 java 进程是正常结束的, 那么读取所有分析结果, 并存入数据库
            if(state === 0){
                readFileToHandle(function(fileObj){
                    handleFile(fileObj, result, textErr, apkMD5)
                }, resultpath, ["analysis_api", "analysis_order", "analysis_permission", "analysis_sdk"])
            }

        })
    })

    /**
     * 读取文件, 以待回调函数处理
     * NOTICE : 文件必须是 txt 格式的文件
     * @param  {function} callback 全部读取完毕后的函数
     *                             回调函数的第一个参数是一个对象, 内容是文件名以及读取到的数据
     * @param  {string} filePath 文件路径
     * @param  {array}  fileName 文件名称, 不带扩展名
     *
     * @return 无返回, 此函数读取到的数据会以回调函数的第一个参数进行返回
     */
    function readFileToHandle(callback, filePath, fileName){

        // 获取将要获取的文件数量
        var fileNum = fileName.length;

        // 待装进回调函数的对象
        var obj = {};

        for(let i = 0; i < fileName.length; i++){
            fs.readFile(filePath + fileName[i] + ".txt", "utf8", function(err, data){
                if(err) throw err;
                obj[fileName[i]] = data;
                fileNum--;

                // 如果全部文件均已读取完成, 则调用回调函数
                if (!fileNum) callback(obj);
            })
        }
    }

    /**
     * 接上一个函数, 将读取到的文件信息进行处理, 加入数据库
     * @param  {object} result  从数据库返回到的文档
     * @param  {object} fileObj 读取到的文件信息
     * @param  {object} textErr java程序的报错信息
     * @param  {object} apkMD5  待分析 apk 的 md5
     */
    function handleFile(fileObj, result, textErr, apkMD5){

        // 将换行符变成标准的 HTML 的换行
        for (let i in fileObj){
            result[i] = html_encode(fileObj[i]);
        }

        // 添加成功代码
        result.state   = 2;
        result.code    = 200;
        result.error   = textErr

        // 更新数据库
        db.collection("apkDetail").update({ apkMD5 : apkMD5 }, result, function(err){
            if(err) throw err;
        })
    }
}

/**
 * 将非 HTML 的内容进行转义
 * @param  {string} str 待转义字符串
 * @return {string}     已转义字符串
 */
function html_encode(str) {   
    var s = "";   
    if (str.length == 0) return "";   
    s = str.replace(/&/g, "&gt;");   
    s = s.replace(/</g, "&lt;");   
    s = s.replace(/>/g, "&gt;");   
    s = s.replace(/ /g, "&nbsp;");   
    s = s.replace(/\'/g, "&#39;");   
    s = s.replace(/\"/g, "&quot;");   
    s = s.replace(/\n/g, "<br>");   
    return s;   
}   

module.exports = router;
