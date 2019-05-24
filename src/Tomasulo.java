import java.io.*;
import java.util.*;

public class Tomasulo {
    // op
    static String[] INSKEY = { "ADD", "SUB", "MUL", "DIV", "LD", "JUMP" };
    public static final int OP_ADD = 0;
    public static final int OP_SUB = 1;
    public static final int OP_MUL = 2;
    public static final int OP_DIV = 3;
    public static final int OP_LD = 4;
    public static final int OP_JUMP = 5;

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

    // hardware
    public CalculateStation[] addReserveStation;
    public CalculateStation[] mulReserveStation;
    public LSStation[] lsReserveStation;
    public int[] F = new int[32];
    public int[] memory = new int[4096];
    public static int pc;

    public static int cur_T;
    // instruction unit
    Vector<String> instructions = new Vector<String>();
    // status
    Vector<InstructionState> instructionStates = new Vector<InstructionState>();
    // wait queue
    Queue<Integer> addWQ = new LinkedList<Integer>();
    Queue<Integer> mulWQ = new LinkedList<Integer>();
    Queue<Integer> lsWQ = new LinkedList<Integer>();

    public int[] F_state = new int[32];
    boolean onJump;

    public Tomasulo() {
        // init
        initAll();
        Scanner sc = new Scanner(System.in);
        while (sc.nextLine().contains("n")) {
            step_next();
        }
        sc.close();
    }

    public void initAll() {
        addWQ.clear();
        mulWQ.clear();
        lsWQ.clear();
        initReserveStation();
        String insStr;
        insStr = readFile("test2.nel");
        initRegister();
        initMemory();
        initInsSet(insStr);
        empty_adder = ADDER;
        empty_mult = MULT;
        empty_load = LOAD;
        pc = 0;
        cur_T = -1;
        onJump = false;
    }

    public int getOp(String order) {
        int res = -1;
        for (int i = 0; i < INSKEY.length; i++) {
            if (order.contains(INSKEY[i])) {
                res = i;
                break;
            }
        }
        return res;
    }

