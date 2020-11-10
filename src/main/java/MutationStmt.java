import java.util.ArrayList;
import java.util.HashSet;

public class MutationStmt {
    final public String METHOD;
    final public int HI;
    final public String HP;
    final public HashSet<String> TPS;
    final public int ID;
    final public String CLASSNAME;
    final public int REMOVE;

    public MutationStmt(String method, int hi, String hp, ArrayList<String> tps, int id) {
        METHOD = method;
        HI = hi;
        HP = hp;
        TPS = new HashSet<>(tps);
        ID = id;
        CLASSNAME = Config.MUTANT_CLASS+ID;
        REMOVE = -1;
    }

    public MutationStmt(String method, int hi, String hp, HashSet<String> tps, int id, int remove) {
        METHOD = method;
        HI = hi;
        HP = hp;
        TPS = tps;
        ID = id;
        CLASSNAME = Config.MUTANT_CLASS+ID;
        REMOVE = remove;
    }
}
