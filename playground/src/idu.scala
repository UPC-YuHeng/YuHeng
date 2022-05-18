import chisel3._
import chisel3.util._

object InstructionList {
	// Arithmetic Operation
	val ADD     = BitPat("b000000 ????? ????? ????? 00000 100000")
	val ADDI    = BitPat("b001000 ????? ????? ????? ????? ??????")
	val ADDU    = BitPat("b000000 ????? ????? ????? 00000 100001")
	val ADDIU   = BitPat("b001001 ????? ????? ????? ????? ??????")
	val SUB     = BitPat("b000000 ????? ????? ????? 00000 100010")
	val SUBU    = BitPat("b000000 ????? ????? ????? 00000 100011")
	val SLT     = BitPat("b000000 ????? ????? ????? 00000 101010")
	val SLTI    = BitPat("b001010 ????? ????? ????? ????? ??????")
	val SLTU    = BitPat("b000000 ????? ????? ????? 00000 101011")
	val SLTIU   = BitPat("b001011 ????? ????? ????? ????? ??????")
	val DIV     = BitPat("b000000 ????? ????? 00000 00000 011010")
	val DIVU    = BitPat("b000000 ????? ????? 00000 00000 011011")
	val MULT    = BitPat("b000000 ????? ????? 00000 00000 011000")
	val MULTU   = BitPat("b000000 ????? ????? 00000 00000 011001")
	// Logical Operation
	val AND     = BitPat("b000000 ????? ????? ????? 00000 100100")
	val ANDI    = BitPat("b001100 ????? ????? ????? ????? ??????")
	val LUI     = BitPat("b001111 ????? ????? ????? ????? ??????")
	val NOR     = BitPat("b000000 ????? ????? ????? 00000 100111")
	val OR      = BitPat("b000000 ????? ????? ????? 00000 100101")
	val ORI     = BitPat("b001101 ????? ????? ????? ????? ??????")
	val XOR     = BitPat("b000000 ????? ????? ????? 00000 100110")
	val XORI    = BitPat("b001110 ????? ????? ????? ????? ??????")
	// Shift Operation
	val SLLV    = BitPat("b000000 ????? ????? ????? 00000 000100")
	val SLL     = BitPat("b000000 00000 ????? ????? ????? 000000")
	val SRAV    = BitPat("b000000 ????? ????? ????? 00000 000111")
	val SRA     = BitPat("b000000 00000 ????? ????? ????? 000011")
	val SRLV    = BitPat("b000000 ????? ????? ????? 00000 000110")
	val SRL     = BitPat("b000000 00000 ????? ????? ????? 000010")
	// Branch & Jump
	val BEQ     = BitPat("b000100 ????? ????? ????? ????? ??????")
	val BNE     = BitPat("b000101 ????? ????? ????? ????? ??????")
	val BGEZ    = BitPat("b000001 ????? 00001 ????? ????? ??????")
	val BGTZ    = BitPat("b000111 ????? 00001 ????? ????? ??????")
	val BLEZ    = BitPat("b000110 ????? 00000 ????? ????? ??????")
	val BLTZ    = BitPat("b000001 ????? 00000 ????? ????? ??????")
	val BGEZAL  = BitPat("b000001 ????? 10001 ????? ????? ??????")
	val BLTZAL  = BitPat("b000001 ????? 10000 ????? ????? ??????")
	val J       = BitPat("b000010 ????? ????? ????? ????? ??????")
	val JAL     = BitPat("b000011 ????? ????? ????? ????? ??????")
	val JR      = BitPat("b000000 ????? 00000 00000 00000 001000")
	val JALR    = BitPat("b000000 ????? 00000 ????? 00000 001001")
	// Data Transfer
	val MFHI    = BitPat("b000000 00000 00000 ????? 00000 010000")
	val MFLO    = BitPat("b000000 00000 00000 ????? 00000 010010")
	val MTHI    = BitPat("b000000 ????? 00000 00000 00000 010001")
	val MTLO    = BitPat("b000000 ????? 00000 00000 00000 010011")
	// Trap
	val BREAK   = BitPat("b000000 ????? ????? ????? ????? 001101")
	val SYSCALL = BitPat("b000000 ????? ????? ????? ????? 001100")
	// Memory Access
	val LB      = BitPat("b100000 ????? ????? ????? ????? ??????")
	val LBU     = BitPat("b100100 ????? ????? ????? ????? ??????")
	val LH      = BitPat("b100001 ????? ????? ????? ????? ??????")
	val LHU     = BitPat("b100101 ????? ????? ????? ????? ??????")
	val LW      = BitPat("b100011 ????? ????? ????? ????? ??????")
	val SB      = BitPat("b101000 ????? ????? ????? ????? ??????")
	val SH      = BitPat("b101001 ????? ????? ????? ????? ??????")
	val SW      = BitPat("b101011 ????? ????? ????? ????? ??????")
	// Privileged
	val ERET    = BitPat("b010000 10000 00000 00000 00000 011000")
	val MFC0    = BitPat("b010000 00000 ????? ????? 00000 000???")
	val MTC0    = BitPat("b010000 00100 ????? ????? 00000 000???")
}

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
		val alu_op    = UInt(4.W)
		val alu_src   = Bool()
		val reg_write = Bool()
		val hilo_en   = Bool()
		val trans_hi  = Bool()
		val trans_lo  = Bool()
		val cp0_read  = Bool()
		val cp0_write = Bool()
		val mem_read  = Bool()
		val mem_write = Bool()
		val mem_mask  = UInt(2.W)
		val call_src  = Bool()
		val cmp_op    = UInt(3.W)
		val branch    = Bool()
		val jr        = Bool()
		val signed    = Bool()
	}
	val io = IO(new Bundle {
		val in    = Input(new idu_in())
		val out   = Output(new idu_out())
		val contr = Output(new idu_contr())
	})

	val rs     = io.in.inst(25, 21)
	val rt     = io.in.inst(20, 16)
	val rd     = io.in.inst(15, 11)
	val sa     = io.in.inst(10, 6)

	io.out.rs := Lookup(io.in.inst, rs, Array(
		SLLV    -> rt,
		SRAV    -> rt,
		SRLV    -> rt
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

	def sext() = Cat(Fill(io.in.inst(15), 16), io.in.inst(15, 0))			// Sign Extended
	def zext() = Cat(0.U(16.W), io.in.inst(15, 0))										// Zero Extended
	def lext() = Cat(io.in.inst(15, 0), 0.U(16.W))										// LUI
	def fext() = Cat(0.U(27.W), sa)																		// SLL/SRA/SRL

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
		SW      -> sext()
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
		BLTZAL  -> alu_subu
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

	io.contr.hilo_en := Lookup(io.in.inst, false.B, Array(
		DIV     -> true.B,
		DIVU    -> true.B,
		MULT    -> true.B,
		MULTU   -> true.B
	))

	io.contr.trans_hi := Lookup(io.in.inst, false.B, Array(
		MFHI    -> true.B,
		MTHI    -> true.B
	))

	io.contr.trans_lo := Lookup(io.in.inst, false.B, Array(
		MFLO    -> true.B,
		MTLO    -> true.B
	))

	io.contr.cp0_read := Lookup(io.in.inst, false.B, Array(
		MFC0    -> true.B
	))

	io.contr.cp0_write := Lookup(io.in.inst, false.B, Array(
		MTC0    -> true.B
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
		JALR    -> true.B
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
		BLTZAL  -> true.B,
		J       -> true.B,
		JAL     -> true.B,
	))

	io.contr.jr := Lookup(io.in.inst, false.B, Array(
		JR      -> true.B,
		JALR    -> true.B
	))

	io.contr.signed := Lookup(io.in.inst, false.B, Array(
		SLT     -> true.B,
		SLTI    -> true.B,
		SLTU    -> false.B,
		SLTIU   -> false.B,
		BEQ     -> true.B,
		BNE     -> true.B,
		BGEZ    -> true.B,
		BGTZ    -> true.B,
		BLEZ    -> true.B,
		BLTZ    -> true.B,
		BGEZAL  -> true.B,
		BLTZAL  -> true.B,
		LB      -> true.B,
		LBU     -> false.B,
		LH      -> true.B,
		LHU     -> false.B,
		LW      -> true.B
	))
}
