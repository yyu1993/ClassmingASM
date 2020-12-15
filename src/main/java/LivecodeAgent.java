import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.instrument.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;

public class LivecodeAgent {
    public static Instrumentation insn;

    public static void loadClass(String[] classData) throws IOException, ClassNotFoundException, UnmodifiableClassException {
        URLClassLoader loader = new URLClassLoader(new URL[] {
                new URL("file://" + classData[1])
        });
        ClassDefinition classDef = new ClassDefinition(loader.loadClass(classData[2]), Files.readAllBytes(Paths.get(classData[3])));
        insn.redefineClasses(classDef);
    }

    public static void listener() {
        try {
            ServerSocket serverSocket = new ServerSocket(Config.SERVER_PORT);
            while(true) {
                Socket clientSocket = serverSocket.accept();

                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

                String inputLine;
                boolean receiving = true;
                while ((inputLine = in.readLine()) != null && receiving) {
                    String msg[] = inputLine.split(" ");
                    String msgType = msg[0];

                    if (msgType == "loadClass") {
                        loadClass(msg);
                    } else {
                        break;
                    }
                }
                clientSocket.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void premain(String agentArgs, Instrumentation inst) {
        insn = inst;

        Thread t1 = new Thread(new Runnable() {
            @Override
            public void run() {
                listener();
            }
        });
        t1.start();

    }
}
