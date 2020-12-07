import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;

public class Method implements Comparable<Method> {
    public String methodName;
    public ArrayList<InsnStmt> insnList;
    public Hashtable<String, ArrayList<MutationStmt>> mutationDictionary; // hp -> mutationStmt
    public int mutationCount;
    public ArrayList<MutationStmt> mutationList;
    public HashSet<String> tpSet;
    public int variableCount;

    public Method(String name) {
        methodName = name;
        insnList = new ArrayList<>();
        mutationCount = 1;
        tpSet = new HashSet<>();
        variableCount = 0;
        mutationList = new ArrayList<>();
        mutationDictionary = new Hashtable<>();
    }

    public void addInsn(InsnStmt is) {
        this.insnList.add(is);
    }

    public void addMutation(MutationStmt ms) {
        if(ms.REMOVE >= 0) {
            for(String hp : mutationDictionary.keySet()) {
                for(MutationStmt m : mutationDictionary.get(hp)) {
                    if(m.ID == ms.REMOVE) {
                        mutationDictionary.get(hp).remove(m);
                        if(mutationDictionary.get(hp).size() == 0) {
                            mutationDictionary.remove(hp);
                        }
                        mutationCount--;
                        mutationList.remove(ms);
                        return;
                    }
                }
            }
        } else {
            if (mutationDictionary.containsKey(ms.HP)) {
                mutationDictionary.get(ms.HP).add(ms);
            } else {
                ArrayList<MutationStmt> msList = new ArrayList<>();
                msList.add(ms);
                mutationDictionary.put(ms.HP, msList);
            }
            tpSet.addAll(ms.TPS);
            mutationList.add(ms);
            mutationCount++;
        }
    }

    @Override
    public int compareTo(Method b) {
        Double pa = (double)this.insnList.size() / (double)this.mutationCount;
        Double pb = (double)b.insnList.size() / (double)b.mutationCount;
        return pa.compareTo(pb);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        sb.append(methodName);
        sb.append(": ");
        for (InsnStmt insn : insnList) {
            sb.append("\n");
            sb.append(insn.toString());
        }
        sb.append("\n");
        return sb.toString();
    }
}
