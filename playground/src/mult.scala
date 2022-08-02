import chisel3._
import chisel3.util._

class mult extends Module {
  class mult_in extends Bundle {
    val valid  = Bool()
    val x = UInt(32.W)
    val y = UInt(32.W)
    val signed = Bool()
  }
  class mult_out extends Bundle {
    val valid  = Bool()
    val h = UInt(32.W)
    val l = UInt(32.W)
  }
  val io = IO(new Bundle {
    val in  = Input (new mult_in())
    val out = Output(new mult_out())
  })

  val x = io.in.x
  val y = io.in.y

  io.out.valid := RegNext(io.in.valid)
  io.out.h     := RegNext(Mux(io.in.valid,
    Mux(io.in.signed, (x.asSInt() * y.asSInt()).asUInt()(63, 32), (x * y)(63, 32)),
    0.U
  ))
  io.out.l     := RegNext(Mux(io.in.valid,
    Mux(io.in.signed, (x.asSInt() * y.asSInt()).asUInt()(31,  0), (x * y)(31,  0)),
    0.U
  ))
}