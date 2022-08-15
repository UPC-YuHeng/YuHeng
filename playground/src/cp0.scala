import chisel3._
import chisel3.util._

import CP0RegisterList._

class cp0 extends Module {
  val io = IO(new Bundle {
    val in         = Input (new intr_cp0info())
    val out        = Output(new intr_cp0data())
    val contr      = Input (new cp0_contr())
    val intr       = Input (new cp0_intr())
    val status     = Output(new cp0_status())
    val tlb_contr  = Output(new tlb_contr())
    val tlb_data   = Input (new tlb_data())
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

  cp0(badvaddr) := MuxCase(cp0(badvaddr), Array(
    (io.intr.addrrd | io.intr.addrwt)            -> io.intr.vaddr,
    (io.intr.tlbl | io.intr.tlbs | io.intr.tlbd) -> io.intr.tlb_vaddr
  ))

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
      io.intr.exceed   -> 0x0c.U(5.W),
      io.intr.tlbd     -> 0x01.U(5.W),
      io.intr.tlbl     -> 0x02.U(5.W),
      io.intr.tlbs     -> 0x03.U(5.W)
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

  cp0(entryhi) := MuxCase(cp0(entryhi), Array(
    (io.contr.write & io.in.rd === entryhi)      -> Cat(io.in.data(31, 13), 0.U(5.W), io.in.data(7, 0)),
    io.contr.tlbr                                -> Cat(io.tlb_data.tlb_rw.vpn2, 0.U(5.W), io.tlb_data.tlb_rw.asid),
    (io.intr.tlbl | io.intr.tlbs | io.intr.tlbd) -> Cat(io.intr.tlb_vpn2, 0.U(5.W), cp0(entryhi)(7, 0))
  ))
  
  cp0(entrylo0) := MuxCase(cp0(entrylo0), Array(
    (io.contr.write & io.in.rd === entrylo0) -> Cat(0.U(6.W), io.in.data(25, 0)),
    io.contr.tlbr                            -> Cat(0.U(6.W), io.tlb_data.tlb_rw.pfn0, io.tlb_data.tlb_rw.c0, io.tlb_data.tlb_rw.d0, io.tlb_data.tlb_rw.v0, io.tlb_data.tlb_rw.g0)
  ))

  cp0(entrylo1) := MuxCase(cp0(entrylo1), Array(
    (io.contr.write & io.in.rd === entrylo1) -> Cat(0.U(6.W), io.in.data(25, 0)),
    io.contr.tlbr                            -> Cat(0.U(6.W), io.tlb_data.tlb_rw.pfn1, io.tlb_data.tlb_rw.c1, io.tlb_data.tlb_rw.d1, io.tlb_data.tlb_rw.v1, io.tlb_data.tlb_rw.g1)
  ))

  cp0(index) := MuxCase(cp0(index), Array(
    (io.contr.write & io.in.rd === index) -> Cat(0.U(27.W), io.in.data(4, 0)),
    io.contr.tlbp                         -> Cat(~io.tlb_data.tlb_pout.found.asUInt(), 0.U(26.W), io.tlb_data.tlb_pout.index)
  ))

  io.out.data        := cp0(io.in.rd)
  io.status.epc      := cp0(epc)
  io.status.exl      := cp0(status)(1)
  io.status.ie       := cp0(status)(0)
  io.status.cp0_intr := sint | eint
  io.tlb_contr.tlb_w.we      := io.contr.tlbwi
  io.tlb_contr.tlb_w.index   := cp0(index)(4, 0)
  io.tlb_contr.tlb_r.index   := cp0(index)(4, 0)
  io.tlb_contr.tlb_p.vpn2    := cp0(entryhi)(31, 13)
  io.tlb_contr.tlb_p.odd     := 0.U
  io.tlb_contr.tlb_p.asid    := cp0(entryhi)(7, 0)
  io.tlb_contr.tlb_p.rd      := false.B
  io.tlb_contr.tlb_p.intr_en := false.B
  io.tlb_contr.tlb_p.vaddr   := 0.U
  io.tlb_contr.tlb_rw.vpn2   := cp0(entryhi)(31, 13)
  io.tlb_contr.tlb_rw.asid   := cp0(entryhi)(7, 0)
  io.tlb_contr.tlb_rw.pfn0   := cp0(entrylo0)(25, 6)
  io.tlb_contr.tlb_rw.c0     := cp0(entrylo0)(5, 3)
  io.tlb_contr.tlb_rw.d0     := cp0(entrylo0)(2)
  io.tlb_contr.tlb_rw.v0     := cp0(entrylo0)(1)
  io.tlb_contr.tlb_rw.g0     := cp0(entrylo0)(0)
  io.tlb_contr.tlb_rw.pfn1   := cp0(entrylo1)(25, 6)
  io.tlb_contr.tlb_rw.c1     := cp0(entrylo1)(5, 3)
  io.tlb_contr.tlb_rw.d1     := cp0(entrylo1)(2)
  io.tlb_contr.tlb_rw.v1     := cp0(entrylo1)(1)
  io.tlb_contr.tlb_rw.g1     := cp0(entrylo1)(0)
}