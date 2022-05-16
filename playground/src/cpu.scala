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
	exu.io.in.aluop := idu.io.contr.aluop
	exu.io.in.srca  := reg.io.out.rs_data
	exu.io.in.srcb  := reg.io.out.rt_data

	val reg = Module(new reg())
	reg.io.in.rs_addr  := idu.io.out.rs_addr
	reg.io.in.rt_addr  := idu.io.out.rt_addr
	reg.io.in.rd_addr  := idu.io.out.rd_addr
	reg.io.in.hilo_en  := idu.io.contr.hilo_en
	reg.io.in.trans_hi := idu.io.contr.trans_hi
	reg.io.in.trans_lo := idu.io.contr.trans_lo
}
