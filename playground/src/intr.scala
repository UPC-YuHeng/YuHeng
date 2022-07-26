import chisel3._
import chisel3.util._

class intr extends Module {
  class delay_data extends Bundle {
    // intr
    val intr      = Bool()
    val branch    = Bool()
    val pc        = UInt(32.W)
    val vaddr     = UInt(32.W)
    // ifu
    val instrd    = Bool()
    // idu
    val syscall   = Bool()
    val breakpt   = Bool()
    val reserved  = Bool()
    val eret      = Bool()
    // exu
    val exceed    = Bool()
    // mem
    val datard    = Bool()
    val datawt    = Bool()
    // cp0
    val addr      = UInt(5.W)
    val sel       = UInt(4.W)
    val cp0_read  = Bool()
    val cp0_write = Bool()
  }
  class valid_data extends Bundle {
    val ifu_idu = Bool()
    val idu_exu = Bool()
    val exu_mem = Bool()
  }
  val io = IO(new Bundle {
    val eint   = Input (UInt(6.W))
    val amu    = Output(new amu_intr())
    val ifu    = Input (new ifu_intr())
    val idu    = Input (new idu_intr())
    val exu    = Input (new exu_intr())
    val mem    = Input (new mem_intr())
    val reg    = Output(new reg_intr())
    val regin  = Output(new intr_reginfo())
    val regout = Input (new intr_regdata())
    val valid  = Input (new valid_data())
    val branch = Input (Bool())
    val lock   = Input (Bool())
    val intr   = Output(Bool())
  })

  val cp0 = Module(new cp0())

  val intr_ifu = io.ifu.instrd
  val intr_idu = io.idu.syscall | io.idu.breakpt | io.idu.reserved
  val intr_exu = io.exu.exceed
  val intr_mem = io.mem.datard | io.mem.datawt

  val ifu    = RegInit(Reg(new delay_data()))
  val idu    = RegInit(Reg(new delay_data()))
  val exu    = RegInit(Reg(new delay_data()))
  val mem    = RegInit(Reg(new delay_data()))

