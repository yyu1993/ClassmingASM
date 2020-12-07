import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import org.objectweb.asm.Opcodes;

/**
 * An implementation of classming using the ASM bytecode manipulation library.
 *
 * @author  Yang Yu
 * @version 0.1.1
 */
public class Main {
    public static HashSet<String> ACC = new HashSet<>();
    public static HashSet<String> REJ = new HashSet<>();
    public static HashSet<String> NONLIVE = new HashSet<>();

    // final public static int[] MUTATORS = {Opcodes.GOTO, Opcodes.RETURN, Opcodes.ATHROW, Opcodes.LOOKUPSWITCH, Opcodes.TABLESWITCH, -1};
    final public static int[] MUTATORS = {Opcodes.RETURN, Opcodes.RETURN, Opcodes.GOTO, Opcodes.GOTO, -1};

    public static String getOpcodesStr(int opcode) {
        switch (opcode) {
            case Opcodes.GOTO:
                return "GOTO";
            case Opcodes.RETURN:
                return "RETURN";
            case Opcodes.ATHROW:
                return "THROW";
            case Opcodes.LOOKUPSWITCH:
                return "LOOKUPSWITCH";
            case Opcodes.TABLESWITCH:
                return "TABLESWITCH";
            default:
                return "REMOVE";
        }
    }

    /**
     * This method selects a random mutator from the MUTATORS list.
     *
     * @return  an int corresponding to the Opcode of the mutator or -1 for remove a previous mutation
     */
    public static int getMutator() {
        return MUTATORS[new Random(System.currentTimeMillis()).nextInt(MUTATORS.length)];
    }

    /**
     * This method selects a method to mutate.
     *
     * @param methodList        list of methods in class
     * @param curLivecodeList   current livecode list
     * @return                  a Method reference to selected method
     */
    public static Method selectMethod(ArrayList<Method> methodList, ArrayList<String> curLivecodeList) {
        // get list of live methods
        ArrayList<Method> liveMethodList = new ArrayList<>();
        HashSet<String> liveMethodNames = new HashSet<>();

        for(String insn : curLivecodeList) {
            liveMethodNames.add(insn.split("-")[0]);
        }

        for(Method method : methodList) {
            if(liveMethodNames.contains(Config.INSN_ID+method.methodName)) {
                liveMethodList.add(method);
            }
        }

        if(liveMethodList.size() == 0) {
            return null;
        }

        // sort methods by potential value in descending order
        liveMethodList.sort(Collections.reverseOrder());
        // get a random value from 0.0 and 1.0
        double randVal = new Random(System.currentTimeMillis()).nextDouble();
        // get method based on the random value
        int k = (int)Math.floor(liveMethodList.size() * Math.log(1.0-randVal) / Math.log(Config.EPSILON)) % liveMethodList.size();

        return liveMethodList.get(k);
    }

    /**
     * This method selects a hooking point.
     *
     * @param method            method to be mutated
     * @param curLivecodeList   current livecode list
     * @return                  a String representing the hooking point insn
     */
    public static String selectHP(Method method, ArrayList<String> curLivecodeList) {
        ArrayList<InsnStmt> methodLivecodeList = new ArrayList<>();
        Hashtable<String, InsnStmt> methodInsnTable = new Hashtable<>();
        for(InsnStmt is : method.insnList) {
            methodInsnTable.put(is.identifier(), is);
        }
        for(String insn : curLivecodeList) {
            if(methodInsnTable.containsKey(insn)) {
                methodLivecodeList.add(methodInsnTable.get(insn));
            }
        }

        int idx1 = new Random(System.currentTimeMillis()).nextInt(methodLivecodeList.size());
        int idx2 = new Random(System.currentTimeMillis()).nextInt(methodLivecodeList.size());

        HashSet<String> valSet1 = new HashSet<>();
        for(int j=0;j<idx1;j++) {
            if(methodLivecodeList.get(j).isDefUse) {
                valSet1.add(methodLivecodeList.get(j).identifier());
            }
        }
        HashSet<String> valSet12 = new HashSet<>();
        for(int j=idx1;j<methodLivecodeList.size();j++) {
            if(methodLivecodeList.get(j).isDefUse) {
                valSet1.add(methodLivecodeList.get(j).identifier());
            }
        }
        valSet1.retainAll(valSet12);

        HashSet<String> valSet2 = new HashSet<>();
        for(int j=0;j<idx2;j++) {
            if(methodLivecodeList.get(j).isDefUse) {
                valSet1.add(methodLivecodeList.get(j).identifier());
            }
        }
        HashSet<String> valSet22 = new HashSet<>();
        for(int j=idx2;j<methodLivecodeList.size();j++) {
            if(methodLivecodeList.get(j).isDefUse) {
                valSet1.add(methodLivecodeList.get(j).identifier());
            }
        }
        valSet2.retainAll(valSet22);

        return valSet1.size() >= valSet2.size() ? methodLivecodeList.get(idx1).identifier() : methodLivecodeList.get(idx2).identifier();
    }

