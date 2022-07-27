import chisel3._
import chisel3.util._

class amu extends Module {
  val io = IO(new Bundle {
    val contr = Input(new amu_contr())
    val intr  = Input(new amu_intr())
    val out   = Decoupled(new inst_info())
  })

  val pc  = RegInit("hbfc00000".U(32.W))
  val pre = RegInit("hbfc00000".U(32.W))

  pc := MuxCase(pc, Array(
    io.intr.intr    -> "hbfc00380".U,
    io.intr.eret    -> io.intr.eaddr,
    io.contr.branch -> io.contr.baddr,
    io.contr.jump   -> io.contr.jaddr,
    io.out.ready    -> (pc + 4.U)
  ))
  pre := Mux(io.out.ready, pc, pre)

  io.out.valid := io.out.ready
  io.out.bits.addr := Mux(io.out.ready, pc, pre)
}
