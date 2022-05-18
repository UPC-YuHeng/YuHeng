import chisel3._
import chisel3.util._

import yuheng.debug.mem

class ifu extends Module {
	class ifu_in extends Bundle {
		val addr = UInt(32.W)
	}
	class ifu_out extends Bundle {
		val inst = UInt(32.W)
	}
	val io = IO(new Bundle {
		val in  = Input(new ifu_in())
		val out = Output(new ifu_out())
	})

	val imem = Module(new mem())
  imem.io.ren   := true.B
  imem.io.wen   := false.B
  imem.io.raddr := io.in.addr
  io.out.inst   := imem.io.rdata
}
