import java.io.*;
import java.util.*;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.text.DefaultStyledDocument.ElementSpec;
import java.awt.*;
import java.awt.event.*;

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
    boolean notOver = false;

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

    // ui
    TomasuloUi ui;

    public Tomasulo() {
        // init
        ui = new TomasuloUi();
        initAll();
        // Scanner sc = new Scanner(System.in);
        // while (step_next()) {
        // if (cur_T % 10 == 0) {
        // sc.nextLine();
        // }
        // }
        // sc.close();
    }

    public void initAll() {
        notOver = true;
        addWQ.clear();
        mulWQ.clear();
        lsWQ.clear();
        initReserveStation();
        String insStr;
        insStr = readFile("test.nel");
        initRegister();
        initMemory();
        initInsSet(insStr);
        empty_adder = ADDER;
        empty_mult = MULT;
        empty_load = LOAD;
        instructionStates.clear();
        pc = 0;
        cur_T = 0;
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
        if (!notOver)
            return false;
        // issue
        cur_T++;
        System.out.println("current T: " + String.valueOf(cur_T));
        String cur_order = "";
        int curOp = -1;
        if (pc < instructions.size()) {
            cur_order = instructions.get(pc);
            curOp = getOp(cur_order);
            while (curOp == -1) {
                instructions.remove(pc);
                cur_order = instructions.get(pc);
                curOp = getOp(cur_order);
            }
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
                        ins.init(cur_order, cur_T, pc);
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
                            addReserveStation[i].qk = ins.src2;
                        }
                        // update dst register state
                        F_state[ins.dst] = addReserveStation[i].id;
                        break;

                    }
                }
            } else if (curOp == OP_MUL || curOp == OP_DIV) {
                for (int i = 0; i < MUL_STATION_NUM; i++) {
                    if (!mulReserveStation[i].busy) {
                        canSend = true;
                        mulReserveStation[i].busy = true;
                        mulReserveStation[i].insId = instructionStates.size();
                        // init instruction state
                        InstructionState ins = new InstructionState();
                        ins.init(cur_order, cur_T, pc);
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
                        F_state[ins.dst] = mulReserveStation[i].id;
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
                        ins.init(cur_order, cur_T, pc);
                        instructionStates.add(ins);
                        // set reserveStation
                        addReserveStation[i].op = ins.op;
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
            // ready for excute? if ready,enqueue
            if (instructionStates.get(addReserveStation[i].insId).state == InstructionState.ISSUE) {
                if (addReserveStation[i].vj_ok && addReserveStation[i].vk_ok) {
                    instructionStates.get(addReserveStation[i].insId).state = InstructionState.READY;
                    instructionStates.get(addReserveStation[i].insId).ready_turn = cur_T;
                    addWQ.offer(i);
                }
            }
            // if excuting,go on
            else if (instructionStates.get(addReserveStation[i].insId).state == InstructionState.EXECUTE) {
                instructionStates.get(addReserveStation[i].insId).exec_timer -= 1;
                if (instructionStates.get(addReserveStation[i].insId).exec_timer == 0) {
                    instructionStates.get(addReserveStation[i].insId).state = InstructionState.WB;
                    instructionStates.get(addReserveStation[i].insId).exec_comp = cur_T;
                    // get result
                    if (addReserveStation[i].op == OP_ADD) {
                        addReserveStation[i].result = addReserveStation[i].qj + addReserveStation[i].qk;
                    } else if (addReserveStation[i].op == OP_SUB) {
                        addReserveStation[i].result = addReserveStation[i].qj - addReserveStation[i].qk;
                    } else if (addReserveStation[i].op == OP_JUMP) {
                        int cmp = instructionStates.get(addReserveStation[i].insId).dst;
                        int v = addReserveStation[i].qj;
                        if (cmp == v) {
                            pc += instructionStates.get(addReserveStation[i].insId).src2;
                        }
                        onJump = false;
                    }
                }
            }
        }
        // dequque and excute
        while (empty_adder > 0 && addWQ.size() > 0) {
            int i = addWQ.poll();
            // the one who just issued should excute next turn.Others can start this turn
            if (instructionStates.get(addReserveStation[i].insId).issue != cur_T) {
                empty_adder--;
                instructionStates.get(addReserveStation[i].insId).state = InstructionState.EXECUTE;
                instructionStates.get(addReserveStation[i].insId).exec_timer -= 1;
                if (instructionStates.get(addReserveStation[i].insId).exec_timer == 0) {
                    instructionStates.get(addReserveStation[i].insId).state = InstructionState.WB;
                    instructionStates.get(addReserveStation[i].insId).exec_comp = cur_T;
                    // get result
                    if (addReserveStation[i].op == OP_ADD) {
                        addReserveStation[i].result = addReserveStation[i].qj + addReserveStation[i].qk;
                    } else if (addReserveStation[i].op == OP_SUB) {
                        addReserveStation[i].result = addReserveStation[i].qj - addReserveStation[i].qk;
                    } else if (addReserveStation[i].op == OP_JUMP) {
                        int cmp = instructionStates.get(addReserveStation[i].insId).dst;
                        int v = addReserveStation[i].qj;
                        System.out.println("??????????" + cmp + "," + v);
                        if (cmp == v) {
                            pc += instructionStates.get(addReserveStation[i].insId).src2;
                        }
                        onJump = false;
                    }
                }
            } else {
                addWQ.offer(i);
                break;
            }
        }

        // for mul
        for (int i = 0; i < MUL_STATION_NUM; i++) {
            // not empty
            if (!mulReserveStation[i].busy)
                continue;
            // not the one just issued
            // if (instructionStates.get(mulReserveStation[i].insId).issue == cur_T)
            // continue;
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
                    // get result
                    if (mulReserveStation[i].op == OP_MUL) {
                        mulReserveStation[i].result = mulReserveStation[i].qj * mulReserveStation[i].qk;
                    } else {
                        // just a temporary solution
                        if (mulReserveStation[i].qk == 0)
                            mulReserveStation[i].result = mulReserveStation[i].qj;
                        else
                            mulReserveStation[i].result = mulReserveStation[i].qj / mulReserveStation[i].qk;
                    }
                }
            }
        }
        // dequque and excute
        while (empty_mult > 0 && mulWQ.size() > 0) {
            int i = mulWQ.poll();
            // instructionStates.get(mulReserveStation[i].insId).state =
            // InstructionState.EXECUTE;
            // the one who just issued should excute next turn.Others can start this turn
            if (instructionStates.get(mulReserveStation[i].insId).issue != cur_T) {
                empty_mult--;
                instructionStates.get(mulReserveStation[i].insId).state = InstructionState.EXECUTE;
                instructionStates.get(mulReserveStation[i].insId).exec_timer -= 1;
                if (instructionStates.get(mulReserveStation[i].insId).exec_timer == 0) {
                    instructionStates.get(mulReserveStation[i].insId).state = InstructionState.WB;
                    instructionStates.get(mulReserveStation[i].insId).exec_comp = cur_T;
                    // get result
                    if (mulReserveStation[i].op == OP_MUL) {
                        mulReserveStation[i].result = mulReserveStation[i].qj * mulReserveStation[i].qk;
                    } else {
                        // just a temporary solution
                        if (mulReserveStation[i].qk == 0)
                            mulReserveStation[i].result = F[instructionStates.get(mulReserveStation[i].insId).dst];
                        else
                            mulReserveStation[i].result = mulReserveStation[i].qj / mulReserveStation[i].qk;
                    }
                }
            } else {
                mulWQ.offer(i);
                break;
            }
        }

        // for L/S
        for (int i = 0; i < LS_STATION_NUM; i++) {
            // not empty
            if (!lsReserveStation[i].busy)
                continue;
            // // not the one just issued
            // if (instructionStates.get(lsReserveStation[i].insId).issue == cur_T)
            // continue;
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
                    // get result
                    if (lsReserveStation[i].op == OP_LD) {
                        lsReserveStation[i].result = memory[lsReserveStation[i].addr];
                    } else {
                        memory[lsReserveStation[i].addr] = lsReserveStation[i].result;
                    }
                }
            }
        }
        // dequque and excute
        while (empty_load > 0 && lsWQ.size() > 0) {
            int i = lsWQ.poll();
            // instructionStates.get(lsReserveStation[i].insId).state =
            // InstructionState.EXECUTE;
            // the one who just issued should excute next turn.Others can start this turn
            if (instructionStates.get(lsReserveStation[i].insId).issue != cur_T) {
                empty_load--;
                instructionStates.get(lsReserveStation[i].insId).state = InstructionState.EXECUTE;
                instructionStates.get(lsReserveStation[i].insId).exec_timer -= 1;
                if (instructionStates.get(lsReserveStation[i].insId).exec_timer == 0) {
                    instructionStates.get(lsReserveStation[i].insId).state = InstructionState.WB;
                    instructionStates.get(lsReserveStation[i].insId).exec_comp = cur_T;
                    // get result
                    if (lsReserveStation[i].op == OP_LD) {
                        lsReserveStation[i].result = memory[lsReserveStation[i].addr];
                    } else {
                        memory[lsReserveStation[i].addr] = lsReserveStation[i].result;
                    }
                }
            } else {
                lsWQ.offer(i);
                break;
            }
        }

        // write back
        // for add
        for (int i = 0; i < ADD_STATION_NUM; i++) {
            if (!addReserveStation[i].busy)
                continue;
            if (instructionStates.get(addReserveStation[i].insId).state == InstructionState.WB) {
                if (instructionStates.get(addReserveStation[i].insId).exec_comp != cur_T) {
                    addReserveStation[i].busy = false;
                    empty_adder++;
                    broadcast(addReserveStation[i].id, addReserveStation[i].result);
                    instructionStates.get(addReserveStation[i].insId).state = InstructionState.FINISHED;
                    instructionStates.get(addReserveStation[i].insId).wb = cur_T;
                }
            }
        }
        // for mul
        for (int i = 0; i < MUL_STATION_NUM; i++) {
            if (!mulReserveStation[i].busy)
                continue;
            if (instructionStates.get(mulReserveStation[i].insId).state == InstructionState.WB) {
                if (instructionStates.get(mulReserveStation[i].insId).exec_comp != cur_T) {
                    mulReserveStation[i].busy = false;
                    empty_mult++;
                    broadcast(mulReserveStation[i].id, mulReserveStation[i].result);
                    instructionStates.get(mulReserveStation[i].insId).state = InstructionState.FINISHED;
                    instructionStates.get(mulReserveStation[i].insId).wb = cur_T;
                }
            }
        }
        // for load
        for (int i = 0; i < LS_STATION_NUM; i++) {
            if (!lsReserveStation[i].busy)
                continue;
            if (instructionStates.get(lsReserveStation[i].insId).state == InstructionState.WB) {
                if (instructionStates.get(lsReserveStation[i].insId).exec_comp != cur_T) {
                    lsReserveStation[i].busy = false;
                    empty_load++;
                    broadcast(lsReserveStation[i].id, lsReserveStation[i].result);
                    instructionStates.get(lsReserveStation[i].insId).state = InstructionState.FINISHED;
                    instructionStates.get(lsReserveStation[i].insId).wb = cur_T;
                }
            }
        }

        printStatus();
        // check if all the instructions have finished
        System.out.println(pc);
        if (pc >= instructions.size()) {
            notOver = false;
            for (int i = 0; i < instructionStates.size(); i++) {
                if (instructionStates.get(i).state != InstructionState.FINISHED) {
                    notOver = true;
                    break;
                }
            }
        } else {
            notOver = true;
        }
        return notOver;
    }

    public void broadcast(int rsId, int res) {
        System.out.println("braodcast " + rsId + "," + res);
        for (int i = 0; i < ADD_STATION_NUM; i++) {
            if (!addReserveStation[i].busy)
                continue;
            if (!addReserveStation[i].vj_ok) {
                if (addReserveStation[i].vj == rsId) {
                    addReserveStation[i].vj_ok = true;
                    addReserveStation[i].qj = res;
                }
            }
            if (!addReserveStation[i].vk_ok) {
                if (addReserveStation[i].vk == rsId) {
                    addReserveStation[i].vk_ok = true;
                    addReserveStation[i].qk = res;
                }
            }
        }

        for (int i = 0; i < MUL_STATION_NUM; i++) {
            if (!mulReserveStation[i].busy)
                continue;
            if (!mulReserveStation[i].vj_ok) {
                if (mulReserveStation[i].vj == rsId) {
                    mulReserveStation[i].vj_ok = true;
                    mulReserveStation[i].qj = res;
                }
            }
            if (!mulReserveStation[i].vk_ok) {
                if (mulReserveStation[i].vk == rsId) {
                    mulReserveStation[i].vk_ok = true;
                    mulReserveStation[i].qk = res;
                }
            }
        }

        for (int i = 0; i < 32; i++) {
            if (F_state[i] == rsId) {
                F[i] = res;
                F_state[i] = -1;
            }
        }
    }

    public void printStatus() {
        for (int i = 0; i < instructionStates.size(); i++) {
            instructionStates.get(i).printStatus();
        }
        // register status
        String tmp = "";
        String tmp1 = "";
        String tmp2 = "";
        System.out.println("register status");
        for (int i = 0; i < 32; i++) {
            tmp += "F" + i + "\t";
            tmp1 += F[i] + "\t";
            tmp2 += F_state[i] + "\t";
        }
        System.out.println(tmp + "\n" + tmp1 + "\n" + tmp2);
    }

    public void initRegister() {
        for (int i = 0; i < F.length; i++) {
            F[i] = 0;
            F_state[i] = -1;
        }
    }

    public void initMemory() {
        for (int i = 0; i < memory.length; i++) {
            memory[i] = i;
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
        instructions.clear();
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

    public class TomasuloUi {
        JFrame jf;

        JTable instructionTable;
        String[] instructionTableColumnName = { "order", "instruction", "ISSUE", "EXEC_COPM", "WB", "Status" };
        String[][] instructionTableData;

        JLabel currentTurnLabel;

        JButton stepButton;
        JTextField stepText;
        JButton goButton;
        JButton stopButton;
        JButton quitButton;
        JButton restartButton;

        boolean notPause = true;

        public TomasuloUi() {
            jf = new JFrame("Tomasulo");

            initUi();
        }

        public void updateUi() {
            updateInstructionTable();
            jf.validate();
        }

        public void updateInstructionTable() {
            int insSize = instructionStates.size();
            int start = 0;
            if (insSize > 10) {
                start = insSize - 10;
            }
            for (int i = start; i < insSize; i++) {
                instructionTableData[i - start + 1][0] = String.valueOf(instructionStates.get(i).order);
                instructionTableData[i - start + 1][1] = String.valueOf(instructionStates.get(i).ins);
                instructionTableData[i - start + 1][2] = String.valueOf(instructionStates.get(i).issue);
                instructionTableData[i - start + 1][3] = String.valueOf(instructionStates.get(i).exec_comp);
                instructionTableData[i - start + 1][4] = String.valueOf(instructionStates.get(i).wb);
                for (int j = 2; j < 5; j++) {
                    if (instructionTableData[i - start + 1][j].contains("-")) {
                        instructionTableData[i - start + 1][j] = "";
                    }
                }
                int state = instructionStates.get(i).state;
                String ttt = "";
                if (state == InstructionState.ISSUE)
                    ttt = "ISSUE";
                else if (state == InstructionState.READY)
                    ttt = "ISSUE";
                else if (state == InstructionState.EXECUTE)
                    ttt = "EXEC";
                else if (state == InstructionState.WB)
                    ttt = "WB";
                else if (state == InstructionState.FINISHED)
                    ttt = "FINISHED";
                instructionTableData[i - start + 1][5] = ttt;
            }
            jf.remove(instructionTable);
            instructionTable = new JTable(new DefaultTableModel(instructionTableData, instructionTableColumnName));
            instructionTable.setBounds(10, 50, 500, 176);
            instructionTable.setPreferredSize(new Dimension(500, 176));
            instructionTable.setEnabled(false);
            jf.add(instructionTable);
        }

        public void initData() {
            instructionTableData = new String[11][7];
            instructionTableData[0] = instructionTableColumnName;
            for (int i = 1; i < 11; i++) {
                for (int j = 0; j < 7; j++) {
                    instructionTableData[i][j] = "";
                }
            }
        }

        public void initUi() {
            initData();
            currentTurnLabel = new JLabel("current turn: 0");
            currentTurnLabel.setBounds(200, 10, 200, 40);
            instructionTable = new JTable(new DefaultTableModel(instructionTableData, instructionTableColumnName));
            instructionTable.setBounds(10, 50, 500, 176);
            instructionTable.setEnabled(false);
            stepButton = new JButton("step");
            stepButton.setBounds(520, 50, 90, 30);
            stepButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    int s = Integer.parseInt(stepText.getText());
                    stepText.setEnabled(false);
                    stepButton.setEnabled(false);
                    stopButton.setEnabled(false);
                    goButton.setEnabled(false);
                    restartButton.setEnabled(false);
                    for (int i = 0; i < s; i++) {
                        if (!step_next()) {
                            break;
                        }
                        currentTurnLabel.setText("current Turn:" + String.valueOf(cur_T));
                    }
                    updateUi();
                    stepText.setEnabled(true);
                    stepButton.setEnabled(true);
                    stopButton.setEnabled(true);
                    goButton.setEnabled(true);
                    restartButton.setEnabled(true);
                }
            });
            stepText = new JTextField("1");
            stepText.setBounds(610, 50, 40, 30);

            goButton = new JButton("run");
            goButton.setBounds(520, 85, 90, 30);
            goButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    stepText.setEnabled(false);
                    stepButton.setEnabled(false);
                    // stopButton.setEnabled(false);
                    goButton.setEnabled(false);
                    restartButton.setEnabled(false);
                    Thread t = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            while (notPause) {
                                boolean c = step_next();
                                if (cur_T % 10 == 0) {
                                    currentTurnLabel.setText("current Turn:" + String.valueOf(cur_T));
                                    updateUi();
                                }
                                if (!c) {
                                    break;
                                }
                            }
                            currentTurnLabel.setText("current Turn:" + String.valueOf(cur_T));
                            updateUi();
                            notPause = true;
                            stepText.setEnabled(true);
                            stepButton.setEnabled(true);
                            stopButton.setEnabled(true);
                            goButton.setEnabled(true);
                            restartButton.setEnabled(true);
                        }
                    });
                    t.start();

                }
            });
            stopButton = new JButton("stop");
            stopButton.setBounds(520, 120, 90, 30);
            stopButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    notPause = false;
                }
            });
            restartButton = new JButton("clear");
            restartButton.setBounds(520, 155, 90, 30);
            restartButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    initAll();
                    initData();
                    currentTurnLabel.setText("current Turn:" + String.valueOf(cur_T));
                    jf.remove(instructionTable);
                    instructionTable = new JTable(
                            new DefaultTableModel(instructionTableData, instructionTableColumnName));
                    instructionTable.setBounds(10, 50, 500, 176);
                    instructionTable.setPreferredSize(new Dimension(500, 176));
                    instructionTable.setEnabled(false);
                    jf.add(instructionTable);
                }
            });
            quitButton = new JButton("quit");
            quitButton.setBounds(520, 190, 90, 30);
            quitButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    System.exit(0);
                }
            });
            jf.setSize(new Dimension(800, 500));
            instructionTable.setPreferredSize(new Dimension(500, 176));
            jf.add(instructionTable);
            jf.add(stopButton);
            jf.add(quitButton);
            jf.add(goButton);
            jf.add(stepText);
            jf.add(restartButton);
            jf.add(stepButton);
            jf.add(currentTurnLabel);
            jf.setBackground(Color.white);
            jf.setLayout(null);
            jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            jf.setVisible(true);
        }
    }
}
