import chisel3._
import chisel3.util._

class tlb extends Module {
  class tlb_in extends Bundle {
    val tlb_p0  = new tlb_p()
    val tlb_p1  = new tlb_p()
    val tlb_r   = new tlb_r()
    val tlb_w   = new tlb_w()
    val tlb_rw  = new tlb_rw()
  }
  class tlb_out extends Bundle {
    val tlb_pout0 = new tlb_pout()
    val tlb_pout1 = new tlb_pout()
    val tlb_rw    = new tlb_rw()
  }
  val io = IO(new Bundle {
    val in       = Input (new tlb_in() )
    val out      = Output(new tlb_out())
    val ifu_intr = Output(new tlb_intr())
    val mem_intr = Output(new tlb_intr())
  })
  val tlb_regs = RegInit(VecInit(Seq.fill(32)(0.U(78.W))))
  
  // tlb_p
  io.out.tlb_pout0 := RegInit(Reg(new tlb_pout()))

  for(i <- 0 to 31) {
    when(tlb_regs(i)(77, 59) === io.in.tlb_p0.vpn2 & (tlb_regs(i)(50) | tlb_regs(i)(58, 51) === io.in.tlb_p0.asid)) {
      io.out.tlb_pout0.found := true.B
      io.out.tlb_pout0.index := i.asUInt()
      when(~io.in.tlb_p0.odd.asBool()) {
        io.out.tlb_pout0.pfn := tlb_regs(i)(49, 30)
        io.out.tlb_pout0.c   := tlb_regs(i)(29, 27)
        io.out.tlb_pout0.d   := tlb_regs(i)(26)
        io.out.tlb_pout0.v   := tlb_regs(i)(25)
      }.otherwise {
        io.out.tlb_pout0.pfn := tlb_regs(i)(24, 5)
        io.out.tlb_pout0.c   := tlb_regs(i)(4, 2)
        io.out.tlb_pout0.d   := tlb_regs(i)(1)
        io.out.tlb_pout0.v   := tlb_regs(i)(0)
      }
    }
  }

  // tlb_p1
  io.out.tlb_pout1 := RegInit(Reg(new tlb_pout()))

  for(i <- 0 to 31) {
    when(tlb_regs(i)(77, 59) === io.in.tlb_p1.vpn2 & (tlb_regs(i)(50) | tlb_regs(i)(58, 51) === io.in.tlb_p1.asid)) {
      io.out.tlb_pout1.found := true.B
      io.out.tlb_pout1.index := i.asUInt()
      when(~io.in.tlb_p1.odd.asBool()) {
        io.out.tlb_pout1.pfn := tlb_regs(i)(49, 30)
        io.out.tlb_pout1.c   := tlb_regs(i)(29, 27)
        io.out.tlb_pout1.d   := tlb_regs(i)(26)
        io.out.tlb_pout1.v   := tlb_regs(i)(25)
      }.otherwise {
        io.out.tlb_pout1.pfn := tlb_regs(i)(24, 5)
        io.out.tlb_pout1.c   := tlb_regs(i)(4, 2)
        io.out.tlb_pout1.d   := tlb_regs(i)(1)
        io.out.tlb_pout1.v   := tlb_regs(i)(0)
      }
    }
  }

  //tlb_r
  io.out.tlb_rw.vpn2  := tlb_regs(io.in.tlb_r.index)(77, 59)
  io.out.tlb_rw.asid  := tlb_regs(io.in.tlb_r.index)(58, 51)
  io.out.tlb_rw.pfn0  := tlb_regs(io.in.tlb_r.index)(49, 30)
  io.out.tlb_rw.c0    := tlb_regs(io.in.tlb_r.index)(29, 27)
  io.out.tlb_rw.d0    := tlb_regs(io.in.tlb_r.index)(26)
  io.out.tlb_rw.v0    := tlb_regs(io.in.tlb_r.index)(25)
  io.out.tlb_rw.g0    := tlb_regs(io.in.tlb_r.index)(50)
  io.out.tlb_rw.pfn1  := tlb_regs(io.in.tlb_r.index)(24, 5)
  io.out.tlb_rw.c1    := tlb_regs(io.in.tlb_r.index)(4, 2)
  io.out.tlb_rw.d1    := tlb_regs(io.in.tlb_r.index)(1)
  io.out.tlb_rw.v1    := tlb_regs(io.in.tlb_r.index)(0)
  io.out.tlb_rw.g1    := tlb_regs(io.in.tlb_r.index)(50)

  //tlb_w
  when(io.in.tlb_w.we){
    val wdata = Cat(io.in.tlb_rw.vpn2, io.in.tlb_rw.asid, io.in.tlb_rw.g0 & io.in.tlb_rw.g1, io.in.tlb_rw.pfn0, io.in.tlb_rw.c0, io.in.tlb_rw.d0, io.in.tlb_rw.v0, io.in.tlb_rw.pfn1, io.in.tlb_rw.c1, io.in.tlb_rw.d1, io.in.tlb_rw.v1)
    tlb_regs(io.in.tlb_w.index) := wdata
  }
  
  //intr
  val tlbl0 = (io.in.tlb_p0.rd & io.in.tlb_p0.intr_en & (~io.out.tlb_pout0.found | (io.out.tlb_pout0.found & ~io.out.tlb_pout0.v)))
  val tlbl1 = (io.in.tlb_p1.rd & io.in.tlb_p1.intr_en & (~io.out.tlb_pout1.found | (io.out.tlb_pout1.found & ~io.out.tlb_pout1.v))) 
  val tlbs1 = (~io.in.tlb_p1.rd & io.in.tlb_p1.intr_en & (~io.out.tlb_pout1.found | (io.out.tlb_pout1.found & ~io.out.tlb_pout1.v)))
  val tlbd1 = (~io.in.tlb_p1.rd & io.in.tlb_p1.intr_en & io.out.tlb_pout1.found & io.out.tlb_pout1.v & ~io.out.tlb_pout1.d)

  io.ifu_intr.tlbl   := tlbl0
  io.ifu_intr.tlbs   := false.B
  io.ifu_intr.tlbd   := false.B
  io.ifu_intr.refill := (io.in.tlb_p0.intr_en & ~io.out.tlb_pout0.found)
  io.ifu_intr.vaddr  := io.in.tlb_p0.vaddr
  io.ifu_intr.vpn2   := io.in.tlb_p0.vpn2
  io.ifu_intr.intr   := tlbl0

  io.mem_intr.tlbl   := tlbl1
  io.mem_intr.tlbs   := tlbs1
  io.mem_intr.tlbd   := tlbd1
  io.mem_intr.refill := (io.in.tlb_p1.intr_en & ~io.out.tlb_pout1.found)
  io.mem_intr.vaddr  := io.in.tlb_p1.vaddr
  io.mem_intr.vpn2   := io.in.tlb_p1.vpn2
  io.mem_intr.intr   := tlbl1 | tlbs1 | tlbd1
}
