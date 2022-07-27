import chisel3._
import chisel3.util._

import InstructionList._
import ALUOperationList._

class idu extends Module {
  class buf_data extends Bundle {
    val valid    = Bool()
    val clear    = Bool()
    val out      = new exu_info()
    val contr    = new inst_contr()
    val conflict = new conflict_data()
    val intr     = new idu_intr()
  }
  val io = IO(new Bundle {
    val in       = Flipped(Decoupled(new inst_data()))
    val rdinfo   = Output(new idu_reginfo())
    val rddata   = Input (new idu_regdata())
    val out      = Decoupled(new exu_info())
    val contr    = Output(new inst_contr())
    val intr     = Output(new idu_intr())
    val conflict = Output(new conflict_data())
    val clear    = Input (Bool())
    val lock     = Input (Bool())
    val ok       = Output(Bool())
  })

  val ret_out      = Wire(new exu_info())
  val ret_contr    = Wire(new inst_contr())
  val ret_conflict = Wire(new conflict_data())
  val ret_intr     = Wire(new idu_intr())

  val buf   = RegInit(Reg(new buf_data()))
  val valid = buf.valid & io.out.ready
  io.ok    := valid

  val clear  = buf.clear | io.clear
  buf.clear  := ~valid & clear

  buf.valid := ~valid & (buf.valid | io.in.valid)
  buf.out   := MuxCase(Reg(new exu_info()), Array(
    clear       -> Reg(new exu_info()),
    io.lock     -> buf.out,
    buf.valid   -> buf.out,
    io.in.valid -> ret_out
  ))
  buf.contr := MuxCase(Reg(new inst_contr()), Array(
    clear       -> Reg(new inst_contr()),
    io.lock     -> buf.contr,
    buf.valid   -> buf.contr,
    io.in.valid -> ret_contr
  ))
  buf.conflict := MuxCase(Reg(new conflict_data()), Array(
    clear       -> Reg(new conflict_data()),
    io.lock     -> buf.conflict,
    buf.valid   -> buf.conflict,
    io.in.valid -> ret_conflict
  ))
  buf.intr  := MuxCase(Reg(new idu_intr()), Array(
    clear       -> Reg(new idu_intr()),
    io.lock     -> buf.intr,
    buf.valid   -> buf.intr,
    io.in.valid -> ret_intr
  ))

  io.out.valid := valid & ~clear & ~io.lock
  io.in.ready  := ~io.lock & io.out.ready
  io.out.bits  := Mux(io.out.valid, buf.out, RegInit(Reg(new exu_info())))
  io.contr     := Mux(io.out.valid, buf.contr, RegInit(Reg(new inst_contr())))
  io.intr      := Mux(io.out.valid, buf.intr, RegInit(Reg(new idu_intr())))
  io.conflict  := buf.conflict

  val pc   = io.in.bits.pc
  val npc  = io.in.bits.pc + 4.U
  val inst = io.in.bits.inst
  val rs   = inst(25, 21)
  val rt   = inst(20, 16)
  val rd   = inst(15, 11)

  def sext() = Cat(Fill(16, inst(15)), inst(15, 0)) // Sign Extended
  def zext() = Cat(0.U(16.W), inst(15, 0))          // Zero Extended
  def lext() = Cat(inst(15, 0), 0.U(16.W))          // LUI
  def fext() = Cat(0.U(27.W), inst(10, 6))          // SLL/SRA/SRL
  def jext() = Cat(0.U(6.W), inst(25,0))            // JAL

  val imm = Lookup(inst, sext(), Array(
    ANDI    -> zext(),
    LUI     -> lext(),
    ORI     -> zext(),
    XORI    -> zext(),
    SLL     -> fext(),
    SRA     -> fext(),
    SRL     -> fext(),
    J       -> jext(),
    JAL     -> jext()
  ))