    /**
     * This method selects a target point.
     *
     * @param method            the method to choose instruction
     * @param totalLivecodeSet  set of all livecodes
     * @param curLivecodeSet    set of most recent livecodes
     * @return                  a String representing the target point insn
     */
    public static String getTP(Method method, HashSet<String> totalLivecodeSet, HashSet<String> curLivecodeSet) {
        while(true) {
            // get a random insn
            Random rand = new Random(System.currentTimeMillis());
            String tp = method.insnList.get((int) (method.insnList.size() * rand.nextDouble()) % method.insnList.size()).identifier();

            // if the insn was never reached before, use it as the target point
            if(!totalLivecodeSet.contains(tp)) {
                return tp;
            }

            // get a random value from 0.0 and 1.0
            double randVal = rand.nextDouble();

            // if the insn was not reached in the current mutation, and random value is smaller than PROB_HIGH, use it as the target point
            if(!curLivecodeSet.contains(tp) && randVal < Config.PROB_HIGH) {
                return tp;
            }

            // if the insn was reached in the current mutation, and random value is smaller than PROB_LOW, use it as the target point
            if(curLivecodeSet.contains(tp) && randVal < Config.PROB_LOW) {
                return tp;
            }
        }
    }

    /**
     * This method is a wrapper method for getTP. It selects number of TPs according to size
     *
     * @param method            method to select tp from
     * @param size              number of tps to select
     * @param totalLivecodeSet  set of all livecodes
     * @param curLivecodeSet    set of most recent livecodes
     * @return                  an ArrayList<String> containing selected target points
     */
    public static ArrayList<String> selectTP(Method method, int size, HashSet<String> totalLivecodeSet, HashSet<String> curLivecodeSet) {
        ArrayList<String> tpList = new ArrayList<>();

        for(int i=0; i<size; i++) {
            tpList.add(getTP(method, totalLivecodeSet, curLivecodeSet));
        }

        return tpList;
    }

    /**
     * This method selects a random mutation from the method given.
     *
     * @param method    method to remove mutation from
     */
    public static MutationStmt getRandomMutation(Method method) {
        if(method.mutationCount == 1) {
            return null;
        }
        String[] insns = method.mutationDictionary.keySet().toArray(new String[0]);
        Random rand = new Random(System.currentTimeMillis());
        int randInt1 = rand.nextInt(insns.length);
        if(method.mutationDictionary.get(insns[randInt1]).size() == 1) {
            return method.mutationDictionary.get(insns[randInt1]).get(0);
        } else {
            int randInt2 = rand.nextInt(method.mutationDictionary.get(insns[randInt1]).size());
            return method.mutationDictionary.get(insns[randInt1]).get(randInt2);
        }
    }

    public static
    void main(String[] args) throws IOException                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();
        System.out.println(String.format("[%s %d] === Fuzzing operation started ===", dtf.format(LocalDateTime.now()), System.currentTimeMillis()));

        // parse the seed class file and get method and label information
        ClassParser cp = new ClassParser();
        InputStream in = new FileInputStream(Config.SEED_DIR+Config.SEED_CLASS+Config.CLASS_EXT);
        cp.parseClass(in);
        System.out.println(cp.methodDictionary.values());
        in.close();

        // get livecode list from seed class
        ArrayList<String> seedLivecode = cp.getLivecode(Config.SEED_DIR, Config.SEED_CLASS);
        cp.totalLivecodeSet.addAll(seedLivecode);
        cp.curLivecodeList = seedLivecode;

