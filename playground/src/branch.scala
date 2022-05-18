import chisel3._
import chisel3.util._

class branch extends Module {
  class branch_in extends Bundle {
    val pc     = UInt(32.W)
    val branch = Bool()
    val jump   = Bool()
    val cmp    = Bool()
    val offset = UInt(32.W)
  }
  class branch_out extends Bundle {
    val pc = UInt(32.W)
  }
  val io = IO(new Bundle {
    val in  = Input(new branch_in())
    val out = Output(new branch_out())
  })
}