  val alu_src = Lookup(inst, false.B, Array(
    ADDI    -> true.B,
    ADDIU   -> true.B,
    SLTI    -> true.B,
    SLTIU   -> true.B,
    ANDI    -> true.B,
    LUI     -> true.B,
    ORI     -> true.B,
    XORI    -> true.B,
    SLL     -> true.B,
    SRA     -> true.B,
    SRL     -> true.B,
    LB      -> true.B,
    LBU     -> true.B,
    LH      -> true.B,
    LHU     -> true.B,
    LW      -> true.B,
    SB      -> true.B,
    SH      -> true.B,
    SW      -> true.B,
  ))

  io.rdinfo.rs := Lookup(inst, rs, Array(
    SLLV    -> rt,
    SRAV    -> rt,
    SRLV    -> rt,
    SLL     -> rt,
    SRA     -> rt,
    SRL     -> rt
  ))
  io.rdinfo.rt := Lookup(inst, rt, Array(
    SLLV    -> rs,
    SRAV    -> rs,
    SRLV    -> rs,
    BGEZ    -> 0.U,
    BGTZ    -> 0.U,
    BLEZ    -> 0.U,
    BLTZ    -> 0.U,
    BGEZAL  -> 0.U,
    BLTZAL  -> 0.U
  ))

  ret_out.pc   := pc
  ret_out.srca := io.rddata.rs
  ret_out.srcb := Mux(alu_src, imm, io.rddata.rt)
  ret_out.rd := Lookup(inst, rd, Array(
    ADDI    -> rt,
    ADDIU   -> rt,
    SLTI    -> rt,
    SLTIU   -> rt,
    ANDI    -> rt,
    LUI     -> rt,
    ORI     -> rt,
    XORI    -> rt,
    BGEZAL  -> 31.U,
    BLTZAL  -> 31.U,
    JAL     -> 31.U,
    LB      -> rt,
    LBU     -> rt,
    LH      -> rt,
    LHU     -> rt,
    LW      -> rt,
    MFC0    -> rt
  ))
  ret_out.data := io.rddata.rt

