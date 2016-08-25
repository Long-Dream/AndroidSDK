import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Stack;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.math.BigDecimal; 

import soot.Body;
import soot.BodyTransformer;
import soot.Local;
import soot.PackManager;
import soot.PatchingChain;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.Transform;
import soot.Unit;
import soot.jimple.AbstractStmtSwitch;
import soot.jimple.InvokeExpr;
import soot.jimple.Jimple;
import soot.options.Options;

import java.io.File; 
import java.io.FileWriter;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.BufferedInputStream;
import java.io.BufferedReader;  
import java.io.BufferedWriter;  
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;  

public class Driver {
    
    /*
     * 整理 : Main 外全部是邓一凡的代码
     */
    
    //  static String sourcepath = "C:/Users/zsy/Desktop/sxs/workspace/SootDemo";
	//  static String apkpath    = "./apkTemp/" + args[0]; 被我扔进 main 里面了
	
    static String jarpath    = "../newSoot/android-platforms";
    static String vulpath    = "../newSoot/Vulnerability/";
    static String outpath    = "../temp/";
    static String resultpath = "../result/";

    static List<String> SDKs = new ArrayList<String>();
    static List<String> PER = new ArrayList<String>();
    static String[] TAG = new String[0]; 
    static String[] API = new String[0];
    static String[] SDK = new String[0];
    static String[] result;
    static int[] index = new int[0];
    static String fill="===============================================================================";
    
