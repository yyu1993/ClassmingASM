public class InsnStmt {
    public String insnName;
    public String insnContent;
    public String methodName;
    public int insnIdx;
    public boolean isDefUse;

    public InsnStmt(String name, String content, String method, int idx, boolean idu) {
        insnName = name;
        insnContent = content;
        methodName = method;
        insnIdx = idx;
        isDefUse = idu;
    }
    public String identifier() {
        String id = Config.INSN_ID+methodName+"-"+insnIdx+"-"+insnIdx+"-"+insnContent;
        return id.replace(" ", "_");
    }

    @Override
    public String toString() {
        if(isDefUse){
            return "    "+insnIdx+" - "+insnName+": "+insnContent+" valDefUse";
        }
        return "    "+insnIdx+" - "+insnName+": "+insnContent;
    }
}
