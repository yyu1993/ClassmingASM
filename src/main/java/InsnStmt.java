import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Comparator;

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
        StringBuilder sb = new StringBuilder();
        sb.append(methodName+"-");
        sb.append(insnIdx+"-");
        sb.append(insnName+"-");
        sb.append(insnContent);
        return sb.toString();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("    ");
        sb.append(insnIdx+" - "+insnName+": ");
        sb.append(insnContent);
        if(isDefUse){
            sb.append(" valDefUse");
        }
        return sb.toString();
    }
}
