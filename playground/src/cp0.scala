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
    val vaddr = UInt(32.W)
    val epc   = UInt(32.W)
  }
  class cp0_out extends Bundle {
    val data     = UInt(32.W)
    val epc      = UInt(32.W)
    val exl      = Bool()
    val ie       = Bool()
    val soft_int = Bool()
    val ext_int  = Bool()
  }

  class cp0_intr extends Bundle {
    val intr    = Bool()
    val branch  = Bool()
    val addrrd  = Bool()
    val addrwt  = Bool()
    val exceed  = Bool()
    val syscall = Bool()
    val breakpt = Bool()
    val noinst  = Bool()
    val eret    = Bool()
    val soft_int= Bool()
    val ext_int = Bool()
  }
  val io = IO(new Bundle {
    val in       = Input(new cp0_in())
    val out      = Output(new cp0_out())
    val intr     = Input(new cp0_intr())
  })

  val cp0_seq = VecInit(Seq.fill(32)(0.U(32.W)))
  cp0_seq(12) := Cat(0x0040.U(16.W), 0.U(16.W)) //cp0_cause
  val cp0 = RegInit(cp0_seq)

  val intr     = io.intr.intr
  val clock_count = RegInit(0.U(1.W))

  clock_count := ~clock_count // clock / 2

  cp0(cause) := Cat(
    Mux(intr,Mux((~cp0(status)(1)) & io.intr.branch, 1.U(1.W), 0.U(1.W)), cp0(cause)(31)), //BD
    0.U(1.W), // TI
    0.U(14.W),
    Mux(io.in.write & io.in.addr === cause, io.in.data(15, 8), Cat(io.in.int | cp0(cause)(15,10), cp0(cause)(9,8))), // IP7_0
    0.U(1.W),
    Mux(io.intr.ext_int | io.intr.soft_int, 0x00.U(5.W), 
      Mux(io.intr.addrrd, 0x04.U(5.W),
        Mux(io.intr.addrwt, 0x05.U(5.W),
          Mux(io.intr.exceed, 0x0c.U(5.W),
            Mux(io.intr.syscall, 0x08.U(5.W),
              Mux(io.intr.breakpt, 0x09.U(5.W),
                Mux(io.intr.noinst, 0x0a.U(5.W),
                  cp0(cause)(6,2)
                )
              )
            )
          )
        )
      )
    ),
    0.U(2.W)
  )

  cp0(badvaddr) := Mux(io.intr.addrrd | io.intr.addrwt, io.in.vaddr, cp0(badvaddr))

  cp0(count) := Mux(io.in.write & io.in.addr === count, io.in.data, Mux(clock_count.asBool(), cp0(count) + 1.U, cp0(count)))

  cp0(status) := Cat(
    0.U(9.W),
    1.U(1.W), // bev
    0.U(6.W),
    Mux(io.in.write & io.in.addr === status, io.in.data(15, 8), cp0(status)(15,8)),
    0.U(6.W),
    Mux(io.in.write & io.in.addr === status, io.in.data(1), // EXL
      Mux(intr, 1.U(1.W), 
        Mux(io.intr.eret, 0.U(1.W),
          cp0(status)(1)
        )
      )
    ),
    Mux(io.in.write & io.in.addr === status, io.in.data(0),
      cp0(status)(0)
    )
  )

  cp0(epc) := Mux(io.in.write & io.in.addr === epc, io.in.data,
    Mux(intr, Mux(cp0(status)(1), cp0(epc), io.in.epc), 
      cp0(epc)
    )
  )

  // when (io.in.write) {
  //   cp0(io.in.addr) := MuxLookup(io.in.addr, cp0(io.in.addr), Array(
  //     badvaddr -> cp0(io.in.addr),
  //     count    -> io.in.data,
  //     status   -> Cat(cp0(io.in.addr)(31, 16), io.in.data(15, 8), cp0(io.in.addr)(7, 2), io.in.data(1, 0)),
  //     cause    -> Cat(cp0(io.in.addr)(31, 16), cp0(io.in.addr)(15, 10), io.in.data(9, 8), cp0(io.in.addr)(7, 0)),
  //     epc      -> io.in.data
  //   ))
  // }

  // interrupt or exception

  // when (intr) {
  //   cp0(epc)    := Mux(cp0(status)(1), cp0(epc), io.in.epc)
  //   cp0(cause)  := Cat(Mux(cp0(status)(1) & io.intr.branch, 1.U(1.W), 0.U(1.W)), cp0(cause)(30, 0))
  //   cp0(status) := Cat(cp0(status)(31, 2), 1.U(1.W), cp0(status)(0))
  // }
  // when (io.intr.ext_int | io.intr.soft_int) {
  //   cp0(cause) := Cat(cp0(cause)(31, 7), 0x00.U(5.W), cp0(cause)(1, 0))
  // }
  // when (io.intr.addrrd) {
  //   cp0(cause)    := Cat(cp0(cause)(31, 7), 0x04.U(5.W), cp0(cause)(1, 0))
  //   cp0(badvaddr) := io.in.vaddr
  // }
  // when (io.intr.addrwt) {
  //   cp0(cause)    := Cat(cp0(cause)(31, 7), 0x05.U(5.W), cp0(cause)(1, 0))
  //   cp0(badvaddr) := io.in.vaddr
  // }
  // when (io.intr.exceed) {
  //   cp0(cause) := Cat(cp0(cause)(31, 7), 0x0c.U(5.W), cp0(cause)(1, 0))
  // }
  // when (io.intr.syscall) {
  //   cp0(cause) := Cat(cp0(cause)(31, 7), 0x08.U(5.W), cp0(cause)(1, 0))
  // }
  // when (io.intr.breakpt) {
  //   cp0(cause) := Cat(cp0(cause)(31, 7), 0x09.U(5.W), cp0(cause)(1, 0))
  // }
  // when (io.intr.noinst) {
  //   cp0(cause) := Cat(cp0(cause)(31, 7), 0x0a.U(5.W), cp0(cause)(1, 0))
  // }
  // when (io.intr.eret) {
  //   cp0(status) := Cat(cp0(status)(31, 2), 0.U(1.W), cp0(status)(0))
  // }

  io.out.data     := cp0(io.in.addr)
  io.out.epc      := cp0(epc)
  io.out.soft_int := (cp0(cause)(9, 8) & cp0(status)(9,8)).orR
  io.out.ext_int  := (cp0(cause)(15, 10) & cp0(status)(15,10)).orR
  io.out.exl      := cp0(status)(1)
  io.out.ie       := cp0(status)(0)
}