    private static void initialAPI(){
        String apireadpath=vulpath+"Api.txt";
        File apifilename = new File(apireadpath);
        InputStreamReader apireader = null;
        try {
            apireader = new InputStreamReader(  
                    new FileInputStream(apifilename));
        } catch (FileNotFoundException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        final BufferedReader apiread = new BufferedReader(apireader); 
        String api ="";  
        int vulnum=-1;
        try {
            api = apiread.readLine();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        while (api != null) {  
            if(api.substring(0, 1).equals("#")){
                TAG=Arrays.copyOf(TAG, TAG.length+1);
                TAG[TAG.length-1]=api.substring(1);
                TAG[TAG.length-1]=toTitle(TAG[TAG.length-1],100);
                vulnum=vulnum+1;
            }else{
                API=Arrays.copyOf(API, API.length+1);
                API[API.length-1]="<"+api+">";
                index=Arrays.copyOf(index, index.length+1);
                index[index.length-1]=vulnum;
            }
            try {
                api = apiread.readLine();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }  
        result = new String[TAG.length];
        try {
            apiread.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    private static void initialSDKs(){
        String sdkreadpath=vulpath+"AdLibraries.txt";
        File sdkfilename = new File(sdkreadpath);
        InputStreamReader sdkreader = null;
        try {
            sdkreader = new InputStreamReader(  
                    new FileInputStream(sdkfilename));
        } catch (FileNotFoundException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        final BufferedReader sdkread = new BufferedReader(sdkreader); 
        String sdks ="";
        try {
            sdks = sdkread.readLine();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        while (sdks != null) {  
            SDKs.add(sdks);
            try {
                sdks = sdkread.readLine();              
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        } 
        try {
            sdkread.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    // 添加了形参 --- ZYF 修改
    private static void decode(String apkpath) throws Exception {
        /**File inFile = new File(apkpath);  
        ApkDecoder decoder = new ApkDecoder();
        delAllFile(outpath); 
        new File(outpath).delete();
        decoder.setOutDir(new File(outpath));  
        decoder.setApkFile(inFile);  
        decoder.decode();  */
        File outdir = new File(outpath); 
        if(!outdir.exists()){
            outdir.mkdirs();
        }
        String cmd  = "java -jar -Duser.language=en ../newSoot/apktool/apktool.jar d " + apkpath + " -fo " + outpath;
        Runtime run = Runtime.getRuntime();
        Process p   = run.exec(cmd);// 启动另一个进程来执行命令  
        BufferedInputStream in = new BufferedInputStream(p.getInputStream());  
        BufferedReader inBr = new BufferedReader(new InputStreamReader(in));  
        String lineStr;  
        while ((lineStr = inBr.readLine()) != null)  
            //获得命令执行后在控制台的输出信息  
            System.out.println(lineStr);// 打印输出信息  
        //检查命令是否执行失败。  
        if (p.waitFor() != 0) {  
            if (p.exitValue() == 1)//p.exitValue()==0表示正常结束，1：非正常结束  
                System.err.println("命令执行失败!");  
        }  
        inBr.close();  
        in.close();  
    }

    private static void getPermission(){
        String perreadpath=outpath+"/AndroidManifest.xml";
        File perfilename = new File(perreadpath);
        InputStreamReader perreader = null;
        try {
            perreader = new InputStreamReader(  
                    new FileInputStream(perfilename));
        } catch (FileNotFoundException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        final BufferedReader perread = new BufferedReader(perreader); 
        String permission ="";
        try {
            permission = perread.readLine();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        while (permission != null) {  
            if((permission.indexOf(">")-permission.indexOf("<"))>20)
                if(permission.substring(permission.indexOf("<")+1, permission.indexOf("<")+16).equals("uses-permission"))
                    PER.add(permission);
            try {
                permission = perread.readLine();                
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        } 
        try {
            perread.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    public static boolean delAllFile(String path) {
           boolean flag = false;
           File file = new File(path);
           if (!file.exists()) {
             return flag;
           }
           if (!file.isDirectory()) {
             return flag;
           }
           String[] tempList = file.list();
           File temp = null;
           for (int i = 0; i < tempList.length; i++) {
              if (path.endsWith(File.separator)) {
                 temp = new File(path + tempList[i]);
              } else {
                  temp = new File(path + File.separator + tempList[i]);
              }
              if (temp.isFile()) {
                 temp.delete();
              }
              if (temp.isDirectory()) {
                 delAllFile(path + "/" + tempList[i]);//先删除文件夹里面的文件
                 delFolder(path + "/" + tempList[i]);//再删除空文件夹
                 flag = true;
              }
           }
           return flag;
         }
    
    public static void delFolder(String folderPath) {
         try {
            delAllFile(folderPath); //删除完里面所有内容
            String filePath = folderPath;
            filePath = filePath.toString();
            java.io.File myFilePath = new java.io.File(filePath);
            myFilePath.delete(); //删除空文件夹
         } catch (Exception e) {
           e.printStackTrace(); 
         }
    }
    
    private static String[] add2Array(String[] array,String add){
        array=Arrays.copyOf(array, array.length+1);
        array[array.length-1]=add;
        return array;
    }
    
    private static void addV(String pack,String api,int i){
        result[index[i]]=result[index[i]]+"{\"Class\":\""+pack.substring(1,pack.indexOf(":"))+"\", \"Method\":\""+
    pack.substring(pack.indexOf(":")+2,pack.indexOf(">"))+"\", \"Api\":\""+api+"\"}\r\n";
    }
    
    private static List<String> removeRep(String[] Rep){
        List<String> norep = new ArrayList<String>();
        for(String s : Rep){
            if(!norep.contains(s))
                norep.add(s);
        }
        return norep;       
    }
    
    private static String toTitle(String title,int length){
        length=length-2;
        String titled=fill.substring(0, (int)Math.ceil((length-title.length())/2))+" "
                +title+" "+fill.substring(0, (int)Math.floor((length-title.length())/2));
        return titled;
    }
    
    private static Local addTmpRef(Body body)
    {
        Local tmpRef = Jimple.v().newLocal("tmpRef", RefType.v("java.io.PrintStream"));
        body.getLocals().add(tmpRef);
        return tmpRef;
    }
    
    private static Local addTmpString(Body body)
    {
        Local tmpString = Jimple.v().newLocal("tmpString", RefType.v("java.lang.String")); 
        body.getLocals().add(tmpString);
        return tmpString;
    }
    
    public static void main(String[] args) {
    	
    	// 扔进来的 apkpath --- ZYF 修改
    	String apkpath    = args[0];
        
        try { 
        	// 添加了参数 --- ZYF 修改
            decode(apkpath);
            getPermission();
            
            initialAPI();
            initialSDKs();
            
            String anapipath = resultpath + "/analysis_api.txt";
            File anapifilename = new File(anapipath);
            anapifilename.createNewFile();
            final BufferedWriter analysisout = new BufferedWriter(new FileWriter(anapifilename)); 
            
            String ansdkpath = resultpath + "/analysis_sdk.txt";
            File ansdkfilename = new File(ansdkpath);
            ansdkfilename.createNewFile();
            final BufferedWriter sdkanalysisout = new BufferedWriter(new FileWriter(ansdkfilename)); 
            
            String anperpath = resultpath + "/analysis_permission.txt";
            File anperfilename = new File(anperpath);
            anperfilename.createNewFile();
            final BufferedWriter peranalysisout = new BufferedWriter(new FileWriter(anperfilename));
            
            String[] argsSoot = new String[]{"-android-jars",jarpath , "-process-dir", apkpath};

            Options.v().set_src_prec(Options.src_prec_apk);
            Options.v().set_output_format(Options.output_format_jimple);
            Options.v().set_output_dir(outpath);
            
            Options.v().set_allow_phantom_refs(true);
            
            Scene.v().addBasicClass("java.io.PrintStream", SootClass.SIGNATURES);
            Scene.v().addBasicClass("java.lang.System", SootClass.SIGNATURES);
            
            for(int i = 0; i < result.length; i++){ 
                TAG[i] = TAG[i] + "\r\n";
                result[i] = TAG[i];
            }

            // 李维
            final Stack<Unit> teststack = new Stack<Unit>();

            PackManager.v().getPack("jtp").add(new Transform("jtp.myInstrumenter", new BodyTransformer() {
                
                // 李维
                String string;

                @Override
                protected void internalTransform(final Body b, String phaseName,
                        Map<String, String> options) {
                    // TODO Auto-generated method stub
                    final PatchingChain<Unit> units = b.getUnits();

                    /**
                     * 邓一凡开始
                     */
                    String pack=b.getMethod()+"";
                    pack=pack.substring(1,pack.indexOf(":"));
                    pack=pack.substring(0,pack.lastIndexOf("."));
                    
                    if(SDKs.contains(pack))
                        SDK=add2Array(SDK,pack);
                    
                    for ( Iterator<Unit> iter = units.snapshotIterator(); iter.hasNext();){
                        final Unit u = iter.next();
                        u.apply(new AbstractStmtSwitch() {
                            public void caseInvokeStmt(soot.jimple.InvokeStmt stmt) {
                                // code here
                                InvokeExpr invokeExpr = stmt.getInvokeExpr();                           
                                for(int i=0;i<API.length;i++){
                                    if((invokeExpr.getMethod().getSignature()).equals(API[i])){
                                        
                                        addV(b.getMethod()+"",invokeExpr.getMethod()+"",i);
                                        }
                                    }                               
                                    b.validate();   
                            };
                        });
                    }

                    /**
                     * 李维开始
                     */
                    
                    for ( Iterator<Unit> iter = units.snapshotIterator(); iter.hasNext();){
                        final Unit u = iter.next();
                        u.apply(new AbstractStmtSwitch() {
                            public void caseInvokeStmt(soot.jimple.InvokeStmt stmt) {
                                // code here
                                InvokeExpr invokeExpr = stmt.getInvokeExpr();
                                if(invokeExpr.getMethod().getName().equals("setAction")){
                                    teststack.add(u);
                                    b.validate();
                                }
                                if(invokeExpr.getMethod().getName().equals("setClass")){
                                    teststack.add(u);
                                    b.validate();
                                }
                                if(invokeExpr.getMethod().getName().equals("setClassName")){
                                    teststack.add(u);
                                    b.validate();
                                }
                                if(invokeExpr.getMethod().getName().equals("<init>")){
                                    teststack.add(u);
                                    b.validate();
                                }
                                
                                if(invokeExpr.getMethod().getName().equals("startActivity")) {
                                    string = stmt.toString();
                                    b.validate();
                                    IntentAnalysis.activityAnalysis(string, teststack);
                                }
                            };
                        });
                    }
                }
            }));


            soot.Main.main(argsSoot);

            /**
             * 邓一凡开始
             */

            List<String> SDKnoRep=removeRep(SDK);
            String sdktitle=toTitle("SDK Package List",100);
            sdkanalysisout.write(sdktitle+"\r\n");
            for(String usedsdk:SDKnoRep)
                sdkanalysisout.write(" "+usedsdk+"\r\n");
            String pertitle=toTitle("Permission List",100);
            peranalysisout.write(pertitle+"\r\n");
            for(String permission:PER)
                peranalysisout.write(" "+permission+"\r\n");
            for(int i=0;i<result.length;i++){
                if(result[i]!=(TAG[i])){
                    try {
                        analysisout.write(result[i]);
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
            analysisout.flush();
            analysisout.close();
            sdkanalysisout.flush();
            sdkanalysisout.close();
            peranalysisout.flush();
            peranalysisout.close();

            /**
             * 李维开始
             */
            
            Output p = new Output();
            try {
                p.printit(resultpath + "/analysis_order.txt");
            } catch (IOException e) {
                e.printStackTrace();
            }

        } catch (Exception e){
            e.printStackTrace();
        }
        
    }
}
