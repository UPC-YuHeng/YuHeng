import chisel3._
import chisel3.util._

import CP0RegisterList._

class cp0 extends Module {
  val io = IO(new Bundle {
    val in     = Input (new intr_cp0info())
    val out    = Output(new intr_cp0data())
    val contr  = Input (new cp0_contr())
    val intr   = Input (new cp0_intr())
    val status = Output(new cp0_status())
  })

  // init value
  val cp0_init = VecInit(Seq.fill(32)(0.U(32.W)))
  cp0_init(badvaddr) := 0.U
  cp0_init(count)    := 0.U
  cp0_init(status)   := 0x00400000.U
  cp0_init(cause)    := 0.U
  cp0_init(epc)      := 0.U

  val cp0 = RegInit(cp0_init)
  val counter = RegInit(0.U(1.W))

  // defines
  val status_exl = cp0(status)(1)
  val status_ie  = cp0(status)(0)

  val sint = (cp0(cause)(9, 8) & cp0(status)(9, 8)).orR
  val eint = (cp0(cause)(15, 10) & cp0(status)(15, 10)).orR

  counter := ~counter // clock / 2

  cp0(badvaddr) := Mux(io.intr.addrrd | io.intr.addrwt,
    io.intr.vaddr,
    cp0(badvaddr)
  )

  cp0(count) := Mux(io.contr.write & io.in.rd === count,
    io.in.data,
    Mux(counter.asBool(), cp0(count) + 1.U, cp0(count))
  )

  cp0(status) := Cat(
    0.U(9.W),
    1.U(1.W),
    0.U(6.W),
    Mux(io.contr.write & io.in.rd === status,
      io.in.data(15, 8),
      cp0(status)(15,8)
    ),
    0.U(6.W),
    Mux(io.contr.write & io.in.rd === status,
      io.in.data(1),
      MuxCase(cp0(status)(1), Array(
        io.intr.intr -> 1.U(1.W),
        io.intr.eret -> 0.U(1.W)
      )),
    ),
    Mux(io.contr.write & io.in.rd === status,
      io.in.data(0),
      cp0(status)(0)
    )
  )

  cp0(cause) := Cat(
    Mux(io.intr.intr,
      Mux(~status_exl & io.intr.branch, 
        1.U(1.W),
        0.U(1.W)
      ),
      cp0(cause)(31)
    ),
    0.U(1.W),
    0.U(14.W),
    Mux(io.contr.write & io.in.rd === cause,
      io.in.data(15, 8),
      Cat(io.intr.eint | cp0(cause)(15, 10), cp0(cause)(9, 8))
    ),
    0.U(1.W),
    MuxCase(cp0(cause)(6, 2), Array(
      eint             -> 0x00.U(5.W),
      sint             -> 0x00.U(5.W),
      io.intr.addrrd   -> 0x04.U(5.W),
      io.intr.addrwt   -> 0x05.U(5.W),
      io.intr.syscall  -> 0x08.U(5.W),
      io.intr.breakpt  -> 0x09.U(5.W),
      io.intr.reserved -> 0x0a.U(5.W),
      io.intr.exceed   -> 0x0c.U(5.W)
    )),
    0.U(2.W)
  )

  cp0(epc) := Mux(io.contr.write & io.in.rd === epc,
    io.in.data,
    Mux(~status_exl & io.intr.intr,
      io.intr.epc, 
      cp0(epc)
    )
  )

  io.out.data        := cp0(io.in.rd)
  io.status.epc      := cp0(epc)
  io.status.exl      := cp0(status)(1)
  io.status.ie       := cp0(status)(0)
  io.status.cp0_intr := sint | eint
}