    public boolean step_next() {
        // issue
        cur_T++;
        System.out.println("current T: " + String.valueOf(cur_T));
        if (pc > instructions.size()) {
            return false;
        }
        String cur_order = instructions.get(pc);
        int curOp = getOp(cur_order);
        while (curOp == -1) {
            instructions.remove(pc);
            cur_order = instructions.get(pc);
            curOp = getOp(cur_order);
        }

        // can current order send?
        boolean canSend = false;
        if (!onJump) {
            if (curOp == OP_ADD || curOp == OP_SUB || curOp == OP_JUMP) {
                for (int i = 0; i < ADD_STATION_NUM; i++) {
                    if (!addReserveStation[i].busy) {
                        canSend = true;
                        if (curOp == OP_JUMP) {
                            onJump = true;
                        }
                        addReserveStation[i].busy = true;
                        addReserveStation[i].insId = instructionStates.size();
                        // init instruction state
                        InstructionState ins = new InstructionState();
                        ins.init(cur_order, cur_T);
                        instructionStates.add(ins);
                        // set reserveStation
                        addReserveStation[i].op = ins.op;
                        if (F_state[ins.src1] == -1) {
                            addReserveStation[i].vj_ok = true;
                            addReserveStation[i].qj = F[ins.src1];
                        } else {
                            addReserveStation[i].vj_ok = false;
                            addReserveStation[i].vj = F_state[ins.src1];
                        }
                        if (curOp != OP_JUMP)
                            if (F_state[ins.src2] == -1) {
                                addReserveStation[i].vk_ok = true;
                                addReserveStation[i].qk = F[ins.src2];
                            } else {
                                addReserveStation[i].vk_ok = false;
                                addReserveStation[i].vk = F_state[ins.src2];
                            }
                        else {
                            addReserveStation[i].vk_ok = true;
                        }
                        // update dst register state
                        F_state[ins.dst] = addReserveStation[i].id;
                        break;

                    }
                }
            } else if (curOp == OP_MUL || curOp == OP_DIV) {
                for (int i = 0; i < ADD_STATION_NUM; i++) {
                    if (!mulReserveStation[i].busy) {
                        canSend = true;
                        mulReserveStation[i].busy = true;
                        mulReserveStation[i].insId = instructionStates.size();
                        // init instruction state
                        InstructionState ins = new InstructionState();
                        ins.init(cur_order, cur_T);
                        instructionStates.add(ins);
                        // set reserveStation
                        mulReserveStation[i].op = ins.op;
                        if (F_state[ins.src1] == -1) {
                            mulReserveStation[i].vj_ok = true;
                            mulReserveStation[i].qj = F[ins.src1];
                        } else {
                            mulReserveStation[i].vj_ok = false;
                            mulReserveStation[i].vj = F_state[ins.src1];
                        }
                        if (F_state[ins.src2] == -1) {
                            mulReserveStation[i].vk_ok = true;
                            mulReserveStation[i].qk = F[ins.src2];
                        } else {
                            mulReserveStation[i].vk_ok = false;
                            mulReserveStation[i].vk = F_state[ins.src2];
                        }
                        // update dst register state
                        F_state[ins.dst] = addReserveStation[i].id;
                        break;
                    }
                }
            } else if (curOp == OP_LD) {
                for (int i = 0; i < LS_STATION_NUM; i++) {
                    if (!lsReserveStation[i].busy) {
                        canSend = true;
                        lsReserveStation[i].busy = true;
                        lsReserveStation[i].insId = instructionStates.size();
                        // init instruction state
                        InstructionState ins = new InstructionState();
                        ins.init(cur_order, cur_T);
                        instructionStates.add(ins);
                        // set reserveStation
                        lsReserveStation[i].addr = ins.src1;
                        F_state[ins.dst] = lsReserveStation[i].id;
                        break;
                    }
                }
            }
        }
        // modify pc
        if (canSend)
            pc++;

        /*---------------------------------*/
        /*----------- excute --------------*/
        /*---------------------------------*/
        // for add
        for (int i = 0; i < ADD_STATION_NUM; i++) {
            // not empty
            if (!addReserveStation[i].busy)
                continue;
            // not the one just issued
            if (instructionStates.get(addReserveStation[i].insId).issue == cur_T)
                continue;
            // ready but not excuting
            if (instructionStates.get(addReserveStation[i].insId).state == InstructionState.ISSUE) {
                if (addReserveStation[i].vj_ok && addReserveStation[i].vk_ok) {
                    instructionStates.get(addReserveStation[i].insId).state = InstructionState.READY;
                    instructionStates.get(addReserveStation[i].insId).ready_turn = cur_T;
                    addWQ.offer(i);
                }
            } else if (instructionStates.get(addReserveStation[i].insId).state == InstructionState.EXECUTE) {
                instructionStates.get(addReserveStation[i].insId).exec_timer -= 1;
                if (instructionStates.get(addReserveStation[i].insId).exec_timer == 0) {
                    instructionStates.get(addReserveStation[i].insId).state = InstructionState.WB;
                    instructionStates.get(addReserveStation[i].insId).exec_comp = cur_T;
                }
            }
        }
        while (empty_adder > 0 && addWQ.size() > 0) {
            empty_adder--;
            int i = addWQ.poll();
            instructionStates.get(addReserveStation[i].insId).state = InstructionState.EXECUTE;
        }

        // for mul
        for (int i = 0; i < MUL_STATION_NUM; i++) {
            // not empty
            if (!mulReserveStation[i].busy)
                continue;
            // not the one just issued
            if (instructionStates.get(mulReserveStation[i].insId).issue == cur_T)
                continue;
            // ready but not excuting
            if (instructionStates.get(mulReserveStation[i].insId).state == InstructionState.ISSUE) {
                if (mulReserveStation[i].vj_ok && mulReserveStation[i].vk_ok) {
                    instructionStates.get(mulReserveStation[i].insId).state = InstructionState.READY;
                    instructionStates.get(mulReserveStation[i].insId).ready_turn = cur_T;
                    mulWQ.offer(i);
                }
            } else if (instructionStates.get(mulReserveStation[i].insId).state == InstructionState.EXECUTE) {
                instructionStates.get(mulReserveStation[i].insId).exec_timer -= 1;
                if (instructionStates.get(mulReserveStation[i].insId).exec_timer == 0) {
                    instructionStates.get(mulReserveStation[i].insId).state = InstructionState.WB;
                    instructionStates.get(mulReserveStation[i].insId).exec_comp = cur_T;
                }
            }
        }
        while (empty_mult > 0 && mulWQ.size() > 0) {
            empty_mult--;
            int i = mulWQ.poll();
            instructionStates.get(mulReserveStation[i].insId).state = InstructionState.EXECUTE;
        }

        // for L/S
        for (int i = 0; i < LS_STATION_NUM; i++) {
            // not empty
            if (!lsReserveStation[i].busy)
                continue;
            // not the one just issued
            if (instructionStates.get(lsReserveStation[i].insId).issue == cur_T)
                continue;
            // ready but not excuting
            if (instructionStates.get(lsReserveStation[i].insId).state == InstructionState.ISSUE) {
                instructionStates.get(lsReserveStation[i].insId).state = InstructionState.READY;
                instructionStates.get(lsReserveStation[i].insId).ready_turn = cur_T;
                lsWQ.offer(i);
            } else if (instructionStates.get(lsReserveStation[i].insId).state == InstructionState.EXECUTE) {
                instructionStates.get(lsReserveStation[i].insId).exec_timer -= 1;
                if (instructionStates.get(lsReserveStation[i].insId).exec_timer == 0) {
                    instructionStates.get(lsReserveStation[i].insId).state = InstructionState.WB;
                    instructionStates.get(lsReserveStation[i].insId).exec_comp = cur_T;
                }
            }
        }
        while (empty_load > 0 && lsWQ.size() > 0) {
            empty_load--;
            int i = lsWQ.poll();
            instructionStates.get(lsReserveStation[i].insId).state = InstructionState.EXECUTE;
        }
        printStatus();
        return true;
    }

    public void printStatus() {
        for (int i = 0; i < instructionStates.size(); i++) {
            instructionStates.get(i).printStatus();
        }
    }

    public void initRegister() {
        for (int i = 0; i < F.length; i++) {
            F[i] = 0;
            F_state[i] = -1;
        }
    }

    public void initMemory() {
        for (int i = 0; i < memory.length; i++) {
            memory[i] = 0;
        }
    }

    public void initReserveStation() {
        addReserveStation = new CalculateStation[ADD_STATION_NUM];
        int id = 0;
        for (int i = 0; i < ADD_STATION_NUM; i++) {
            addReserveStation[i] = new CalculateStation("add" + i, id++);
        }
        mulReserveStation = new CalculateStation[MUL_STATION_NUM];
        for (int i = 0; i < MUL_STATION_NUM; i++) {
            mulReserveStation[i] = new CalculateStation("mul" + i, id++);
        }
        lsReserveStation = new LSStation[LS_STATION_NUM];
        for (int i = 0; i < LS_STATION_NUM; i++) {
            lsReserveStation[i] = new LSStation("mul" + i, id++);
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
            for (int j = 0; j < INSKEY.length; j++) {
                if (t[i].contains(INSKEY[j])) {
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
