import chisel3._
import chisel3.util._

class mem extends Module {
  class ram_io extends Bundle {
    val en    = Output(Bool())
    val wen   = Output(UInt(4.W))
    val addr  = Output(UInt(32.W))
    val wdata = Output(UInt(32.W))
    val rdata = Input(UInt(32.W))
  }
  class mem_in extends Bundle {
    val addr = UInt(32.W)
    val data = UInt(32.W)
  }
  class mem_out extends Bundle {
    val data = UInt(32.W)
  }
  class mem_contr extends Bundle {
    val mem_read  = Bool()
    val mem_write = Bool()
    val mem_mask  = UInt(2.W)
  }
  class mem_intr extends Bundle {
    val mem_addrrd = Bool()
    val mem_addrwt = Bool()
  }
  val io = IO(new Bundle {
    val exu_mem_valid = Input (Bool())
    val idu_exu_valid = Input (Bool())
    val pause = Input (Bool())
    val in    = Input (new mem_in())
    val out   = Output(new mem_out())
    val contr = Input (new mem_contr())
    val intr  = Output(new mem_intr())
    val ram   = new ram_io()
  })

  val tlb = Module(new tlb())
  tlb.io.in.addr     := io.in.addr

  io.ram.en    := (io.contr.mem_write | io.contr.mem_read) & io.exu_mem_valid
  io.ram.wen   := Mux(io.contr.mem_write & io.exu_mem_valid,
    MuxLookup(io.contr.mem_mask, 0.U, Array(
      1.U -> MuxLookup(tlb.io.out.addr(1, 0), 0.U, Array(
        "b00".U -> "b0001".U,
        "b01".U -> "b0010".U,
        "b10".U -> "b0100".U,
        "b11".U -> "b1000".U,
      )),
      2.U -> MuxLookup(tlb.io.out.addr(1, 0), 0.U, Array(
        "b00".U -> "b0011".U,
        "b10".U -> "b1100".U
      )),
      3.U -> 0xf.U
    )),
    0.U
  )
  io.ram.addr  := Cat(tlb.io.out.addr(31, 2), 0.U(2.W))
  io.ram.wdata := MuxLookup(io.contr.mem_mask, 0.U, Array(
    1.U -> Fill(4, io.in.data( 7, 0)),
    2.U -> Fill(2, io.in.data(15, 0)),
    3.U -> io.in.data
  ))
  io.out.data  := io.ram.rdata

  io.intr.mem_addrrd := (io.contr.mem_read & (~io.pause) & io.idu_exu_valid & MuxLookup(io.contr.mem_mask, false.B, Array(
    2.U -> tlb.io.out.addr(0),
    3.U -> tlb.io.out.addr(1, 0).orR
  )))
  io.intr.mem_addrwt := (io.contr.mem_write & (~io.pause) & io.idu_exu_valid & MuxLookup(io.contr.mem_mask, false.B, Array(
    2.U -> tlb.io.out.addr(0),
    3.U -> tlb.io.out.addr(1, 0).orR
  )))
}
