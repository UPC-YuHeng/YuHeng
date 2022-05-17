import chisel3._
import chisel3.util._

class cpu extends Module {
	val io = IO(new Bundle {
	})

	val pc = RegInit((0x80000000).U(32.W))
	
	val ifu = Module(new ifu())
	ifu.io.in.addr := pc

	val idu = Module(new idu())
	idu.io.in.inst := ifu.io.out.inst

	val exu = Module(new exu())
	exu.io.in.alu_op := idu.io.contr.alu_op
	exu.io.in.cmp_op := idu.io.contr.cmp_op
	exu.io.in.signed := idu.io.contr.signed
	exu.io.in.srca   := reg.io.out.rs_data
	exu.io.in.srcb   := Mux(idu.io.contr.alu_src, idu.io.out.imm, reg.io.out.rt_data)

	val reg = Module(new reg())
	// reg
	reg.io.in.reg_write := idu.io.contr.reg_write
	reg.io.in.rs_addr   := idu.io.out.rs
	reg.io.in.rt_addr   := idu.io.out.rt
	reg.io.in.rd_addr   := idu.io.out.rd
	reg.io.in.rd_data   := Mux(idu.io.contr.call_src, pc + 8.U, exu.io.out.dest)
	// hi/lo
	reg.io.in.hilo_en   := idu.io.contr.hilo_en
	reg.io.in.trans_hi  := idu.io.contr.trans_hi
	reg.io.in.trans_lo  := idu.io.contr.trans_lo
	reg.io.in.hi_data   := exu.io.out.dest_hi
	reg.io.in.lo_data   := exu.io.out.dest_lo
	// cp0
	reg.io.in.cp0_read  := idu.io.contr.cp0_read
	reg.io.in.cp0_write := idu.io.contr.cp0_write
	reg.io.in.cp0_addr  := idu.io.out.rd
	reg.io.in.cp0_sel   := ifu.io.out.inst(3, 0)

	val branch = Module(new branch())
}
