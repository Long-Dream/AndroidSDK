var express    = require('express');
var formidable = require('formidable');
var fs         = require('fs');
var crypto     = require('crypto');
var router     = express.Router();
var cp         = require('child_process');

var db         = require('../database.js')

/* GET home page. */
router.get('/', function(req, res, next) {
  res.render('index', { title: 'Express' });
});

/**
 * 实现文件的上传功能----后端实现
 */
router.post('/uploadAPK', function(req, res, next){

    // 事先清空 result 文件
    fs.writeFile("../temp/result.txt", "", function(err){
        if(err) throw err;
    })

    var form = new formidable.IncomingForm();
    form.parse(req, function(error, fields, files) {

        //将文件名以.分隔，取得数组最后一项作为文件后缀名。
        var types = files.apkFile.name.split('.'); 

        if(types[types.length - 1] !== "apk"){
            return res.send("上传的文件格式错误!请重试!")
        }

        // 上传时间的毫秒数
        var ms = new Date().getTime(); 
        var apkMD5 = md5(String(ms + Math.random()));
        fs.renameSync(files.apkFile.path, "./apkTemp/" + apkMD5 + ".apk");

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
                code    : result.code,
                message : result.message,
                error   : result.error
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
        
    db.collection("apkDetail").findOne({
        apkMD5 : apkMD5
    }, function(err, result){
        if(err) throw err;

        var javaThread = cp.exec("java -jar ../newSoot/newSoot.jar " + apkMD5 + ".apk", {
            maxBuffer: 50000 * 1024
        },function(err, stdout, stderr){
            if(err) throw err;

            // console.log("stdout " + stdout)
            // console.log("stderr " + stderr)

            if(stderr){
                textErr = stderr.replace(/\n/g, "<br />");
            }
        })

        javaThread.on("exit", function(state){

            // 如果 java 进程是正常结束的, 那么读取 result.txt 获得分析结果
            if(state === 0){
                fs.readFile("../temp/result.txt", 'utf8', function(err, data){
                    if(err) throw err;

                    data = data.replace(/\n/g, "<br />");

                    result.state   = 2;
                    result.code    = 200;
                    result.message = data;
                    result.error   = textErr

                    // 更新数据库
                    db.collection("apkDetail").update({ apkMD5 : apkMD5 }, result, function(err){
                        if(err) throw err;
                    })
                })
            }

        })
    })

}

module.exports = router;
