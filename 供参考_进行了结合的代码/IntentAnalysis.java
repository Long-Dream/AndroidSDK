import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import soot.Unit;
import soot.Value;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;

public class IntentAnalysis {
	
	private static final String TAG = "---------IntentAnalysis---------";

	private static String invokeActivity = null;
	private static String targetActivity = null;
	
	private static Map<String, Set<String>> activityMap = new HashMap<String, Set<String>>();
	
	public static void activityAnalysis(String invokeStr, Stack<Unit> unitStack){
		if (unitStack.size() == 0) {
			return;
		}
		int beginIndex = invokeStr.indexOf("<") + 1;
		int endIndex = invokeStr.indexOf(":");
		invokeActivity = invokeStr.substring(beginIndex, endIndex);
		if (!activityMap.containsKey(invokeActivity)) {
			Set<String> set = new HashSet<>();
			activityMap.put(invokeActivity, set);
		}
		Unit unit = null;
//		synchronized (invokeStr) {
//			for (Unit unit : unitStack) {
			while(!unitStack.empty()){	
				try {
					unit = unitStack.pop();
				} catch (Exception e) {
					return;
				}
				InvokeExpr invokeExpr = ((InvokeStmt)unit).getInvokeExpr();
				String methodName = invokeExpr.getMethod().getName();
				if (methodName.equals("<init>")) {
					List<Value> args = invokeExpr.getArgs();
					if (args.size() == 1) {
						// TODO
						System.out.println(TAG + args.get(0).toString() + args.get(0).getType());
					}
					if (args.size() == 2) {
						if (args.get(1).getType().toString().equals("java.lang.Class")) {
							targetActivity = args.get(1).toString();
							if (targetActivity.contains("\"")) {
								targetActivity = targetActivity.split("\"")[1].replace('/', '.');
							}
							break;
						}
					}
				}else if (methodName.equals("setClass")) {
					targetActivity = invokeExpr.getArg(1).toString();
					if (targetActivity.contains("\""))
						targetActivity = targetActivity.split("\"")[1].replace('/', '.');
					break;
				}else if (methodName.equals("setClassName")) {
					try {
						targetActivity = invokeExpr.getArg(1).toString();
					} catch (Exception e) {
						return;
					}
					targetActivity = (targetActivity.substring(1, targetActivity.length() - 1)).replace("/", ".");
					break;
				}else if (methodName.equals("setAction")) {
					// TODO
					//String args = invokeExpr.getArg(0).toString();
					/*
					while(targetActivity = locate(args).pop) {
						activityMap.get(invokeActivity).add(targetActivity);
					}
					*/
					
				}else if (invokeExpr.getMethod().getDeclaringClass().getName().equals("android.content.ComponentName")) {
					if (methodName.equals("<init>")) {
						targetActivity = invokeExpr.getArg(1).toString();
						if (targetActivity.contains("\"") && !targetActivity.equals("\"\""))
							targetActivity = targetActivity.split("\"")[1];
						break;
					}
				}
			}
			unitStack.clear();
			activityMap.get(invokeActivity).add(targetActivity);
		}
		
//	}
	
	public static Map<String, Set<String>> getAllActivity(){
		return activityMap;
	}

}