        int iter = 1;
        while (iter <= Config.MAX_ITERATIONS) {
            try {
                System.out.println(String.format("[%s %d] === Starting iteration #%d ===", dtf.format(LocalDateTime.now()), System.currentTimeMillis(), iter));

                // STEP 1: select LBC mutator: picks from goto, return, throw, lookupswitch, tableswitch
                int hi = getMutator();
                System.out.println(String.format("[%s %d] HI generated: %s", dtf.format(LocalDateTime.now()), System.currentTimeMillis(), getOpcodesStr(hi)));

                // STEP 2: select method to mutate based on the potential function
                if (cp.methodDictionary.size() < 1) {
                    System.out.println(String.format("[%s %d] No method found, exit loop.", dtf.format(LocalDateTime.now()), System.currentTimeMillis()));
                    break;
                }
                Method methodToMutate = selectMethod(new ArrayList<>(cp.methodDictionary.values()), cp.curLivecodeList);
                if (methodToMutate == null) {
                    System.out.println(String.format("[%s %d] Method selected: %s", dtf.format(LocalDateTime.now()), System.currentTimeMillis(), "No live method found"));
                    break;
                }
                System.out.println(String.format("[%s %d] Method selected: %s", dtf.format(LocalDateTime.now()), System.currentTimeMillis(), methodToMutate.methodName));

                // STEP 3: get mutations
                MutationStmt ms;
                if (hi == -1) {
                    // remove a previous mutation
                    MutationStmt msToRemove = getRandomMutation(methodToMutate);
                    if (msToRemove == null) {
                        continue;
                    }
                    ms = new MutationStmt(msToRemove.METHOD, msToRemove.HI, msToRemove.HP, msToRemove.TPS, iter, msToRemove.ID);
                } else {
                    String hp = selectHP(methodToMutate, cp.curLivecodeList);
                    System.out.println(String.format("[%s %d] HP selected: %s", dtf.format(LocalDateTime.now()), System.currentTimeMillis(), hp));
                    ArrayList<String> tps = new ArrayList<>();
                    if (hi == Opcodes.GOTO) {
                        tps = selectTP(methodToMutate, 1, cp.totalLivecodeSet, new HashSet<>(cp.curLivecodeList));
                    } else if (hi == Opcodes.LOOKUPSWITCH || hi == Opcodes.TABLESWITCH) {
                        tps = selectTP(methodToMutate, 3, cp.totalLivecodeSet, new HashSet<>(cp.curLivecodeList));
                    }
                    System.out.println(String.format("[%s %d] TPs selected: %s", dtf.format(LocalDateTime.now()), System.currentTimeMillis(), tps));

                    // create new mutation
                    ms = new MutationStmt(methodToMutate.methodName, hi, hp, tps, iter);
                }
                System.out.println(String.format("[%s %d] Mutation generated: %s", dtf.format(LocalDateTime.now()), System.currentTimeMillis(), ms));

                // STEP 4: generate new mutant
                cp.generateMutant(ms);
                System.out.println(String.format("[%s %d] Mutant file generated: %s", dtf.format(LocalDateTime.now()), System.currentTimeMillis(), ms.CLASSNAME));

                // STEP 5: select new mutant
                String res = cp.selectMutant(ms);
                System.out.println(String.format("[%s %d] Mutant selected: %s", dtf.format(LocalDateTime.now()), System.currentTimeMillis(), res));
                if (res == Config.ACC) {
                    ACC.add(ms.CLASSNAME);
                } else if (res == Config.REJ) {
                    REJ.add(ms.CLASSNAME);
                } else if (res == Config.NONLIVE) {
                    NONLIVE.add(ms.CLASSNAME);
                }
            } catch(Exception e) {
                e.printStackTrace();
            }
            // end of iteration
            iter++;
        }

        System.out.println(String.format("[%s %d] === Fuzzing operation finished ===", dtf.format(LocalDateTime.now()), System.currentTimeMillis()));

        // write result to file
//        File resultFile = new File(Config.SEED_CLASS+".result");
//        resultFile.createNewFile();
        FileWriter resultWriter = new FileWriter(Config.SEED_CLASS+".result");
        for(String m : ACC) {
            resultWriter.write(String.format("%s, ACC\n", m));
        }
        for(String m : REJ) {
            resultWriter.write(String.format("%s, REJ\n", m));
        }
        for(String m : NONLIVE) {
            resultWriter.write(String.format("%s, NONLIVE\n", m));
        }
        resultWriter.close();
    }

}