  ret_contr.alu_op := Lookup(inst, alu_nop, Array(
    ADD     -> alu_adds,
    ADDI    -> alu_adds,
    ADDU    -> alu_addu,
    ADDIU   -> alu_addu,
    SUB     -> alu_subs,
    SUBU    -> alu_subu,
    SLT     -> alu_subu,
    SLTI    -> alu_subu,
    SLTU    -> alu_subu,
    SLTIU   -> alu_subu,
    DIV     -> alu_divs,
    DIVU    -> alu_divu,
    MULT    -> alu_mults,
    MULTU   -> alu_multu,
    AND     -> alu_and,
    ANDI    -> alu_and,
    LUI     -> alu_or,
    NOR     -> alu_nor,
    OR      -> alu_or,  
    ORI     -> alu_or,  
    XOR     -> alu_xor,
    XORI    -> alu_xor,
    SLLV    -> alu_sftl,
    SLL     -> alu_sftl,
    SRAV    -> alu_sftrs,
    SRA     -> alu_sftrs,
    SRLV    -> alu_sftru,
    SRL     -> alu_sftru,
    BEQ     -> alu_subu,
    BNE     -> alu_subu,
    BGEZ    -> alu_subu,
    BGTZ    -> alu_subu,
    BLEZ    -> alu_subu,
    BLTZ    -> alu_subu,
    BGEZAL  -> alu_subu,
    BLTZAL  -> alu_subu,
    LB      -> alu_adds,
    LBU     -> alu_adds,
    LH      -> alu_adds,
    LHU     -> alu_adds,
    LW      -> alu_adds,
    SB      -> alu_adds,
    SH      -> alu_adds,
    SW      -> alu_adds
  ))
  ret_contr.reg_write := Lookup(inst, false.B, Array(
    ADD     -> true.B,
    ADDI    -> true.B,
    ADDU    -> true.B,
    ADDIU   -> true.B,
    SUB     -> true.B,
    SUBU    -> true.B,
    SLT     -> true.B,
    SLTI    -> true.B,
    SLTU    -> true.B,
    SLTIU   -> true.B,
    AND     -> true.B,
    ANDI    -> true.B,
    LUI     -> true.B,
    NOR     -> true.B,
    OR      -> true.B,
    ORI     -> true.B,
    XOR     -> true.B,
    XORI    -> true.B,
    SLLV    -> true.B,
    SLL     -> true.B,
    SRAV    -> true.B,
    SRA     -> true.B,
    SRLV    -> true.B,
    SRL     -> true.B,
    BGEZAL  -> true.B,
    BLTZAL  -> true.B,
    JAL     -> true.B,
    JR      -> true.B,
    JALR    -> true.B,
    MFHI    -> true.B,
    MFLO    -> true.B,
    LB      -> true.B,
    LBU     -> true.B,
    LH      -> true.B,
    LHU     -> true.B,
    LW      -> true.B,
    MFC0    -> true.B
  ))
  ret_contr.hi_write := Lookup(inst, false.B, Array(
    DIV     -> true.B,
    DIVU    -> true.B,
    MULT    -> true.B,
    MULTU   -> true.B,
    MTHI    -> true.B
  ))
  ret_contr.lo_write := Lookup(inst, false.B, Array(
    DIV     -> true.B,
    DIVU    -> true.B,
    MULT    -> true.B,
    MULTU   -> true.B,
    MTLO    -> true.B
  ))
  ret_contr.hi_read := Lookup(inst, false.B, Array(
    MFHI    -> true.B
  ))
  ret_contr.lo_read := Lookup(inst, false.B, Array(
    MFLO    -> true.B
  ))
  ret_contr.hilo_src := Lookup(inst, false.B, Array(
    MTHI    -> true.B,
    MTLO    -> true.B
  ))
  ret_contr.mem_read := Lookup(inst, false.B, Array(
    LB      -> true.B,
    LBU     -> true.B,
    LH      -> true.B,
    LHU     -> true.B,
    LW      -> true.B
  ))
  ret_contr.mem_write := Lookup(inst, false.B, Array(
    SB      -> true.B,
    SH      -> true.B,
    SW      -> true.B
  ))
  ret_contr.mem_mask := Lookup(inst, 0.U, Array(
    // 0 -> 0B, 1 -> 1B, 2 -> 2B, 3 -> 4B
    LB      -> 1.U,
    LBU     -> 1.U,
    LH      -> 2.U,
    LHU     -> 2.U,
    LW      -> 3.U,
    SB      -> 1.U,
    SH      -> 2.U,
    SW      -> 3.U
  ))
  ret_contr.branch := Lookup(inst, false.B, Array(
    BEQ     -> true.B,
    BNE     -> true.B,
    BGEZ    -> true.B,
    BGTZ    -> true.B,
    BLEZ    -> true.B,
    BLTZ    -> true.B,
    BGEZAL  -> true.B,
    BLTZAL  -> true.B
  ))
  ret_contr.cmp := Lookup(inst, 0.U, Array(
    // 0 -> NOP, 1 -> Reserved, 2 -> "==", 3 -> "!="
    // 4 -> ">=", 5 -> ">", 6 -> "<=", 7 -> "<"
    SLT     -> 7.U,
    SLTI    -> 7.U,
    SLTU    -> 7.U,
    SLTIU   -> 7.U,
    BEQ     -> 2.U,
    BNE     -> 3.U,
    BGEZ    -> 4.U,
    BGTZ    -> 5.U,
    BLEZ    -> 6.U,
    BLTZ    -> 7.U,
    BGEZAL  -> 4.U,
    BLTZAL  -> 7.U
  ))
  ret_contr.baddr := npc + Cat(Fill(14, imm(15)), imm(15, 0), 0.U(2.W))
  ret_contr.jump := Lookup(inst, false.B, Array(
    J       -> true.B,
    JAL     -> true.B,
    JR      -> true.B,
    JALR    -> true.B
  ))
  ret_contr.jaddr := Lookup(inst, io.rddata.rs, Array(
    J       -> Cat(npc(31, 28), imm(25, 0), 0.U(2.W)),
    JAL     -> Cat(npc(31, 28), imm(25, 0), 0.U(2.W))
  ))
  ret_contr.link := Lookup(inst, false.B, Array(
    BGEZAL  -> true.B,
    BLTZAL  -> true.B,
    JAL     -> true.B,
    JALR    -> true.B
  ))
  ret_contr.signed := Lookup(inst, false.B, Array(
    SLT     -> true.B,
    SLTI    -> true.B,
    BEQ     -> true.B,
    BNE     -> true.B,
    BGEZ    -> true.B,
    BGTZ    -> true.B,
    BLEZ    -> true.B,
    BLTZ    -> true.B,
    BGEZAL  -> true.B,
    BLTZAL  -> true.B,
    LB      -> true.B,
    LH      -> true.B,
    LW      -> true.B
  ))
  // conflict
  ret_conflict.rs := io.rdinfo.rs
  ret_conflict.rt := io.rdinfo.rt
  ret_conflict.rd := ret_out.rd

