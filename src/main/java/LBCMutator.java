import org.objectweb.asm.ClassWriter;

import java.util.*;

public class LBCMutator {
    public ClassWriter cw;
    public Method[] methodList;
    public int methodCount;
    public ArrayList<Integer> hpList;

    public LBCMutator(Hashtable<String, ArrayList<String>> methodDictionary) {

    }

    public Method selectMethod() {
        // sort methods by potential value in descending order
        Arrays.sort(methodList, Collections.reverseOrder());
        // get a random value from 0.0 and 1.0
        Random rand = new Random();
        double randVal = rand.nextDouble();
        // get method based on the random value
        int k = (int)(methodCount * Math.log(1-randVal));
        return methodList[k];
    }

    public int selectHookingPoint() {
        return 0;
    }

    public int selectTargetPoint() {
        return 0;
    }
}
