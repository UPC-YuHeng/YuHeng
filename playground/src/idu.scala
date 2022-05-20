import chisel3._
import chisel3.util._

import InstructionList._
import ALUOperationList._

class idu extends Module {
	class idu_in extends Bundle {
		val inst = UInt(32.W)
	}
	class idu_out extends Bundle {
		val rs  = UInt(5.W)
		val rt  = UInt(5.W)
		val rd  = UInt(5.W)
		val imm = UInt(32.W)
	}
	class idu_contr extends Bundle {
		// alu
		val alu_op    = UInt(4.W)
		val alu_src   = Bool()
		// reg
		val reg_write = Bool()
		val hi_write  = Bool()
		val lo_write  = Bool()
		val hi_read   = Bool()
		val lo_read   = Bool()
		val hilo_src  = Bool()
		// mem
		val mem_read  = Bool()
		val mem_write = Bool()
		val mem_mask  = UInt(2.W)
		// branch & jump
		val branch    = Bool()
		val cmp_op    = UInt(3.W)
		val jump      = Bool()
		val jsrc      = Bool()
		val call_src  = Bool()
		// signed / unsigned
		val signed    = Bool()
		// cp0
		val cp0_read  = Bool()
		val cp0_write = Bool()
	}
	class idu_intr extends Bundle {
		val eret      = Bool()
	}
	val io = IO(new Bundle {
		val in    = Input(new idu_in())
		val out   = Output(new idu_out())
		val contr = Output(new idu_contr())
		val intr  = Output(new idu_intr())
	})

	val rs     = io.in.inst(25, 21)
	val rt     = io.in.inst(20, 16)
	val rd     = io.in.inst(15, 11)
	val sa     = io.in.inst(10, 6)

	io.out.rs := Lookup(io.in.inst, rs, Array(
		SLLV    -> rt,
		SRAV    -> rt,
		SRLV    -> rt,
		SLL	  	-> rt,
		SRA		  -> rt,
		SRL 	  -> rt,
	))

	io.out.rt := Lookup(io.in.inst, rt, Array(
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

	io.out.rd := Lookup(io.in.inst, rd, Array(
		ADDI    -> rt,
		ADDIU   -> rt,
		SLTI    -> rt,
		SLTIU   -> rt,
		ANDI    -> rt,
		LUI     -> rt,
		ORI     -> rt,
		XORI    -> rt,
		LB      -> rt,
		LBU     -> rt,
		LH      -> rt,
		LHU     -> rt,
		LW      -> rt
	))

	def sext() = Cat(Fill(16, io.in.inst(15)), io.in.inst(15, 0))			// Sign Extended
	def zext() = Cat(0.U(16.W), io.in.inst(15, 0))										// Zero Extended
	def lext() = Cat(io.in.inst(15, 0), 0.U(16.W))										// LUI
	def fext() = Cat(0.U(27.W), sa)																		// SLL/SRA/SRL
	def jext() = Cat(0.U(6.W), io.in.inst(25,0))											// JAL

	io.out.imm := Lookup(io.in.inst, sext(), Array(
		ADDI    -> sext(),
		ADDIU   -> sext(),
		SLTI    -> sext(),
		SLTIU   -> sext(),
		ANDI    -> zext(),
		LUI     -> lext(),
		ORI     -> zext(),
		XORI    -> zext(),
		SLL     -> fext(),
		SRA     -> fext(),
		SRL     -> fext(),
		LB      -> sext(),
		LBU     -> sext(),
		LH      -> sext(),
		LHU     -> sext(),
		LW      -> sext(),
		SB      -> sext(),
		SH      -> sext(),
		SW      -> sext(),
		J       -> jext(),
		JAL     -> jext(),
		BLTZAL  -> sext(),
		BGEZAL  -> sext(),
		BLTZ    -> sext(),
		BLEZ    -> sext(),
		BGTZ    -> sext(),
		BGEZ    -> sext(),
		BNE     -> sext(),
		BEQ     -> sext()
	))

	io.contr.alu_op := Lookup(io.in.inst, alu_nop, Array(
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
		LH      -> alu_adds,
	))
	
	io.contr.alu_src := Lookup(io.in.inst, false.B, Array(
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
		LB	  	-> true.B,
		LBU	  	-> true.B,
		LH	  	-> true.B,
		LHU	  	-> true.B,
		LW	  	-> true.B,
		SB	  	-> true.B,
		SH	  	-> true.B,
		SW	  	-> true.B,
		LH	  	-> true.B,
	))

	io.contr.reg_write := Lookup(io.in.inst, false.B, Array(
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

	io.contr.hi_write := Lookup(io.in.inst, false.B, Array(
		DIV     -> true.B,
		DIVU    -> true.B,
		MULT    -> true.B,
		MULTU   -> true.B,
    MTHI    -> true.B
	))

  io.contr.lo_write := Lookup(io.in.inst, false.B, Array(
		DIV     -> true.B,
		DIVU    -> true.B,
		MULT    -> true.B,
		MULTU   -> true.B,
    MTLO    -> true.B
	))

	io.contr.hi_read := Lookup(io.in.inst, false.B, Array(
		MFHI    -> true.B
	))

	io.contr.lo_read := Lookup(io.in.inst, false.B, Array(
		MFLO    -> true.B
	))

	io.contr.hilo_src := Lookup(io.in.inst, false.B, Array(
		MTHI    -> true.B,
		MTLO    -> true.B
	))

	io.contr.mem_read := Lookup(io.in.inst, false.B, Array(
		LB      -> true.B,
		LBU     -> true.B,
		LH      -> true.B,
		LHU     -> true.B,
		LW      -> true.B
	))

	io.contr.mem_write := Lookup(io.in.inst, false.B, Array(
		SB      -> true.B,
		SH      -> true.B,
		SW      -> true.B
	))

	io.contr.mem_mask := Lookup(io.in.inst, 0.U, Array(
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

	io.contr.call_src := Lookup(io.in.inst, false.B, Array(
		BGEZAL  -> true.B,
		BLTZAL  -> true.B,
		JAL     -> true.B,
		JALR    -> true.B,
	))

	io.contr.cmp_op := Lookup(io.in.inst, 0.U, Array(
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
		BLTZAL  -> 7.U,
	))

	io.contr.branch := Lookup(io.in.inst, false.B, Array(
		BEQ     -> true.B,
		BNE     -> true.B,
		BGEZ    -> true.B,
		BGTZ    -> true.B,
		BLEZ    -> true.B,
		BLTZ    -> true.B,
		BGEZAL  -> true.B,
		BLTZAL  -> true.B
	))

	io.contr.jump := Lookup(io.in.inst, false.B, Array(
		J       -> true.B,
		JAL     -> true.B,
		JR      -> true.B,
		JALR    -> true.B
	))

	io.contr.jsrc := Lookup(io.in.inst, false.B, Array(
		JR      -> true.B,
		JALR    -> true.B
	))

	io.contr.signed := Lookup(io.in.inst, false.B, Array(
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

	io.contr.cp0_read := Lookup(io.in.inst, false.B, Array(
		MFC0    -> true.B
	))

	io.contr.cp0_write := Lookup(io.in.inst, false.B, Array(
		MTC0    -> true.B
	))

	io.intr.eret := Lookup(io.in.inst, false.B, Array(
		ERET    -> true.B
	))
}
