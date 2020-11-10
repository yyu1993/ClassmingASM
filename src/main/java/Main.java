import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Random;
import org.apache.commons.io.FileUtils;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class Main {
    public static HashSet ACC;
    public static HashSet REJ;
    public static HashSet NONLIVE;

    public static HashSet livecode;

    public static int[] mutators = {Opcodes.GOTO, Opcodes.RETURN, Opcodes.ATHROW, Opcodes.LOOKUPSWITCH, Opcodes.TABLESWITCH, -1};

    public static int getMutator() {
        return mutators[new Random().nextInt(mutators.length)];
    }

    public static Method selectMethod(ArrayList<Method> methodList) {
        // sort methods by potential value in descending order
        Collections.sort(methodList, Collections.reverseOrder());
        // get a random value from 0.0 and 1.0
        Random rand = new Random();
        double randVal = rand.nextDouble();
        // get method based on the random value
        int k = (int)(methodList.size() * Math.log(1-randVal));
        return methodList.get(k);
    }

    public static String selectHP(Method method) {
        String insn = "";
        HashSet<String> valSet = new HashSet<>();

        for(int i=0;i<method.insnList.size();i++) {
            HashSet<String> valSet0 = new HashSet<>();
            for(int j=0;j<i;j++) {
                if(method.insnList.get(j).isDefUse) {
                    valSet0.add(method.insnList.get(j).identifier());
                }
            }
            HashSet<String> valSet1 = new HashSet<>();
            for(int j=i;j<method.insnList.size();j++) {
                if(method.insnList.get(j).isDefUse) {
                    valSet1.add(method.insnList.get(j).identifier());
                }
            }
            valSet0.retainAll(valSet1);
            if(valSet0.size() > valSet.size()) {
                insn = method.insnList.get(i).identifier();
                valSet = valSet0;
            }
        }

        return insn;
    }

    public static String getTP(Method method) {
        while(true) {
            Random rand = new Random();
            String tp = method.insnList.get((int) (method.insnList.size() * rand.nextDouble()) % method.insnList.size()).identifier();

            return tp;

        }
    }

    public static String[] selectTP(Method method, int size) {
        String[] tpList = new String[size];

        for(int i=0; i<size; i++) {
            tpList[i] = getTP(method);
        }

        return tpList;
    }

    public static void removeMutation(Method method) {
        if(method.mutationCount == 1) {
            return;
        }
        String[] labels = method.mutationDictionary.keySet().toArray(new String[method.mutationDictionary.size()]);
        int rand = new Random().nextInt(labels.length);
        method.mutationDictionary.remove(labels[rand]);
    }

    public static HashSet<String> getLivecodeSet(String classFile) {
        HashSet<String> livecodes = new HashSet<>();
        return livecodes;
    }

    public static void main(String[] args) throws IOException {
        // parse the seed class file and get method and label information
        ClassParser cp = new ClassParser();
        InputStream in = Main.class.getResourceAsStream(Config.SEED_FILE);
        cp.parseClass(in);

        System.out.println(cp.methodDictionary.values());

        // get livecode set from seed class
        int mutationCount = 0;
        ArrayList<HashSet<String>> livecodeSetList = new ArrayList<>();

        InputStream in1 = Main.class.getResourceAsStream(Config.SEED_FILE);
        byte[] traced = cp.instrumentClass(in1);
        FileUtils.writeByteArrayToFile(new File(Config.SEED_FILE), traced);
        System.out.println(traced.length);
//        HashSet<String> originalLivecodeSet = getLivecodeSet(Config.SEED_FILE);
//        livecodeSetList.add(originalLivecodeSet);


//
//        // start mutation iterations
//        int iterations = 0;
//        while (iterations < Config.MAX_ITERATIONS) {
//            // STEP 1: select LBC mutator: picks from goto, return, throw, lookupswitch, tableswitch
//            int hi = getMutator();
//            // STEP 2: select method to mutate based on the potential function
//            if(cp.methodList.size() < 1) {
//                break;
//            }
//            Method methodToMutate = selectMethod(cp.methodList);
//            // STEP 3: apply mutation
//            if(hi == -1) {
//                // remove a previous mutation
//                removeMutation(methodToMutate);
//            } else {
//                String label = selectHP(methodToMutate);
//                String[] tp = new String[0];
//                if(hi == Opcodes.GOTO) {
//                    tp = selectTP(methodToMutate, 1);
//                } else if (hi == Opcodes.LOOKUPSWITCH || hi == Opcodes.TABLESWITCH) {
//                    tp = selectTP(methodToMutate, 3);
//                }
//
//            }
//
//
//            //-
//            iterations++;
//        }
//        // select LBC mutator
//        System.out.println(cp.methodList);
    }

}