import java.io.*;
import java.util.*;
import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.text.DefaultStyledDocument.ElementSpec;
import java.awt.*;
import java.awt.event.*;

public class Tomasulo {

    // just change the following file dir,reserve station num,function unit num if
    // you like.
    String file_dir = "../testcases/test0.nel";
    // excute time
    public static int T_ADD = 3;
    public static int T_SUB = 3;
    public static int T_MUL = 12;
    public static int T_DIV = 40;
    public static int T_LDM = 3;
    public static int T_JUMP = 1;
    public static int T_ST = 3;
    public static int T_LD = 3;
    // reserve station num
    public int LS_STATION_NUM = 3;
    public int MUL_STATION_NUM = 3;
    public int ADD_STATION_NUM = 6;
    public int LOAD_BUFFER_NUM = 3;
    // function unit num
    public static int ADDER = 3;
    public static int MULT = 2;
    public static int LOAD = 2; // for memory load and store
    public static int LOADER = 2; // for load

    // op
    static String[] INSKEY = { "ADD", "SUB", "MUL", "DIV", "LDM", "JUMP", "ST", "LD" };
    public static final int OP_ADD = 0;
    public static final int OP_SUB = 1;
    public static final int OP_MUL = 2;
    public static final int OP_DIV = 3;
    public static final int OP_LDM = 4;
    public static final int OP_JUMP = 5;
    public static final int OP_ST = 6;
    public static final int OP_LD = 7;

    // reserve station type
    public static final int RS_LS = 20; // load and store
    public static final int RS_MUL = 21; // mul and div
    public static final int RS_ADD = 22; // add and sub

    // empty function unit num
    public int empty_adder = 3;
    public int empty_mult = 2;
    public int empty_load = 2;
    public int empty_buffer = 3;

    boolean notOver = false;

    // hardware
    public CalculateStation[] addReserveStation;
    public CalculateStation[] mulReserveStation;
    public LSStation[] lsReserveStation;
    public LSStation[] loadBuffer;
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
    Queue<Integer> loadWQ = new LinkedList<Integer>();

    public int[] F_state = new int[32];
    boolean onJump;

    // ui
    TomasuloUi ui;