  ret_intr.pc := pc
  ret_intr.syscall := Lookup(inst, false.B, Array(
    SYSCALL -> true.B
  ))
  ret_intr.breakpt := Lookup(inst, false.B, Array(
    BREAK   -> true.B
  ))
  ret_intr.reserved := Lookup(inst, true.B, Array(
    ADD     -> false.B,
    ADDI    -> false.B,
    ADDU    -> false.B,
    ADDIU   -> false.B,
    SUB     -> false.B,
    SUBU    -> false.B,
    SLT     -> false.B,
    SLTI    -> false.B,
    SLTU    -> false.B,
    SLTIU   -> false.B,
    DIV     -> false.B,
    DIVU    -> false.B,
    MULT    -> false.B,
    MULTU   -> false.B,
    AND     -> false.B,
    ANDI    -> false.B,
    LUI     -> false.B,
    NOR     -> false.B,
    OR      -> false.B,
    ORI     -> false.B,
    XOR     -> false.B,
    XORI    -> false.B,
    SLLV    -> false.B,
    SLL     -> false.B,
    SRAV    -> false.B,
    SRA     -> false.B,
    SRLV    -> false.B,
    SRL     -> false.B,
    BEQ     -> false.B,
    BNE     -> false.B,
    BGEZ    -> false.B,
    BGTZ    -> false.B,
    BLEZ    -> false.B,
    BLTZ    -> false.B,
    BGEZAL  -> false.B,
    BLTZAL  -> false.B,
    J       -> false.B,
    JAL     -> false.B,
    JR      -> false.B,
    JALR    -> false.B,
    MFHI    -> false.B,
    MFLO    -> false.B,
    MTHI    -> false.B,
    MTLO    -> false.B,
    BREAK   -> false.B,
    SYSCALL -> false.B,
    LB      -> false.B,
    LBU     -> false.B,
    LH      -> false.B,
    LHU     -> false.B,
    LW      -> false.B,
    SB      -> false.B,
    SH      -> false.B,
    SW      -> false.B,
    ERET    -> false.B,
    MFC0    -> false.B,
    MTC0    -> false.B
  ))
  ret_intr.eret := Lookup(inst, false.B, Array(
    ERET    -> true.B
  ))
  ret_intr.cp0_read := Lookup(inst, false.B, Array(
    MFC0    -> true.B
  ))
  ret_intr.cp0_write := Lookup(inst, false.B, Array(
    MTC0    -> true.B
  ))
  ret_intr.addr := Mux(ret_intr.cp0_write, rt, rd)
  ret_intr.sel  := inst(2, 0)
}
