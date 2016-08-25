import java.util.Map;
import java.util.Set;
import java.util.Stack;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import soot.Unit;

public class Output {
	public void printit(String a) throws IOException {
		FileOutputStream fs = new FileOutputStream(new File(a));
		PrintStream p = new PrintStream(fs);
		Map<String, Set<String>> testmap = IntentAnalysis.getAllActivity();
		p.println("==== result ====");
		for(Map.Entry<String, Set<String>> entry:testmap.entrySet()) {
			p.println(entry.getKey());
			for (String s : entry.getValue()) {
				p.println("-> " + s);
			}
			p.println();
		}
		p.close();
	}
	public void printother(String a, String b, Stack<Unit> teststack) throws IOException {
		FileOutputStream fs = new FileOutputStream(new File(a), true);
		PrintStream p = new PrintStream(fs);
		p.println("b -> " + b);
		p.println("stack - > " + teststack);
		p.close();
	}
}