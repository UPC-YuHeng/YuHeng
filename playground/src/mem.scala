import chisel3._
import chisel3.util._

class mem extends Module {
  class buf_data extends Bundle {
    val ready = Bool()
    val valid = Bool()
    val clear = Bool()
    val in    = new mem_info()
    val out   = new reg_info()
    val contr = new mem_contr()
    val intr  = new mem_intr()
  }
  class ram_io extends Bundle {
    val en    = Bool()
    val wen   = UInt(4.W)
    val addr  = UInt(32.W)
    val wdata = UInt(32.W)
  }
  val io = IO(new Bundle {
    val in    = Flipped(Decoupled(new mem_info()))
    val out   = Decoupled(new reg_info())
    val contr = Input (new mem_contr())
    val intr  = Output(new mem_intr())
    val ram   = Output(new ram_io())
    val rdata = Input (UInt(32.W))
    val rdok  = Input (Bool())
    val clear = Input (Bool())
    val ok    = Output(Bool())
  })

  val buf   = RegInit(Reg(new buf_data()))
  val valid = buf.valid & io.out.ready
  io.ok    := valid

  val clear  = buf.clear | io.clear
  buf.clear  := ~valid & clear

  val datard    = Mux(buf.ready,
    buf.intr.datard,
    io.contr.mem_read & MuxLookup(io.contr.mem_mask, false.B, Array(
      2.U -> io.in.bits.dest(0),
      3.U -> io.in.bits.dest(1, 0).orR
    ))
  )
  val datawt    = Mux(buf.ready,
    buf.intr.datawt,
    io.contr.mem_write & MuxLookup(io.contr.mem_mask, false.B, Array(
      2.U -> io.in.bits.dest(0),
      3.U -> io.in.bits.dest(1, 0).orR
    ))
  )
  val intr       = datard | datawt

  buf.ready       := ~valid & (buf.ready | io.in.valid)
  buf.in          := Mux(buf.ready, buf.in, io.in.bits)
  buf.contr       := Mux(buf.ready, buf.contr, io.contr)
  buf.intr.pc     := Mux(buf.ready, buf.intr.pc, io.in.bits.pc)
  buf.intr.datard := datard
  buf.intr.datawt := datawt

  // fetch data
  io.ram.en    := buf.ready & (buf.contr.mem_read | buf.contr.mem_write) & ~buf.valid & ~intr
  io.ram.wen   := Mux(buf.contr.mem_write,
    MuxLookup(buf.contr.mem_mask, 0.U, Array(
      1.U -> MuxLookup(buf.in.dest(1, 0), 0.U, Array(
        "b00".U -> "b0001".U,
        "b01".U -> "b0010".U,
        "b10".U -> "b0100".U,
        "b11".U -> "b1000".U,
      )),
      2.U -> MuxLookup(buf.in.dest(1, 0), 0.U, Array(
        "b00".U -> "b0011".U,
        "b10".U -> "b1100".U
      )),
      3.U -> "b1111".U
    )),
    0.U
  )
  io.ram.addr  := Cat(buf.in.dest(31, 2), 0.U(2.W))
  io.ram.wdata := MuxLookup(buf.contr.mem_mask, 0.U, Array(
    1.U -> Fill(4, buf.in.data( 7, 0)),
    2.U -> Fill(2, buf.in.data(15, 0)),
    3.U -> buf.in.data
  ))
  val rdata = MuxLookup(buf.contr.mem_mask, 0.U, Array(
    1.U -> MuxLookup(buf.in.dest(1, 0), 0.U, Array(
        "b00".U -> Cat(Fill(24, io.rdata( 7) & buf.contr.signed.asUInt()), io.rdata( 7,  0)),
        "b01".U -> Cat(Fill(24, io.rdata(15) & buf.contr.signed.asUInt()), io.rdata(15,  8)),
        "b10".U -> Cat(Fill(24, io.rdata(23) & buf.contr.signed.asUInt()), io.rdata(23, 16)),
        "b11".U -> Cat(Fill(24, io.rdata(31) & buf.contr.signed.asUInt()), io.rdata(31, 24))
    )),
    2.U -> MuxLookup(buf.in.dest(1, 0), 0.U, Array(
        "b00".U -> Cat(Fill(16, io.rdata(15) & buf.contr.signed.asUInt()), io.rdata(15,  0)),
        "b10".U -> Cat(Fill(16, io.rdata(31) & buf.contr.signed.asUInt()), io.rdata(31, 16))
    )),
    3.U -> io.rdata
  ))

  buf.valid    := ~valid & (buf.valid | io.rdok | intr | (buf.ready & ~io.ram.en))
  buf.out.pc   := buf.in.pc
  buf.out.addr := MuxCase(0.U, Array(
    clear     -> 0.U,
    buf.valid -> buf.out.addr,
    buf.ready -> buf.in.rd
  ))
  buf.out.data := MuxCase(0.U, Array(
    clear     -> 0.U,
    buf.valid -> buf.out.data,
    buf.ready -> Mux(buf.contr.mem_read, rdata, buf.in.dest)
  ))
  buf.out.hi   := MuxCase(0.U, Array(
    clear     -> 0.U,
    buf.valid -> buf.out.hi,
    buf.ready -> buf.in.hi
  ))
  buf.out.lo   := MuxCase(0.U, Array(
    clear     -> 0.U,
    buf.valid -> buf.out.lo,
    buf.ready -> buf.in.lo
  ))

  io.out.valid := valid & ~clear
  io.in.ready  := ~buf.ready
  io.out.bits  := Mux(io.out.valid, buf.out, RegInit(Reg(new reg_info())))
  io.intr      := Mux(io.out.valid, buf.intr, RegInit(Reg(new mem_intr())))
}
