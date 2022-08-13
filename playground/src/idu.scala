import chisel3._
import chisel3.util._

import InstructionList._
import ALUOperationList._

class idu extends Module {
  class buf_data extends Bundle {
    val ready = Bool()
    val valid = Bool()
    val bits  = new idu_out()
  }
  val io = IO(new Bundle {
    val in     = Flipped(Decoupled(new idu_in()))
    val out    = Decoupled(new idu_out())
    val regin  = Output(new regread_in())
    val regout = Input (new regread_out())
    val conf   = Output(new conflict_data())
    val lock   = Input (Bool())
    val flush  = Input (Bool())
  })

  val clear = io.flush

  val in  = io.in
  val out = io.out
  val buf = RegInit(Reg(new buf_data()))

  val inst     = in.bits.data.inst
  val decoded  = Wire(Bool())         // for "reserved", implemented in the end.
  val syscall  = Mux((in.valid & in.ready), inst === SYSCALL, buf.bits.intr.syscall)
  val breakpt  = Mux((in.valid & in.ready), inst === BREAK,   buf.bits.intr.breakpt)
  val reserved = Mux((in.valid & in.ready), decoded,          buf.bits.intr.reserved)
  val eret     = Mux((in.valid & in.ready), inst === ERET,    buf.bits.intr.eret)

  val intr   = syscall | breakpt | reserved | eret

  io.in.ready  := ~buf.ready & (~io.lock)

  buf.ready := MuxCase(buf.ready, Array(    // low active
    clear                   -> false.B,
    (in.valid & in.ready)   -> true.B,
    (out.valid & out.ready) -> false.B
  ))

  val idu_data  = Wire(new exu_info())
  val idu_contr = Wire(new inst_contr())
  val idu_conf  = Wire(new conflict_data())
  val idu_intr  = Wire(new inst_intr())
  buf.bits.data   := MuxCase(buf.bits.data, Array(
    clear                   -> RegInit(Reg(new exu_info())),
    (in.valid & in.ready)   -> idu_data,
    (out.valid & out.ready) -> RegInit(Reg(new exu_info()))
  ))
  buf.bits.contr  := MuxCase(buf.bits.contr, Array(
    clear                   -> RegInit(Reg(new inst_contr())),
    (in.valid & in.ready)   -> idu_contr,
    (out.valid & out.ready) -> RegInit(Reg(new inst_contr()))
  ))
  buf.bits.conf   := MuxCase(buf.bits.conf, Array(
    clear                   -> RegInit(Reg(new conflict_data())),
    (in.valid & in.ready)   -> idu_conf,
    (out.valid & out.ready) -> RegInit(Reg(new conflict_data()))
  ))
  buf.bits.intr   := MuxCase(buf.bits.intr, Array(
    clear                   -> RegInit(Reg(new inst_intr())),
    (in.valid & in.ready)   -> idu_intr,
    (out.valid & out.ready) -> RegInit(Reg(new inst_intr()))
  ))

  buf.valid := MuxCase(buf.valid, Array(
    clear                   -> false.B,
    (in.valid & in.ready)   -> true.B,
    (out.valid & out.ready) -> false.B
  ))

  out.valid     := buf.valid
  out.bits      := buf.bits
  out.bits.conf := buf.bits.conf
  io.conf       := idu_conf

/****************************** data ******************************/
  val pc   = in.bits.data.pc
  val npc  = in.bits.data.pc + 4.U
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

