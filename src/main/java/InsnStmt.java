import org.objectweb.asm.Label;

public class InsnStmt {
    public int labelIdx;
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
        labelIdx = 0;
    }

    public InsnStmt(String name, String content, String method, int idx, boolean idu, int lidx) {
        insnName = name;
        insnContent = content;
        methodName = method;
        insnIdx = idx;
        isDefUse = idu;
        labelIdx = lidx;
    }

    public String identifier() {
        String insnType = insnContent.split(" ")[0];
        String id = Config.INSN_ID+methodName+"-"+insnIdx+"-"+insnName;
        return id.replace(" ", "_");
    }

    @Override
    public String toString() {
//        if(isDefUse){
//            return "    "+insnIdx+" - "+insnName+": "+insnContent+" valDefUse";
//        }
//        return "    "+insnIdx+" - "+insnName+": "+insnContent;
        return identifier();
    }
}
