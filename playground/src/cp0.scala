import chisel3._
import chisel3.util._

import CP0RegisterList._

class cp0 extends Module {
  class cp0_in extends Bundle {
    // normal
    val write = Bool()
    val addr  = UInt(5.W)
    val sel   = UInt(3.W)
    val data  = UInt(32.W)
    // intr
    val int   = UInt(6.W)
    val pc    = UInt(32.W)
    val epc   = UInt(32.W)
  }
  class cp0_out extends Bundle {
    val data = UInt(32.W)
    val epc  = UInt(32.W)
  }
  class cp0_intr extends Bundle {
    val intr    = Bool()
    val branch  = Bool()
    val addrrd  = Bool()
    val addrwt  = Bool()
    val exceed  = Bool()
    val syscall = Bool()
    val breakpt = Bool()
    val resinst = Bool()
    val eret    = Bool()
  }
  val io = IO(new Bundle {
    val in   = Input(new cp0_in())
    val out  = Output(new cp0_out())
    val intr = Input(new cp0_intr())
  })

  val cp0 = RegInit(VecInit(Seq.fill(32)(0.U(32.W))))

  when (reset.asBool()) {
    cp0(status) := Cat(0x0040.U(16.W), cp0(status)(15, 8), 0.U(8.W))
    cp0(cause)  := 0.U
  }

  when (io.in.write) {
    cp0(io.in.addr) := MuxLookup(io.in.addr, cp0(io.in.addr), Array(
      badvaddr -> cp0(io.in.addr),
      count    -> io.in.data,
      status   -> Cat(cp0(io.in.addr)(31, 16), io.in.data(15, 8), cp0(io.in.addr)(7, 2), io.in.data(1, 0)),
      cause    -> Cat(cp0(io.in.addr)(31, 10), io.in.data(9, 8), cp0(io.in.addr)(7, 0)),
      epc      -> io.in.data
    ))
  }

  // interrupt or exception
  val ext_int  = io.in.int.orR
  val intr     = io.intr.intr | ext_int

  when (intr) {
    cp0(epc)    := Mux(cp0(status)(1), cp0(epc), io.in.epc)
    cp0(cause)  := Cat(Mux(cp0(status)(1) & io.intr.branch, 1.U(1.W), 0.U(1.W)), cp0(cause)(30, 0))
    cp0(status) := Cat(cp0(status)(31, 2), 1.U(1.W), cp0(status)(0))
  }
  when (ext_int) {
    cp0(cause) := Cat(cp0(cause)(31, 16), io.in.int, cp0(cause)(9, 7), 0x00.U(5.W), cp0(cause)(1, 0))
  }
  when (io.intr.addrrd) {
    cp0(cause)    := Cat(cp0(cause)(31, 7), 0x04.U(5.W), cp0(cause)(1, 0))
    cp0(badvaddr) := io.in.pc
  }
  when (io.intr.addrwt) {
    cp0(cause)    := Cat(cp0(cause)(31, 7), 0x05.U(5.W), cp0(cause)(1, 0))
    cp0(badvaddr) := io.in.pc
  }
  when (io.intr.exceed) {
    cp0(cause) := Cat(cp0(cause)(31, 7), 0x0c.U(5.W), cp0(cause)(1, 0))
  }
  when (io.intr.syscall) {
    cp0(cause) := Cat(cp0(cause)(31, 7), 0x08.U(5.W), cp0(cause)(1, 0))
  }
  when (io.intr.breakpt) {
    cp0(cause) := Cat(cp0(cause)(31, 7), 0x09.U(5.W), cp0(cause)(1, 0))
  }
  when (io.intr.resinst) {
    cp0(cause) := Cat(cp0(cause)(31, 7), 0x0a.U(5.W), cp0(cause)(1, 0))
  }
  when (io.intr.eret) {
    cp0(status) := Cat(cp0(status)(31, 2), 0.U(1.W), cp0(status)(0))
  }

  io.out.data := cp0(io.in.addr)
  io.out.epc  := cp0(epc)
}