  val reg_rs = Lookup(inst, 0.U, Array(
    ADD     -> rs,
    ADDI    -> rs,
    ADDU    -> rs,
    ADDIU   -> rs,
    SUB     -> rs,
    SUBU    -> rs,
    SLT     -> rs,
    SLTI    -> rs,
    SLTU    -> rs,
    SLTIU   -> rs,
    DIV     -> rs,
    DIVU    -> rs,
    MULT    -> rs,
    MULTU   -> rs,
    AND     -> rs,
    ANDI    -> rs,
    NOR     -> rs,
    OR      -> rs,
    ORI     -> rs,
    XOR     -> rs,
    XORI    -> rs,
    SLLV    -> rt,
    SLL     -> rt,
    SRAV    -> rt,
    SRA     -> rt,
    SRLV    -> rt,
    SRL     -> rt,
    BEQ     -> rs,
    BNE     -> rs,
    BGEZ    -> rs,
    BGTZ    -> rs,
    BLEZ    -> rs,
    BLTZ    -> rs,
    BGEZAL  -> rs,
    BLTZAL  -> rs,
    JR      -> rs,
    JALR    -> rs,
    MTHI    -> rs,
    MTLO    -> rs,
    LB      -> rs,
    LBU     -> rs,
    LH      -> rs,
    LHU     -> rs,
    LW      -> rs,
    SB      -> rs,
    SH      -> rs,
    SW      -> rs,
    MFC0    -> rd,
    MTC0    -> rd,
    MUL     -> rs
  ))
  val reg_rt = Lookup(inst, 0.U, Array(
    ADD     -> rt,
    ADDU    -> rt,
    SUB     -> rt,
    SUBU    -> rt,
    SLT     -> rt,
    SLTU    -> rt,
    DIV     -> rt,
    DIVU    -> rt,
    MULT    -> rt,
    MULTU   -> rt,
    AND     -> rt,
    NOR     -> rt,
    OR      -> rt,
    XOR     -> rt,
    SLLV    -> rs,
    SRAV    -> rs,
    SRLV    -> rs,
    BEQ     -> rt,
    BNE     -> rt,
    BGEZ    -> 0.U,
    BGTZ    -> 0.U,
    BLEZ    -> 0.U,
    BLTZ    -> 0.U,
    BGEZAL  -> 0.U,
    BLTZAL  -> 0.U,
    SB      -> rt,
    SH      -> rt,
    SW      -> rt,
    MFC0    -> rd,
    MTC0    -> rt,
    MUL     -> rt
  ))
  val reg_rd = Lookup(inst, rd, Array(
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
  io.regin.rs   := reg_rs
  io.regin.rt   := reg_rt
  idu_data.pc   := pc
  idu_data.srca := io.regout.rs
  idu_data.srcb := Mux(alu_src, imm, io.regout.rt)
  idu_data.data := io.regout.rt

/****************************** contr ******************************/
  idu_contr.alu_op := Lookup(inst, alu_nop, Array(
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
    SW      -> alu_adds,
    MUL     -> alu_mults
  ))
  idu_contr.mem_read := Lookup(inst, false.B, Array(
    LB      -> true.B,
    LBU     -> true.B,
    LH      -> true.B,
    LHU     -> true.B,
    LW      -> true.B
  ))
  idu_contr.mem_write := Lookup(inst, false.B, Array(
    SB      -> true.B,
    SH      -> true.B,
    SW      -> true.B
  ))
  idu_contr.mem_mask := Lookup(inst, 0.U, Array(
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
  idu_contr.reg_write := Lookup(inst, false.B, Array(
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
    MFC0    -> true.B,
    MUL     -> true.B
  ))
  idu_contr.hi_write := Lookup(inst, false.B, Array(
    DIV     -> true.B,
    DIVU    -> true.B,
    MULT    -> true.B,
    MULTU   -> true.B,
    MTHI    -> true.B
  ))
  idu_contr.lo_write := Lookup(inst, false.B, Array(
    DIV     -> true.B,
    DIVU    -> true.B,
    MULT    -> true.B,
    MULTU   -> true.B,
    MTLO    -> true.B
  ))
  idu_contr.hi_read := Lookup(inst, false.B, Array(
    MFHI    -> true.B
  ))
  idu_contr.lo_read := Lookup(inst, false.B, Array(
    MFLO    -> true.B
  ))
  idu_contr.hilo_src := Lookup(inst, false.B, Array(
    MTHI    -> true.B,
    MTLO    -> true.B
  ))
  idu_contr.jump := Lookup(inst, false.B, Array(
    J       -> true.B,
    JAL     -> true.B,
    JR      -> true.B,
    JALR    -> true.B
  ))
  idu_contr.jaddr := Lookup(inst, io.regout.rs, Array(
    J       -> Cat(npc(31, 28), imm(25, 0), 0.U(2.W)),
    JAL     -> Cat(npc(31, 28), imm(25, 0), 0.U(2.W))
  ))
  idu_contr.branch := Lookup(inst, false.B, Array(
    BEQ     -> true.B,
    BNE     -> true.B,
    BGEZ    -> true.B,
    BGTZ    -> true.B,
    BLEZ    -> true.B,
    BLTZ    -> true.B,
    BGEZAL  -> true.B,
    BLTZAL  -> true.B
  ))
  idu_contr.cmp := Lookup(inst, 0.U, Array(
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
  idu_contr.baddr := npc + Cat(Fill(14, imm(15)), imm(15, 0), 0.U(2.W))
  idu_contr.link := Lookup(inst, false.B, Array(
    BGEZAL  -> true.B,
    BLTZAL  -> true.B,
    JAL     -> true.B,
    JALR    -> true.B
  ))
  idu_contr.signed := Lookup(inst, false.B, Array(
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
    LW      -> true.B,
    MUL     -> true.B
  ))
  idu_contr.cp0_read := Lookup(inst, false.B, Array(
    MFC0    -> true.B
  ))
  idu_contr.cp0_write := Lookup(inst, false.B, Array(
    MTC0    -> true.B
  ))

/****************************** conf ******************************/
  idu_conf.rs := reg_rs
  idu_conf.rt := reg_rt
  idu_conf.rd := Mux(idu_contr.reg_write, reg_rd, 0.U)

/****************************** intr ******************************/
  idu_intr.instrd   := in.bits.intr.instrd
  idu_intr.datard   := false.B
  idu_intr.datawt   := false.B
  idu_intr.vaddr    := in.bits.intr.vaddr
  idu_intr.syscall  := syscall
  idu_intr.breakpt  := breakpt
  idu_intr.reserved := reserved
  idu_intr.eret     := eret
  idu_intr.exceed   := false.B

  decoded := Lookup(inst, true.B, Array(
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
    MTC0    -> false.B,
    MUL     -> false.B
  ))
}