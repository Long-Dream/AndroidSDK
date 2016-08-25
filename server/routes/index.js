"use strict";

var express       = require('express');
var formidable    = require('formidable');
var fs            = require('fs');
var crypto        = require('crypto');
var router        = express.Router();
var cp            = require('child_process');

var db            = require('../database.js')("WandoujiaAPP")
var doSootAnalyse = require("../doSootAnalyse.js")

var resultpath    = "../result/";
var apkTempPath   = "../apkTemp/"

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
    fs.writeFile(resultpath + "analysis_minapilevel.txt", "", function(err){if(err) throw err; })


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
            doSootAnalyse(`../apkTemp/${apkMD5}.apk`, `../newSoot/newSoot.jar`, resultpath, ["analysis_api", "analysis_order", "analysis_permission", "analysis_sdk", "analysis_minapilevel"], function(err, data){
                
                if(err) throw err;

                console.log(data);

                db.collection("apkDetail").findOne({apkMD5 : apkMD5}, function(err, result){
                    if(err) throw err;

                    result.state                = 2;
                    result.analysis_api         = data.analysis_api;
                    result.analysis_order       = data.analysis_order;
                    result.analysis_permission  = data.analysis_permission;
                    result.analysis_sdk         = data.analysis_sdk;
                    result.analysis_minapilevel = data.analysis_minapilevel;
                    result.javaErr              = data.javaErr;
                    result.error                = data.textErr;

                    db.collection("apkDetail").update({ apkMD5 : apkMD5 }, result, function(err){
                        if(err) throw err;
                    })
                })
            });
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
                code                 : result.code,
                analysis_api         : result.analysis_api,
                analysis_order       : result.analysis_order,
                analysis_permission  : result.analysis_permission,
                analysis_sdk         : result.analysis_sdk,
                analysis_minapilevel : result.analysis_minapilevel,
                error                : result.error
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


module.exports = router;
