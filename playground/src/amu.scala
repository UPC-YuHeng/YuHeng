import chisel3._
import chisel3.util._

class amu extends Module {
  val io = IO(new Bundle {
    val in  = Input(new amu_in())
    val out = Decoupled(new amu_out())
  })

  val in  = io.in
  val out = io.out

  val pc     = RegInit("hbfc00000".U(32.W))
  val pre_pc = RegInit("hbfc00000".U(32.W))

  pc := MuxCase(pc, Array(
    in.intr.intr    -> "hbfc00380".U,
    in.intr.eret    -> in.intr.eaddr,
    in.contr.branch -> in.contr.baddr,
    in.contr.jump   -> in.contr.jaddr,
    out.ready       -> (pc + 4.U)
  ))
  pre_pc := out.bits.data.addr

  out.valid := true.B
  out.bits.data.addr := MuxCase(pre_pc, Array(
    in.intr.intr    -> "hbfc00380".U,
    in.intr.eret    -> in.intr.eaddr,
    out.ready       -> pc
  ))
}
