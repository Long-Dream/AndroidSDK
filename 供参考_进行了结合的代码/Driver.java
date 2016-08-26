import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Stack;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.math.BigDecimal; 

import javax.swing.text.html.Option;

import soot.Body;
import soot.BodyTransformer;
import soot.Local;
import soot.PackManager;
import soot.PatchingChain;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Transform;
import soot.Unit;
import soot.jimple.AbstractStmtSwitch;
import soot.jimple.InvokeExpr;
import soot.jimple.Jimple;
import soot.jimple.StringConstant;
import soot.options.Options;

import java.math.BigDecimal; 

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
    
    static String jarpath    = "../newSoot/android-platforms/";
    static String vulpath    = "../newSoot/Vulnerability/";
    static String outpath    = "../temp/";
    static String resultpath = "../result/";

    static List<String> SDKs = new ArrayList<String>();
    static List<String> PER = new ArrayList<String>();
    static String[] TAG = new String[0]; 
    static List<Api> API = new ArrayList<Api>();
    static String[] SDK = new String[0];
    static String[] result= new String[0];
    static int apilevel = 1;
    static List<Integer[]> REL;

    static String fill="===============================================================================";
    
    public static void main(String[] args) {

        // 扔进来的 apkpath --- ZYF 修改
        String apkpath    = args[0];
         
        try {
            // 添加了参数 --- ZYF 修改
            decode(apkpath);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        getPermission();
        
        initialApiLevel();
        initialAPI();

        REL = getRelFromAPI(API);
        initialSDKs();
        
        args = new String[]{"-android-jars",jarpath , "-process-dir", apkpath};
        Options.v().set_src_prec(Options.src_prec_apk);
        Options.v().set_output_format(Options.output_format_jimple);
        Options.v().set_output_dir(outpath);
        
        Options.v().set_allow_phantom_refs(true);
        
        Scene.v().addBasicClass("java.io.PrintStream", SootClass.SIGNATURES);
        Scene.v().addBasicClass("java.lang.System", SootClass.SIGNATURES);

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
                            analysisAPI(invokeExpr,b);                              
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

        soot.Main.main(args);

        /**
         * 邓一凡开始
         */
        
        delNoRel();
        output2file();

        /**
         * 李维开始
         */
        
        Output p = new Output();
        try {
            p.printit(resultpath + "/analysis_order.txt");
        } catch (IOException e) {
            e.printStackTrace();
        }
        
    }
    
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
                result=Arrays.copyOf(result, TAG.length);
                TAG[TAG.length-1]=api.substring(1);
                TAG[TAG.length-1]=toTitle(TAG[TAG.length-1],100);
                TAG[TAG.length-1]=TAG[TAG.length-1]+"\r\n";
                result[TAG.length-1]=TAG[TAG.length-1];
                vulnum=vulnum+1;
            }else{
                API.add(new Api(api,vulnum));
                if(! API.get(API.size()-1).getKind()){
                    if(apilevel<API.get(API.size()-1).getApilevel()){
                        result[API.get(API.size()-1).getVulnum()]=result[API.get(API.size()-1).getVulnum()]+"Lack "+
                                API.get(API.size()-1).getSignature()+"\r\n";
                    }
                }
            }
            try {
                api = apiread.readLine();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
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
    
    private static void initialApiLevel(){
        String levreadpath=outpath+"/AndroidManifest.xml";
        File levfilename = new File(levreadpath);
        InputStreamReader levreader = null;
        int mode=0;
        try {
            levreader = new InputStreamReader(  
                    new FileInputStream(levfilename));
        } catch (FileNotFoundException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        final BufferedReader levread = new BufferedReader(levreader); 
        String sdkversion ="";
        try {
            sdkversion = levread.readLine();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        while (sdkversion != null) {
            if(mode==0){
                if((sdkversion.length()-sdkversion.indexOf("<"))>10)
                    if(sdkversion.substring(sdkversion.indexOf("<")+1, sdkversion.indexOf("<")+9).equals("uses-sdk"))
                        mode=1;
            }
            if(mode==1){
                if(sdkversion.indexOf("android:minSdkVersion")>=0){
                    try{
                           int temp = Integer.parseInt(sdkversion.substring(sdkversion.indexOf("\"")+1, sdkversion.lastIndexOf("\"")));
                           apilevel=temp;
                           break;
                         }catch(Exception e){
                             apilevel=1;
                        }                   
                }
                if(sdkversion.indexOf("/>")>=0){
                    break;
                }
            }
            try {
                sdkversion = levread.readLine();                
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        } 
        try {
            levread.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    // 此处, 将所有 outpath 改为 resultpath
    private static void output2file(){
        try {
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
            
            String anlevpath = resultpath + "/analysis_minapilevel.txt";
            File anlevfilename = new File(anlevpath);
            anlevfilename.createNewFile();
            final BufferedWriter levanalysisout = new BufferedWriter(new FileWriter(anlevfilename));
            
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
            levanalysisout.write(Integer.toString(apilevel));
            analysisout.flush();
            analysisout.close();
            sdkanalysisout.flush();
            sdkanalysisout.close();
            peranalysisout.flush();
            peranalysisout.close();
            levanalysisout.flush();
            levanalysisout.close();
        }catch(Exception e){
            e.printStackTrace();
        }
    }
    
    // 添加了形参 --- ZYF 修改
    private static void decode(String apkpath) throws IOException, InterruptedException  {
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

        // 将使用 bat 改为了直接使用 jar --- ZYF 修改
        String cmd  = "java -jar -Duser.language=en ../newSoot/apktool/apktool.jar d " + apkpath + " -fo " + outpath;
        Runtime run = Runtime.getRuntime();
        Process p = run.exec(cmd);// 启动另一个进程来执行命令  
        BufferedInputStream in = new BufferedInputStream(p.getInputStream());  
        BufferedReader inBr = new BufferedReader(new InputStreamReader(in));  
        String lineStr;  
        while ((lineStr = inBr.readLine()) != null)  
            //获得命令执行后在控制台的输出信息 
            System.out.println(lineStr);// 打印输出信息  
        //检查命令是否执行失败。  
        if (p.waitFor() != 0) {  
            if (p.exitValue() == 1)//p.exitValue()==0表示正常结束，1：非正常结束  
                System.err.println("ÃüÁîÖ´ÐÐÊ§°Ü!");  
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
                if(permission.substring(permission.indexOf("<")+1, permission.indexOf("<")+16).equals("uses-permission")){
                    permission=permission.substring(permission.indexOf("\"")+1,permission.lastIndexOf("\""));
                    PER.add(permission);
                }
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
    
    private static void analysisAPI(InvokeExpr invokeExpr,Body b){
        if(REL.size()!=0){      
            String pack=invokeExpr.getMethod()+"";
            String func=invokeExpr.getMethod().getSignature()+"";
            
            pack=pack.substring(1,pack.indexOf(":"));
            for(int i=0;i<REL.size();i++){              
                for(int j=0;j<API.get(REL.get(i)[0]).getRelpack().length;j++){
                    int relkind=Integer.parseInt(API.get(REL.get(i)[0]).getRelpack()[j].substring(0, 1));
                    switch(relkind){
                    case 1:
                        if(pack==API.get(REL.get(i)[0]).getRelpack()[j]){
                            REL.remove(i);
                            i=i-1;
                        }
                        break;
                    case 2:
                        Api temp =new Api(API.get(REL.get(i)[0]).getRelpack()[j].replace("*", "@").substring(1),API.get(REL.get(i)[0]).getVulnum());
                        if (temp.getSignature()==func){
                            if(temp.getParameters().length!=0){
                                boolean paracheck=true;
                                for(int k=0;k<temp.getParameters().length;k++){
                                    if(!invokeExpr.getArgs().get(k).equals(temp.getParameters()[k]))
                                        paracheck=false;
                                }
                                if(paracheck){
                                    REL.remove(i);
                                    i=i-1;
                                }
                            }
                        }
                        
                    default:
                        break;
                    }
                }
            }
        }
        for(int i=0;i<API.size();i++){
            if((invokeExpr.getMethod().getSignature()).equals(API.get(i).getSignature()) && apilevel<API.get(i).getApilevel()){
                if(API.get(i).getKind()){
                    if(API.get(i).getParameters().length==0){
                        addV(b.getMethod()+"",invokeExpr.getMethod()+"",i);
                    }else{
                        boolean paracheck=true;
                        for(int j =0;j<API.get(i).getParameters().length;j++){
                            if(API.get(i).getParameters()[j].substring(0, 1).equals(";")){
                                if (invokeExpr.getArgs().get(j).equals(API.get(i).getParameters()[j].substring(1,API.get(i).getParameters()[j].length()))){
                                    paracheck=false;
                                }
                            }else{
                                if (! invokeExpr.getArgs().get(j).equals(API.get(i).getParameters()[j].substring(1,API.get(i).getParameters()[j].length()))){
                                    paracheck=false;
                                }
                            }
                            if(paracheck){
                                addV(b.getMethod()+"",invokeExpr.getMethod()+"",i);
                            }
                        }
                    }
                }else{
                    if(API.get(i).getParameters().length==0){
                        delV(i);                        
                    }else{
                        boolean paracheck=true;
                        for(int j =0;j<API.get(i).getParameters().length;j++){
                            if(API.get(i).getParameters()[j].substring(0, 1).equals(";")){
                                if (invokeExpr.getArgs().get(j).equals(API.get(i).getParameters()[j].substring(1,API.get(i).getParameters()[j].length()))){
                                    paracheck=false;
                                }
                            }else{
                                if (! invokeExpr.getArgs().get(j).equals(API.get(i).getParameters()[j].substring(1,API.get(i).getParameters()[j].length()))){
                                    paracheck=false;
                                }
                            }
                            if(paracheck){
                                delV(i);
                            }
                        }
                    }
                }
            }
        }
    }
    
    private static void delNoRel(){
        if(REL.size()!=0){
            for(int i=0;i<REL.size();i++){
                if(! API.get(REL.get(i)[0]).getKind()){
                    delV(REL.get(i)[0]);
                    i=i-1;
                }
            }
        }
    }
    
    private static List<Integer[]> getRelFromAPI(List<Api> Apis){
        List<Integer[]> rels=new ArrayList<Integer[]>();
        for(int i=0;i<Apis.size();i++){
            if(!(Apis.get(i).getRelpack().length==1&&Apis.get(i).getRelpack()[0].equals("?"))){
                for(int j=0;j<Apis.get(i).getRelpack().length;j++){
                    Integer[] temp=new Integer[2];
                    temp[0]=i;
                    temp[1]=j;
                    rels.add(temp);
                }
            }
        }
        return rels;
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
        result[API.get(i).getVulnum()]=result[API.get(i).getVulnum()]+"{\"Class\":\""+pack.substring(1,pack.indexOf(":"))+"\", \"Method\":\""+
    pack.substring(pack.indexOf(":")+2,pack.indexOf(">"))+"\", \"Api\":\""+api+"\"}\r\n";
    }
    
    private static void delV(int i){
        result[API.get(i).getVulnum()]=result[API.get(i).getVulnum()].replaceAll("Lack <"+
                API.get(API.size()-1).getSignature()+">\r\n", "");
        for(int j=0;j<REL.size();j++){
            if(REL.get(j)[0]==i)
                REL.remove(j);
        }
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

}
