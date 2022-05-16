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

	val reg = Module(new reg())
	reg.io.in.rs_addr := idu.io.out.rs_addr
	reg.io.in.rt_addr := idu.io.out.rt_addr
	reg.io.in.rd_addr := idu.io.out.rd_addr
}