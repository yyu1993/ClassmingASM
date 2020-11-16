public class Config {
    public final static String CLASS_EXT = ".class";
    public final static String SEED_CLASS= "Seed";
    public final static String MUTANT_CLASS = SEED_CLASS+"_MUTANT_";
    public final static String SEED_DIR = "seed/";
    public final static String RUN_DIR = "run/";
    public final static String MUTANT_DIR = "mutant/mutant/";
    public final static String ACC_DIR = "mutant/acc/";
    public final static String REJ_DIR = "mutant/rej/";
    public final static String NONLIVE_DIR = "mutant/nonlive/";

    public final static int MAX_ITERATIONS = 10;
    public final static int LOOP_COUNT = 5;
    public final static double PROB_LOW = 0.2;
    public final static double PROB_HIGH = 0.8;
    public final static double BETA = 0.08;
    public final static double EPSILON = 0.05;

    public final static String INSN_ID = "[INSNID]";
}
