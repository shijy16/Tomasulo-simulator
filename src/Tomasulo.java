import java.io.*;
import java.util.*;

public class Tomasulo {
    // op
    public static final int OP_ADD = 1;
    public static final int OP_SUB = 2;
    public static final int OP_MUL = 3;
    public static final int OP_DIV = 4;
    public static final int OP_LD = 5;
    public static final int OP_JUMP = 6;

    // reserve station type
    public static final int RS_LS = 20; // load and store
    public static final int RS_MUL = 21; // mul and div
    public static final int RS_ADD = 22; // add and sub

    // excute time
    public static int T_ADD = 3;
    public static int T_SUB = 3;
    public static int T_MUL = 12;
    public static int T_DIV = 40;
    public static int T_LD = 3;
    public static int T_JUMP = 1;

    // empty function unit num
    public int empty_adder = 3;
    public int empty_mult = 2;
    public int empty_load = 2;

    // reserve station num
    public int LS_STATION_NUM = 3;
    public int MUL_STATION_NUM = 2;
    public int ADD_STATION_NUM = 3;
    // function unit num
    public static int ADDER = 3;
    public static int MULT = 2;
    public static int LOAD = 2;

    public CalculateStation[] addResverstation;
    public CalculateStation[] mulResverstation;
    public LSStation[] lsResverstation;

    public int cur_T;
    // instruction unit
    Vector<String> instructions = new Vector<String>();
    Vector<InstructionState> instructionStates = new Vector<InstructionState>();
    String insStr;
    String[] insKeyWords = { "ADD", "MUL", "SUB", "DIV", "LD", "JUMP" };

    public Tomasulo() {
        initReserveStation();
        insStr = readFile("test1.nel");
        initInsSet(insStr);
        empty_adder = ADDER;
        empty_mult = MULT;
        empty_load = LOAD;
    }

    public void initReserveStation() {
        addResverstation = new CalculateStation[ADD_STATION_NUM];
        int id = 0;
        for (int i = 0; i < ADD_STATION_NUM; i++) {
            addResverstation[i].init("add" + i, id++);
        }
        mulResverstation = new CalculateStation[MUL_STATION_NUM];
        for (int i = 0; i < MUL_STATION_NUM; i++) {
            addResverstation[i].init("mul" + i, id++);
        }
        lsResverstation = new LSStation[LS_STATION_NUM];
        for (int i = 0; i < LS_STATION_NUM; i++) {
            addResverstation[i].init("ls" + i, id++);
        }
    }

    public String readFile(String fileDir) {
        File f = new File(fileDir);
        BufferedReader reader = null;
        String content = "";
        try {
            reader = new BufferedReader(new FileReader(f));
            String tempString = null;
            while ((tempString = reader.readLine()) != null) {
                content += "\n" + tempString;
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e1) {
                }
            }
        }
        return content;
    }

    public void initInsSet(String insStr) {
        String[] t = insStr.split("\n");
        for (int i = 0; i < t.length; i++) {
            for (int j = 0; j < insKeyWords.length; j++) {
                if (t[i].contains(insKeyWords[j])) {
                    instructions.add(t[i]);
                    break;
                }
            }
        }
    }

    public static void main(String[] args) {
        new Tomasulo();
    }
}
