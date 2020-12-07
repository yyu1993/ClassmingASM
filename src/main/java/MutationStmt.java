import org.objectweb.asm.Label;

import java.util.ArrayList;
import java.util.Hashtable;

public class MutationStmt {
    final public String METHOD;
    final public int HI;
    final public String HP;
    final public ArrayList<String> TPS;
    final public int ID;
    final public String CLASSNAME;
    final public int REMOVE;
    int loopVar;
    Hashtable<String, Label> labelDict;

    public MutationStmt(String method, int hi, String hp, ArrayList<String> tps, int id) {
        METHOD = method;
        HI = hi;
        HP = hp;
        TPS = tps;
        ID = id;
        CLASSNAME = Config.MUTANT_CLASS+ID;
        REMOVE = -1;
        labelDict = new Hashtable<>();
    }

    public MutationStmt(String method, int hi, String hp, ArrayList<String> tps, int id, int remove) {
        METHOD = method;
        HI = hi;
        HP = hp;
        TPS = tps;
        ID = id;
        CLASSNAME = Config.MUTANT_CLASS+ID;
        REMOVE = remove;
        labelDict = new Hashtable<>();
    }

    public void addLabel(String tp, Label l) {
        labelDict.put(tp, l);
    }

    public Label getLabel(String tp) {
        return labelDict.get(tp);
    }

    @Override
    public String toString() {
        String tp = "";
        for(String t : TPS) {
            tp = tp + t + " ";
        }
        return "[" + Main.getOpcodesStr(HI) + "]" + " from " + HP + " to " + tp + "in " + METHOD;
    }
}
