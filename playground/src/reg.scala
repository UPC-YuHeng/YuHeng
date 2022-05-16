import chisel3._
import chisel3.util._

class reg_in extends Bundle {
  val reg_write = Bool()
  val rs_addr   = UInt(5.W)
  val rt_addr   = UInt(5.W)
  val rd_addr   = UInt(5.W)
  val rd_data   = UInt(32.W)
}

class reg_out extends Bundle {
  val rs_data = UInt(32.W)
  val rt_data = UInt(32.W)
}

class reg extends Module {
  val io = IO(new Bundle {
    val in  = Input(new reg_in())
    val out = Output(new reg_out())
  })

  val reg = RegInit(VecInit(Seq.fill(32)(0.U(32.W))))

  io.out.rs_data := reg(io.in.rs_addr)
  io.out.rt_data := reg(io.in.rt_data)
  when (io.in.reg_write) {
    reg(io.in.rd_addr) := io.in.rd_data
  }

  reg(0) = 0.U
}