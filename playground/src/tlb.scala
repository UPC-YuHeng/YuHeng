import chisel3._
import chisel3.util._

class tlb extends Module {
  class tlb_in extends Bundle {
    val addr = UInt(32.W)
  }
  class tlb_out extends Bundle {
    val addr = UInt(32.W)
  }
  val io = IO(new Bundle {
    val in  = Input (new tlb_in())
    val out = Output(new tlb_out())
  })
  io.out.addr := Mux(io.in.addr(31, 30) === "b10".U, Cat(0.U(3.W), io.in.addr(28, 0)), io.in.addr)
}