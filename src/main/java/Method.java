import java.util.ArrayList;
import java.util.Comparator;
import java.util.Hashtable;

public class Method implements Comparable<Method> {
    public String methodName;
    public ArrayList<InsnStmt> insnList;
    public Hashtable<String, ArrayList<MutationStmt>> mutationDictionary;
    public int mutationCount;

    public Method(String name) {
        this.methodName = name;
        this.insnList = new ArrayList<>();
        this.mutationCount = 1;
    }

    public void addInsn(InsnStmt is) {
        this.insnList.add(is);
    }

    public void addMutation(String label, int hi, String[] tp) {
        MutationStmt ms = new MutationStmt(hi, tp, mutationCount);
        if(mutationDictionary.contains(label)) {
            mutationDictionary.get(label).add(ms);
        } else {
            ArrayList<MutationStmt> msList = new ArrayList<>();
            msList.add(ms);
            mutationDictionary.put(label, new ArrayList<>());
        }
        mutationCount++;
    }

    @Override
    public int compareTo(Method b) {
        double pa = (double)this.insnList.size() / (double)this.mutationCount;
        double pb = (double)b.insnList.size() / (double)b.mutationCount;
        if (pa > pb) {
            return 1;
        } else if (pa < pb) {
            return -1;
        } else {
            return 0;
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n"+methodName+": ");
        for (int i=0;i<insnList.size();i++) {
            sb.append("\n"+insnList.get(i));
        }
        sb.append("\n");
        return sb.toString();
    }
}
