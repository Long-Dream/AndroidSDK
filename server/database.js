var mongodb  = require('mongodb');
var server   = new mongodb.Server('localhost', 27017, {auto_reconnect:true});
var db       = new mongodb.Db("APKAnalyse", server, {safe:true});

db.open(function(err, db){
    if(err) throw err;
    console.log('连接数据库成功！');
})

module.exports = db;