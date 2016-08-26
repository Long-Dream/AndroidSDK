public class Api{
    private String signature;
    private int apilevel;
    private boolean kind;
    private String[] parameters;
    private int vulnum;
    private String[] relpack;
    
    public String getSignature(){
        return signature;
    }
    public int getApilevel(){
        return apilevel;
    }
    public boolean getKind(){
        return kind;
    }
    public String[] getParameters(){
        return parameters;
    }
    public String[] getRelpack(){
        return relpack;
    }
    public int getVulnum(){
        return vulnum;
    }
    
    public Api(String initialstring, int initialvulnum){
        String[] initials = initialstring.split("@");
        kind = (initials[0].equals("1"));
        signature = "<" + initials[1] + ">";
        apilevel = Integer.parseInt(initials[2]);
        parameters = new String[0];
        if (!initials[3].equals("?")) {
            parameters = initials[3].split("#");
        }
        relpack = initials[4].split("#");
        vulnum = initialvulnum;
    }
}