  /******************** delay ********************/
  // ifu
  ifu.intr      := intr_ifu
  ifu.branch    := io.branch
  ifu.pc        := MuxCase(ifu.pc, Array(
    intr_ifu         -> io.ifu.pc
  ))
  ifu.vaddr     := io.ifu.vaddr
  ifu.instrd    := io.ifu.instrd
  // idu
  idu.intr      := Mux(io.valid.ifu_idu, ifu.intr, idu.intr) | intr_idu
  idu.branch    := Mux(io.valid.ifu_idu, ifu.branch, idu.branch)
  idu.pc        := MuxCase(idu.pc, Array(
    intr_idu         -> io.idu.pc,
    io.valid.ifu_idu -> ifu.pc
  ))
  idu.vaddr     := Mux(io.valid.ifu_idu, ifu.vaddr, idu.vaddr)
  idu.instrd    := Mux(io.valid.ifu_idu, ifu.instrd, idu.instrd)
  idu.syscall   := io.idu.syscall
  idu.breakpt   := io.idu.breakpt
  idu.reserved  := io.idu.reserved
  idu.eret      := io.idu.eret
  idu.addr      := io.idu.addr
  idu.sel       := io.idu.sel
  idu.cp0_read  := io.idu.cp0_read
  idu.cp0_write := io.idu.cp0_write
  // exu
  exu.intr      := Mux(io.lock, false.B, Mux(io.valid.idu_exu, idu.intr, exu.intr) | intr_exu)
  exu.branch    := Mux(io.lock, false.B, Mux(io.valid.idu_exu, idu.branch, exu.branch))
  exu.pc        := MuxCase(exu.pc, Array(
    io.lock          -> 0.U,
    intr_exu         -> io.exu.pc,
    io.valid.idu_exu -> idu.pc
  ))
  exu.vaddr     := Mux(io.lock, 0.U, Mux(io.valid.idu_exu, idu.vaddr, exu.vaddr))
  exu.instrd    := Mux(io.lock, false.B, Mux(io.valid.idu_exu, idu.instrd, exu.instrd))
  exu.syscall   := Mux(io.lock, false.B, Mux(io.valid.idu_exu, idu.syscall, exu.syscall))
  exu.breakpt   := Mux(io.lock, false.B, Mux(io.valid.idu_exu, idu.breakpt, exu.breakpt))
  exu.reserved  := Mux(io.lock, false.B, Mux(io.valid.idu_exu, idu.reserved, exu.reserved))
  exu.eret      := Mux(io.lock, false.B, Mux(io.valid.idu_exu, idu.eret, exu.eret))
  exu.exceed    := Mux(io.lock, false.B, io.exu.exceed)
  exu.addr      := Mux(io.lock, 0.U, Mux(io.valid.idu_exu, idu.addr, exu.addr))
  exu.sel       := Mux(io.lock, 0.U, Mux(io.valid.idu_exu, idu.sel, exu.sel))
  exu.cp0_read  := Mux(io.lock, false.B, Mux(io.valid.idu_exu, idu.cp0_read, exu.cp0_read))
  exu.cp0_write := Mux(io.lock, false.B, Mux(io.valid.idu_exu, idu.cp0_write, exu.cp0_write))
  // mem
  mem.intr      := Mux(io.valid.exu_mem, exu.intr, mem.intr) | intr_mem
  mem.branch    := Mux(io.valid.exu_mem, exu.branch, mem.branch)
  mem.pc        := MuxCase(mem.pc, Array(
    intr_mem         -> io.mem.pc,
    io.valid.exu_mem -> exu.pc
  ))
  mem.vaddr     := MuxCase(mem.vaddr, Array(
    io.mem.datard    -> io.mem.vaddr,
    io.mem.datawt    -> io.mem.vaddr,
    io.valid.exu_mem -> exu.vaddr
  ))
  mem.instrd    := Mux(io.valid.exu_mem, exu.instrd, mem.instrd)
  mem.syscall   := Mux(io.valid.exu_mem, exu.syscall, mem.syscall)
  mem.breakpt   := Mux(io.valid.exu_mem, exu.breakpt, mem.breakpt)
  mem.reserved  := Mux(io.valid.exu_mem, exu.reserved, mem.reserved)
  mem.eret      := Mux(io.valid.exu_mem, exu.eret, mem.eret)
  mem.exceed    := Mux(io.valid.exu_mem, exu.exceed, mem.exceed)
  mem.datard    := io.mem.datard
  mem.datawt    := io.mem.datawt
  mem.addr      := Mux(io.valid.exu_mem, exu.addr, mem.addr)
  mem.sel       := Mux(io.valid.exu_mem, exu.sel, mem.sel)
  mem.cp0_read  := Mux(io.valid.exu_mem, exu.cp0_read, mem.cp0_read)
  mem.cp0_write := Mux(io.valid.exu_mem, exu.cp0_write, mem.cp0_write)

  /******************** ports ********************/
  io.intr := (mem.intr | cp0.io.status.cp0_intr) & (~cp0.io.status.exl) & cp0.io.status.ie

  // amu
  io.amu.intr  := (mem.intr | cp0.io.status.cp0_intr) & (~cp0.io.status.exl) & cp0.io.status.ie
  io.amu.eret  := io.idu.eret
  io.amu.eaddr := cp0.io.status.epc

  // reg (read)
  io.regin.rt   := mem.addr       // ret.rt

  // cp0
  cp0.io.in.rd       := mem.addr     // cp0.rd(sel)
  cp0.io.in.sel      := mem.sel
  cp0.io.in.data     := io.regout.data
  cp0.io.contr.write := mem.cp0_write

  // reg (write)
  io.reg.cp0_read := mem.cp0_read
  io.reg.cp0_data := cp0.io.out.data

  // cp0
  cp0.io.intr.intr     := io.intr
  cp0.io.intr.eint     := io.eint
  cp0.io.intr.branch   := mem.branch
  cp0.io.intr.addrrd   := mem.instrd | mem.datard
  cp0.io.intr.addrwt   := mem.datawt
  cp0.io.intr.exceed   := mem.exceed
  cp0.io.intr.syscall  := mem.syscall
  cp0.io.intr.breakpt  := mem.breakpt
  cp0.io.intr.reserved := mem.reserved
  cp0.io.intr.eret     := mem.eret
  cp0.io.intr.epc      := mem.pc
  cp0.io.intr.vaddr    := mem.vaddr
}
