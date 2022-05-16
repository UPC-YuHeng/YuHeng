import chisel3._
import chisel3.util._

class ifu_in extends Bundle {
	val addr = UInt(32.W)
}

class ifu_out extends Bundle {
	val inst = UInt(32.W)
}

class ifu extends Module {
	val io = IO(new Bundle {
		val in  = Input(new ifu_in())
		val out = Output(new ifu_out())
	})

	io.out.inst := io.in.addr
}
