import chisel3._
import chisel3.util._

class ifu extends Module {
  class buf_data extends Bundle {
    val ready = Bool()
    val valid = Bool()
    val in    = new inst_info()
    val out   = new inst_data()
    val intr  = new ifu_intr()
  }
  class ram_io extends Bundle {
    val en    = Bool()
    val wen   = UInt(4.W)
    val addr  = UInt(32.W)
    val wdata = UInt(32.W)
  }
  val io = IO(new Bundle {
    val in    = Flipped(Decoupled(new inst_info()))
    val out   = Decoupled(new inst_data())
    val intr  = Output(new ifu_intr())
    val rdok  = Input(Bool())
    val ram   = Output(new ram_io())
    val rdata = Input(UInt(32.W))
    val clear = Input(Bool())
  })

  val buf    = RegInit(Reg(new buf_data()))
  val instrd = Mux(buf.ready, buf.intr.instrd, io.in.bits.addr(1, 0).orR)
  val end    = instrd | io.clear

  buf.ready       := ~(buf.valid & io.out.ready) & (buf.ready | io.in.valid)
  buf.in.addr     := Mux(buf.ready, buf.in.addr, io.in.bits.addr)
  buf.intr.pc     := Mux(buf.ready, buf.intr.pc, io.in.bits.addr)
  buf.intr.instrd := instrd

  // fetch inst
  io.ram.en    := buf.ready & ~buf.valid & ~end
  io.ram.wen   := 0.U
  io.ram.addr  := buf.in.addr
  io.ram.wdata := 0.U

  buf.valid       := ~(buf.valid & io.out.ready) & (buf.valid | io.rdok | end)
  buf.out.pc      := MuxCase(buf.in.addr, Array(
    io.clear  -> 0.U,
    buf.valid -> buf.out.pc
  ))
  buf.out.inst    := MuxCase(io.rdata, Array(
    io.clear  -> 0.U,
    buf.valid -> buf.out.inst
  ))

  io.out.valid := buf.valid
  io.in.ready  := ~buf.ready
  io.out.bits  := Mux(buf.valid, buf.out, RegInit(Reg(new inst_data())))
  io.intr      := Mux(buf.valid, buf.intr, RegInit(Reg(new ifu_intr())))
}