    public Tomasulo() {
        // init
        initAll();
        ui = new TomasuloUi();
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
        loadWQ.clear();
        initReserveStation();
        String insStr;
        insStr = readFile(file_dir);
        initRegister();
        initMemory();
        initInsSet(insStr);
        empty_adder = ADDER;
        empty_mult = MULT;
        empty_load = LOAD;
        empty_buffer = LOADER;
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
                            break;
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
            } else if (curOp == OP_LDM || curOp == OP_ST) {
                for (int i = 0; i < LS_STATION_NUM; i++) {
                    if (!lsReserveStation[i].busy) {
                        // check RAW
                        if (curOp == OP_LDM) {
                            String temp[] = cur_order.split(",");
                            String t = temp[2].substring(2);
                            int src1 = InstructionState.hex2int(t);
                            boolean flag = true;
                            for (int j = 0; j < LS_STATION_NUM; j++) {
                                if (lsReserveStation[j].op == OP_ST && lsReserveStation[j].busy) {
                                    if (lsReserveStation[j].addr == src1) {
                                        canSend = false;
                                        flag = false;
                                    }
                                }
                            }
                            if (!flag)
                                break;
                        }
                        // check WAW and WAR
                        if (curOp == OP_ST) {
                            String temp[] = cur_order.split(",");
                            String t = temp[2].substring(2);
                            int src1 = InstructionState.hex2int(t);
                            boolean flag = true;
                            for (int j = 0; j < LS_STATION_NUM; j++) {
                                if (lsReserveStation[j].busy) {
                                    if (lsReserveStation[j].addr == src1) {
                                        canSend = false;
                                        flag = false;
                                    }
                                }
                            }
                            if (!flag)
                                break;
                        }
                        canSend = true;
                        lsReserveStation[i].busy = true;
                        lsReserveStation[i].insId = instructionStates.size();
                        // init instruction state
                        InstructionState ins = new InstructionState();
                        ins.init(cur_order, cur_T, pc);
                        instructionStates.add(ins);
                        // set reserveStation
                        lsReserveStation[i].op = ins.op;
                        if (ins.op == OP_LDM) {
                            lsReserveStation[i].addr = ins.src1;
                            F_state[ins.dst] = lsReserveStation[i].id;
                        } else {
                            lsReserveStation[i].addr = ins.dst;
                            if (F_state[ins.src1] == -1) {
                                lsReserveStation[i].f_ok = true;
                                lsReserveStation[i].q = F[ins.src1];
                            } else {
                                lsReserveStation[i].f_ok = false;
                                lsReserveStation[i].v = F_state[ins.src1];
                            }
                        }
                        break;
                    }
                }
            } else if (curOp == OP_LD) {
                for (int i = 0; i < LOAD_BUFFER_NUM; i++) {
                    if (!loadBuffer[i].busy) {
                        canSend = true;
                        loadBuffer[i].busy = true;
                        loadBuffer[i].insId = instructionStates.size();
                        // init instruction state
                        InstructionState ins = new InstructionState();
                        ins.init(cur_order, cur_T, pc);
                        instructionStates.add(ins);
                        // set reserveStation
                        loadBuffer[i].op = ins.op;
                        loadBuffer[i].addr = ins.src1;
                        F_state[ins.dst] = loadBuffer[i].id;
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
        int temp1 = 0;
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
                    temp1++;
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
                    temp1++;
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
                            pc += instructionStates.get(addReserveStation[i].insId).src2 - 1;
                        }
                        onJump = false;
                    }
                }
            } else {
                addWQ.offer(i);
                break;
            }
        }
        // the FU just finished this cycle,it can't be use in this cycle again.So add
        // after while loop
        empty_adder += temp1;

        // for mul
        int temp2 = 0;
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
                if (mulReserveStation[i].qk == 0) {
                    mulReserveStation[i].result = mulReserveStation[i].qj;
                    instructionStates.get(mulReserveStation[i].insId).state = InstructionState.WB;
                    instructionStates.get(mulReserveStation[i].insId).exec_comp = cur_T;
                    continue;
                }
                if (instructionStates.get(mulReserveStation[i].insId).exec_timer == 0) {
                    temp2++;
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
                if (mulReserveStation[i].qk == 0) {
                    mulReserveStation[i].result = mulReserveStation[i].qj;
                    instructionStates.get(mulReserveStation[i].insId).state = InstructionState.WB;
                    instructionStates.get(mulReserveStation[i].insId).exec_comp = cur_T;
                    continue;
                }
                if (instructionStates.get(mulReserveStation[i].insId).exec_timer == 0) {
                    temp2++;
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
        empty_mult += temp2;

        // for L/S
        int temp3 = 0;
        for (int i = 0; i < LS_STATION_NUM; i++) {
            // not empty
            if (!lsReserveStation[i].busy)
                continue;
            // // not the one just issued
            // if (instructionStates.get(lsReserveStation[i].insId).issue == cur_T)
            // continue;
            // ready but not excuting
            if (instructionStates.get(lsReserveStation[i].insId).state == InstructionState.ISSUE) {
                if (lsReserveStation[i].op == OP_LDM) {
                    instructionStates.get(lsReserveStation[i].insId).state = InstructionState.READY;
                    instructionStates.get(lsReserveStation[i].insId).ready_turn = cur_T;
                    lsWQ.offer(i);
                } else {
                    if (lsReserveStation[i].f_ok) {
                        instructionStates.get(lsReserveStation[i].insId).state = InstructionState.READY;
                        instructionStates.get(lsReserveStation[i].insId).ready_turn = cur_T;
                        lsWQ.offer(i);
                    }
                }
            } else if (instructionStates.get(lsReserveStation[i].insId).state == InstructionState.EXECUTE) {
                instructionStates.get(lsReserveStation[i].insId).exec_timer -= 1;
                if (instructionStates.get(lsReserveStation[i].insId).exec_timer == 0) {
                    instructionStates.get(lsReserveStation[i].insId).state = InstructionState.WB;
                    instructionStates.get(lsReserveStation[i].insId).exec_comp = cur_T;
                    temp3++;
                    // get result
                    if (lsReserveStation[i].addr > 4095 || lsReserveStation[i].addr < 0) {
                        if (lsReserveStation[i].op == OP_LDM) {
                            lsReserveStation[i].result = 0;
                        }
                    } else if (lsReserveStation[i].op == OP_LDM) {
                        lsReserveStation[i].result = memory[lsReserveStation[i].addr];
                    } else {
                        lsReserveStation[i].result = lsReserveStation[i].q;
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
                    temp3++;
                    instructionStates.get(lsReserveStation[i].insId).state = InstructionState.WB;
                    instructionStates.get(lsReserveStation[i].insId).exec_comp = cur_T;
                    // get result
                    if (lsReserveStation[i].addr > 4095 || lsReserveStation[i].addr < 0) {
                        if (lsReserveStation[i].op == OP_LDM) {
                            lsReserveStation[i].result = 0;
                        }
                    } else if (lsReserveStation[i].op == OP_LDM) {
                        lsReserveStation[i].result = memory[lsReserveStation[i].addr];
                    } else {
                        lsReserveStation[i].result = lsReserveStation[i].q;
                        memory[lsReserveStation[i].addr] = lsReserveStation[i].result;
                    }
                }
            } else {
                lsWQ.offer(i);
                break;
            }
        }
        empty_load += temp3;

        // for load
        int temp4 = 0;
        for (int i = 0; i < LOAD_BUFFER_NUM; i++) {
            // not empty
            if (!loadBuffer[i].busy)
                continue;
            // ready but not excuting
            if (instructionStates.get(loadBuffer[i].insId).state == InstructionState.ISSUE) {
                instructionStates.get(loadBuffer[i].insId).state = InstructionState.READY;
                instructionStates.get(loadBuffer[i].insId).ready_turn = cur_T;
                loadWQ.offer(i);
            } else if (instructionStates.get(loadBuffer[i].insId).state == InstructionState.EXECUTE) {
                instructionStates.get(loadBuffer[i].insId).exec_timer -= 1;
                if (instructionStates.get(loadBuffer[i].insId).exec_timer == 0) {
                    temp4 += 1;
                    instructionStates.get(loadBuffer[i].insId).state = InstructionState.WB;
                    instructionStates.get(loadBuffer[i].insId).exec_comp = cur_T;
                    // get result
                    loadBuffer[i].result = loadBuffer[i].addr;
                }
            }
        }
        while (empty_buffer > 0 && loadWQ.size() > 0) {
            int i = loadWQ.poll();
            // the one who just issued should excute next turn.Others can start this turn
            if (instructionStates.get(loadBuffer[i].insId).issue != cur_T) {
                empty_buffer--;
                instructionStates.get(loadBuffer[i].insId).state = InstructionState.EXECUTE;
                instructionStates.get(loadBuffer[i].insId).exec_timer -= 1;
                if (instructionStates.get(loadBuffer[i].insId).exec_timer == 0) {
                    temp4++;
                    instructionStates.get(loadBuffer[i].insId).state = InstructionState.WB;
                    instructionStates.get(loadBuffer[i].insId).exec_comp = cur_T;
                    // get result
                    loadBuffer[i].result = loadBuffer[i].addr;
                }
            } else {
                loadWQ.offer(i);
                break;
            }
        }
        empty_buffer += temp4;
        /*---------------------------------*/
        /*----------- write back-----------*/
        /*---------------------------------*/
        // for add
        for (int i = 0; i < ADD_STATION_NUM; i++) {
            if (!addReserveStation[i].busy)
                continue;
            if (instructionStates.get(addReserveStation[i].insId).state == InstructionState.WB) {
                if (instructionStates.get(addReserveStation[i].insId).exec_comp != cur_T) {
                    addReserveStation[i].busy = false;
                    if (addReserveStation[i].op != OP_JUMP) {
                        // F[instructionStates.get(addReserveStation[i].insId).dst] =
                        // addReserveStation[i].result;
                        broadcast(addReserveStation[i].id, addReserveStation[i].result);
                    }
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
                    // F[instructionStates.get(mulReserveStation[i].insId).dst] =
                    // mulReserveStation[i].result;
                    mulReserveStation[i].busy = false;
                    broadcast(mulReserveStation[i].id, mulReserveStation[i].result);
                    instructionStates.get(mulReserveStation[i].insId).state = InstructionState.FINISHED;
                    instructionStates.get(mulReserveStation[i].insId).wb = cur_T;
                }
            }
        }
        // for ls
        for (int i = 0; i < LS_STATION_NUM; i++) {
            if (!lsReserveStation[i].busy)
                continue;
            if (instructionStates.get(lsReserveStation[i].insId).state == InstructionState.WB) {
                if (instructionStates.get(lsReserveStation[i].insId).exec_comp != cur_T) {
                    lsReserveStation[i].busy = false;
                    instructionStates.get(lsReserveStation[i].insId).state = InstructionState.FINISHED;
                    instructionStates.get(lsReserveStation[i].insId).wb = cur_T;
                    if (lsReserveStation[i].op == OP_LDM) {
                        broadcast(lsReserveStation[i].id, lsReserveStation[i].result);
                        // F[instructionStates.get(lsReserveStation[i].insId).dst] =
                        // lsReserveStation[i].result;
                    }
                }
            }
        }
        // for load
        for (int i = 0; i < LOAD_BUFFER_NUM; i++) {
            if (!loadBuffer[i].busy)
                continue;
            if (instructionStates.get(loadBuffer[i].insId).state == InstructionState.WB) {
                if (instructionStates.get(loadBuffer[i].insId).exec_comp != cur_T) {
                    loadBuffer[i].busy = false;
                    // F[instructionStates.get(loadBuffer[i].insId).dst] = loadBuffer[i].result;
                    broadcast(loadBuffer[i].id, loadBuffer[i].result);
                    instructionStates.get(loadBuffer[i].insId).state = InstructionState.FINISHED;
                    instructionStates.get(loadBuffer[i].insId).wb = cur_T;
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

        for (int i = 0; i < LS_STATION_NUM; i++) {
            if (!lsReserveStation[i].busy)
                continue;
            if (lsReserveStation[i].op == OP_ST) {
                if (!lsReserveStation[i].f_ok) {
                    if (lsReserveStation[i].v == rsId) {
                        lsReserveStation[i].f_ok = true;
                        lsReserveStation[i].q = res;
                    }
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
            tmp += "F" + i + " ";
            tmp1 += F[i] + " ";
            tmp2 += F_state[i] + " ";
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
            lsReserveStation[i] = new LSStation("laod" + i, id++);
        }
        loadBuffer = new LSStation[LOAD_BUFFER_NUM];
        for (int i = 0; i < LOAD_BUFFER_NUM; i++) {
            loadBuffer[i] = new LSStation("LB" + i, id++);
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

    // ui
    public class TomasuloUi {
        JFrame jf;

        JTable instructionTable;
        String[] instructionTableColumnName = { "addr", "instruction", "ISSUE", "EXEC_COPM", "WB", "Status" };
        String[][] instructionTableData;

        JTable rsTable;
        String[] rsTableColumnName = { "name", "id", "busy", "op", "Qj", "Qk", "Vj", "Vk" };
        String[][] rsTableData;

        JTable registerTable;
        String[] registerTableColumnName;
        String[][] registerTableData;

        JTable loadTable;
        String[] loadTableColumnName = { "name", "busy", "addr/value", "FU" };
        String[][] loadTableData;

        JTable memoryTable;
        String[] memoryTableColumnName = { "1", "2" };
        String[][] memoryTableData;

        JLabel currentTurnLabel;

        JButton stepButton;
        JTextField stepText;
        JButton goButton;
        JButton stopButton;
        JButton quitButton;
        JButton restartButton;
        JButton fileButton;
        JButton searchButton;
        JButton setButton;

        String fileName = "test0.nel";

        boolean notPause = true;

        public TomasuloUi() {
            jf = new JFrame("Tomasulo");

            initUi();
        }

        public void updateUi() {
            updateInstructionTable();
            updateRsTable();
            updateRegisterTable();
            updateLoadTable();
            currentTurnLabel.setText("Cycle :" + String.valueOf(cur_T) + "   " + fileName);
            jf.validate();
        }

        public String getStationName(int id) {
            for (int i = 0; i < ADD_STATION_NUM; i++) {
                if (addReserveStation[i].id == id) {
                    return addReserveStation[i].name;
                }
            }
            for (int i = 0; i < MUL_STATION_NUM; i++) {
                if (mulReserveStation[i].id == id) {
                    return mulReserveStation[i].name;
                }
            }
            for (int i = 0; i < LS_STATION_NUM; i++) {
                if (lsReserveStation[i].id == id) {
                    return lsReserveStation[i].name;
                }
            }
            for (int i = 0; i < LOAD_BUFFER_NUM; i++) {
                if (loadBuffer[i].id == id) {
                    return loadBuffer[i].name;
                }
            }
            return "";
        }

        public void updateRegisterTable() {
            registerTableData = new String[6][17];
            registerTableData[0][0] = "";
            registerTableData[1][0] = "v";
            registerTableData[2][0] = "s";
            registerTableData[3][0] = "";
            registerTableData[4][0] = "v";
            registerTableData[5][0] = "s";
            for (int i = 0; i < 32; i++) {
                if (i / 16 == 0) {
                    registerTableData[0][i + 1] = "F" + String.valueOf(i);
                    registerTableData[1][i + 1] = String.valueOf(F[i]);
                    registerTableData[2][i + 1] = F_state[i] == -1 ? "" : getStationName(F_state[i]);
                } else {
                    registerTableData[3][i % 16 + 1] = "F" + String.valueOf(i);
                    registerTableData[4][i % 16 + 1] = String.valueOf(F[i]);
                    registerTableData[5][i % 16 + 1] = F_state[i] == -1 ? "" : getStationName(F_state[i]);
                }
            }
            jf.remove(registerTable);
            registerTable = new JTable(new DefaultTableModel(registerTableData, registerTableColumnName));
            registerTable.setPreferredSize(new Dimension(750, 98));
            registerTable.setBounds(10, 260 + 20 * (ADD_STATION_NUM + MUL_STATION_NUM), 750, 98);
            registerTable.setEnabled(false);
            jf.add(registerTable);
        }

        public void updateRsTable() {
            rsTableData = new String[ADD_STATION_NUM + MUL_STATION_NUM + 1][8];
            rsTableData[0] = rsTableColumnName;
            for (int i = 1; i < ADD_STATION_NUM + MUL_STATION_NUM + 1; i++) {
                if (i <= ADD_STATION_NUM) {
                    rsTableData[i][0] = addReserveStation[i - 1].name;
                    rsTableData[i][1] = String.valueOf(addReserveStation[i - 1].id);
                    rsTableData[i][2] = "no";
                    if (addReserveStation[i - 1].busy) {
                        rsTableData[i][2] = "yes";
                        rsTableData[i][3] = INSKEY[addReserveStation[i - 1].op];
                        if (addReserveStation[i - 1].vj_ok) {
                            rsTableData[i][4] = String.valueOf(addReserveStation[i - 1].qj);
                            rsTableData[i][6] = "";
                        } else {
                            rsTableData[i][4] = "";
                            rsTableData[i][6] = getStationName(addReserveStation[i - 1].vj);
                        }
                        if (addReserveStation[i - 1].vk_ok) {
                            rsTableData[i][5] = String.valueOf(addReserveStation[i - 1].qk);
                            rsTableData[i][7] = "";
                        } else {
                            rsTableData[i][5] = "";
                            rsTableData[i][7] = getStationName(addReserveStation[i - 1].vk);
                        }
                    } else {
                        for (int j = 3; j < 8; j++) {
                            rsTableData[i][j] = "";
                        }
                    }
                } else {
                    rsTableData[i][0] = mulReserveStation[i - ADD_STATION_NUM - 1].name;
                    rsTableData[i][1] = String.valueOf(mulReserveStation[i - ADD_STATION_NUM - 1].id);
                    rsTableData[i][2] = "no";
                    if (mulReserveStation[i - ADD_STATION_NUM - 1].busy) {
                        rsTableData[i][2] = "yes";
                        rsTableData[i][3] = INSKEY[mulReserveStation[i - ADD_STATION_NUM - 1].op];
                        if (mulReserveStation[i - ADD_STATION_NUM - 1].vj_ok) {
                            rsTableData[i][4] = String.valueOf(mulReserveStation[i - ADD_STATION_NUM - 1].qj);
                            rsTableData[i][6] = "";
                        } else {
                            rsTableData[i][4] = "";
                            rsTableData[i][6] = getStationName(mulReserveStation[i - ADD_STATION_NUM - 1].vj);
                        }
                        if (mulReserveStation[i - ADD_STATION_NUM - 1].vk_ok) {
                            rsTableData[i][5] = String.valueOf(mulReserveStation[i - ADD_STATION_NUM - 1].qk);
                            rsTableData[i][7] = "";
                        } else {
                            rsTableData[i][5] = "";
                            rsTableData[i][7] = getStationName(mulReserveStation[i - ADD_STATION_NUM - 1].vk);
                        }
                    } else {
                        for (int j = 3; j < 8; j++) {
                            rsTableData[i][j] = "";
                        }
                    }
                }
            }
            jf.remove(rsTable);
            rsTable = new JTable(new DefaultTableModel(rsTableData, rsTableColumnName));
            rsTable.setPreferredSize(new Dimension(500, 20 * (ADD_STATION_NUM + MUL_STATION_NUM)));
            rsTable.setBounds(10, 250, 500, 20 * (ADD_STATION_NUM + MUL_STATION_NUM));
            rsTable.setEnabled(false);
            jf.add(rsTable);
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
            instructionTable.setBounds(10, 50, 600, 176);
            instructionTable.setPreferredSize(new Dimension(600, 176));
            instructionTable.setEnabled(false);
            jf.add(instructionTable);
        }

        public void updateMemoryTable() {
            int address = 0;
            try {
                address = Integer.parseInt(memoryTable.getValueAt(0, 1).toString());
            } catch (Exception e) {
                return;
            }
            memoryTableData[0][1] = String.valueOf(address);
            if (address >= 4096 || address < 0) {
                memoryTableData[1][1] = "address invalid";
            } else {
                memoryTableData[1][1] = String.valueOf(memory[address]);
            }

            jf.remove(memoryTable);
            memoryTable = new JTable(new DefaultTableModel(memoryTableData, memoryTableColumnName));
            memoryTable.setPreferredSize(new Dimension(200, 35));
            memoryTable.setBounds(10, 260 + 20 * (ADD_STATION_NUM + MUL_STATION_NUM) + 120, 200, 35);
            jf.add(memoryTable);
        }

        public void setMemory() {
            int address = 0;
            try {
                address = Integer.parseInt(memoryTable.getValueAt(0, 1).toString());
            } catch (Exception e) {
                return;
            }
            int value = 0;
            try {
                value = Integer.parseInt(memoryTable.getValueAt(1, 1).toString());
            } catch (Exception e) {
                return;
            }
            if (address >= 4096 || address < 0) {
                memoryTableData[1][1] = "address invalid";
                jf.remove(memoryTable);
                memoryTable = new JTable(new DefaultTableModel(memoryTableData, memoryTableColumnName));
                memoryTable.setPreferredSize(new Dimension(200, 35));
                memoryTable.setBounds(10, 260 + 20 * (ADD_STATION_NUM + MUL_STATION_NUM) + 120, 200, 35);
                jf.add(memoryTable);
            } else {
                memory[address] = value;
            }
        }

        public void initData() {
            instructionTableData = new String[11][7];
            instructionTableData[0] = instructionTableColumnName;
            for (int i = 1; i < 11; i++) {
                for (int j = 0; j < 7; j++) {
                    instructionTableData[i][j] = "";
                }
            }

            rsTableData = new String[ADD_STATION_NUM + MUL_STATION_NUM + 1][8];
            rsTableData[0] = rsTableColumnName;
            for (int i = 1; i < ADD_STATION_NUM + MUL_STATION_NUM + 1; i++) {
                if (i <= ADD_STATION_NUM) {
                    rsTableData[i][0] = addReserveStation[i - 1].name;
                    rsTableData[i][1] = String.valueOf(addReserveStation[i - 1].id);
                    rsTableData[i][2] = "no";
                } else {
                    rsTableData[i][0] = mulReserveStation[i - ADD_STATION_NUM - 1].name;
                    rsTableData[i][1] = String.valueOf(mulReserveStation[i - ADD_STATION_NUM - 1].id);
                    rsTableData[i][2] = "no";
                }
            }

            registerTableColumnName = new String[17];
            registerTableColumnName[0] = "";
            for (int i = 1; i < 17; i++) {
                registerTableColumnName[i] = String.valueOf(i - 1);
            }
            registerTableData = new String[6][17];
            registerTableData[0][0] = "";
            registerTableData[1][0] = "v";
            registerTableData[2][0] = "s";
            registerTableData[3][0] = "";
            registerTableData[4][0] = "v";
            registerTableData[5][0] = "s";
            for (int i = 0; i < 32; i++) {
                if (i / 16 == 0) {
                    registerTableData[0][i + 1] = "F" + String.valueOf(i);
                    registerTableData[1][i + 1] = String.valueOf(F[i]);
                    registerTableData[2][i + 1] = F_state[i] == -1 ? "" : getStationName(F_state[i]);
                } else {
                    registerTableData[3][i % 16 + 1] = "F" + String.valueOf(i);
                    registerTableData[4][i % 16 + 1] = String.valueOf(F[i]);
                    registerTableData[5][i % 16 + 1] = F_state[i] == -1 ? "" : getStationName(F_state[i]);
                }
            }

            loadTableData = new String[LS_STATION_NUM + LOAD_BUFFER_NUM + 1][4];
            loadTableData[0] = loadTableColumnName;
            for (int i = 0; i < LS_STATION_NUM; i++) {
                loadTableData[i + 1][0] = lsReserveStation[i].name;
                loadTableData[i + 1][1] = "no";
                loadTableData[i + 1][2] = "";
                loadTableData[i + 1][3] = "";
            }
            for (int i = LS_STATION_NUM; i < LS_STATION_NUM + LOAD_BUFFER_NUM; i++) {
                loadTableData[i + 1][0] = loadBuffer[i - LS_STATION_NUM].name;
                loadTableData[i + 1][1] = "no";
                loadTableData[i + 1][2] = "";
                loadTableData[i + 1][3] = "";
            }

            memoryTableData = new String[2][2];
            memoryTableData[0][0] = "address";
            memoryTableData[0][1] = memoryTableData[1][1] = "";
            memoryTableData[1][0] = "value";
        }

        public void updateLoadTable() {
            loadTableData = new String[LS_STATION_NUM + LOAD_BUFFER_NUM + 1][4];
            loadTableData[0] = loadTableColumnName;
            for (int i = 0; i < LS_STATION_NUM; i++) {
                loadTableData[i + 1][0] = lsReserveStation[i].name;
                if (!lsReserveStation[i].busy) {
                    loadTableData[i + 1][1] = "no";
                    loadTableData[i + 1][2] = "";
                    loadTableData[i + 1][3] = "";
                } else {
                    loadTableData[i + 1][1] = "yes";
                    loadTableData[i + 1][2] = String.valueOf(lsReserveStation[i].addr);
                    if (lsReserveStation[i].op == OP_LDM) {
                        loadTableData[i + 1][3] = "X";
                    } else {
                        if (lsReserveStation[i].f_ok) {
                            loadTableData[i + 1][3] = "";
                        } else {
                            loadTableData[i + 1][3] = getStationName(lsReserveStation[i].v);
                        }
                    }
                }
            }
            for (int i = LS_STATION_NUM; i < LS_STATION_NUM + LOAD_BUFFER_NUM; i++) {
                loadTableData[i + 1][0] = loadBuffer[i - LS_STATION_NUM].name;
                if (!loadBuffer[i - LS_STATION_NUM].busy) {
                    loadTableData[i + 1][1] = "no";
                    loadTableData[i + 1][2] = "";
                    loadTableData[i + 1][3] = "";
                } else {
                    loadTableData[i + 1][1] = "yes";
                    loadTableData[i + 1][2] = String.valueOf(loadBuffer[i - LS_STATION_NUM].addr);
                    loadTableData[i + 1][3] = "X";
                }
            }
            jf.remove(loadTable);
            loadTable = new JTable(new DefaultTableModel(loadTableData, loadTableColumnName));
            loadTable.setPreferredSize(new Dimension(250, (LS_STATION_NUM + LOAD_BUFFER_NUM) * 17 + 17));
            loadTable.setBounds(550, 250, 250, (LS_STATION_NUM + LOAD_BUFFER_NUM) * 17 + 17);
            loadTable.setEnabled(false);
            loadTable.validate();
            jf.add(loadTable);

        }

        public void initUi() {
            initData();
            currentTurnLabel = new JLabel(" Cycle : 0 " + fileName);
            currentTurnLabel.setBounds(200, 10, 200, 40);

            instructionTable = new JTable(new DefaultTableModel(instructionTableData, instructionTableColumnName));
            instructionTable.setBounds(10, 50, 600, 176);
            instructionTable.setEnabled(false);

            rsTable = new JTable(new DefaultTableModel(rsTableData, rsTableColumnName));
            rsTable.setPreferredSize(new Dimension(500, 20 * (ADD_STATION_NUM + MUL_STATION_NUM)));
            rsTable.setBounds(10, 250, 500, 20 * (ADD_STATION_NUM + MUL_STATION_NUM));
            rsTable.setEnabled(false);

            loadTable = new JTable(new DefaultTableModel(loadTableData, loadTableColumnName));
            loadTable.setPreferredSize(new Dimension(250, (LS_STATION_NUM + LOAD_BUFFER_NUM) * 17 + 17));
            loadTable.setBounds(550, 250, 250, (LS_STATION_NUM + LOAD_BUFFER_NUM) * 17 + 17);
            loadTable.setEnabled(false);

            registerTable = new JTable(new DefaultTableModel(registerTableData, registerTableColumnName));
            registerTable.setPreferredSize(new Dimension(750, 98));
            registerTable.setBounds(10, 260 + 20 * (ADD_STATION_NUM + MUL_STATION_NUM), 750, 98);
            registerTable.setEnabled(false);

            memoryTable = new JTable(new DefaultTableModel(memoryTableData, memoryTableColumnName));
            memoryTable.setPreferredSize(new Dimension(200, 35));
            memoryTable.setBounds(10, 260 + 20 * (ADD_STATION_NUM + MUL_STATION_NUM) + 120, 200, 35);

            searchButton = new JButton("find");
            searchButton.setBounds(240, 260 + 20 * (ADD_STATION_NUM + MUL_STATION_NUM) + 120, 70, 15);
            searchButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    updateMemoryTable();
                    updateUi();
                }
            });
            setButton = new JButton("set");
            setButton.setBounds(240, 278 + 20 * (ADD_STATION_NUM + MUL_STATION_NUM) + 120, 70, 15);
            setButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    setMemory();
                    updateUi();
                }
            });

            fileButton = new JButton("file...");
            fileButton.setBounds(10, 10, 100, 30);
            fileButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    int result = 0;
                    JFileChooser fileChooser = new JFileChooser();
                    try {
                        fileChooser.setCurrentDirectory((new File("../testcases")));
                    } catch (Exception s) {
                        return;
                    }
                    fileChooser.setDialogTitle("choose source code...");
                    fileChooser.setApproveButtonText("ok");
                    fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                    result = fileChooser.showOpenDialog(jf);
                    if (JFileChooser.APPROVE_OPTION == result) {
                        file_dir = fileChooser.getSelectedFile().getPath();
                        fileName = fileChooser.getSelectedFile().getName();
                    }
                    initAll();
                    initData();
                    updateUi();
                }
            });

            stepButton = new JButton("step");
            stepButton.setBounds(620, 50, 90, 30);
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
            stepText.setBounds(710, 50, 40, 30);

            goButton = new JButton("run");
            goButton.setBounds(620, 85, 90, 30);
            goButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    notPause = true;
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
                                    currentTurnLabel.setText(" Cycle :" + String.valueOf(cur_T) + " " + fileName);
                                    updateUi();
                                }
                                if (!c) {
                                    break;
                                }
                            }
                            currentTurnLabel.setText(" Cycle :" + String.valueOf(cur_T) + " " + fileName);
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
            stopButton.setBounds(620, 120, 90, 30);
            stopButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    notPause = false;
                }
            });
            restartButton = new JButton("clear");
            restartButton.setBounds(620, 155, 90, 30);
            restartButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    initAll();
                    initData();
                    updateUi();
                }
            });
            quitButton = new JButton("quit");
            quitButton.setBounds(620, 190, 90, 30);
            quitButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    System.exit(0);
                }
            });
            jf.setSize(new Dimension(850, 700));
            instructionTable.setPreferredSize(new Dimension(600, 176));
            jf.add(memoryTable);
            jf.add(loadTable);
            jf.add(instructionTable);
            jf.add(rsTable);
            jf.add(registerTable);

            jf.add(fileButton);
            jf.add(setButton);
            jf.add(searchButton);
            jf.add(stopButton);
            jf.add(quitButton);
            jf.add(goButton);
            jf.add(restartButton);
            jf.add(stepButton);

            jf.add(currentTurnLabel);
            jf.add(stepText);

            jf.setBackground(Color.white);
            jf.setLayout(null);
            jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            jf.setVisible(true);
        }
    }
}
