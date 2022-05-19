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
    val in  = Input(new tlb_in())
    val out = Output(new tlb_out())
  })

  io.out.addr := Mux(io.in.addr(31),
    Mux(io.in.addr(30),
      io.in.addr,
      io.in.addr & 0x1fffffff.U
    ),
    io.in.addr
  )
}