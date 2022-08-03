import chisel3._
import chisel3.util._

class icache extends Module {
  class cpu_in extends Bundle{
    val ucache  =  Bool()
    val valid   =  Bool()
    val op      =  Bool()
    val index   =  UInt(7.W)
    val tag     =  UInt(20.W)
    val offset  =  UInt(5.W)
    val wstrb   =  UInt(4.W)
    val wdata   =  UInt(32.W)
  }
  class cpu_out extends Bundle{
    val addr_ok =  Bool()
    val data_ok =  Bool()
    val rdata   =  UInt(32.W)
  }

  val io = IO(new Bundle{
    val cin      =  Input(new cpu_in())
    val cout     =  Output(new cpu_out())
    val ain      =  Input(new axi_in())
    val aout     =  Output(new axi_out())
  })

  val tagv0 = Module(new sram_128_21()).io
  val tagv1 = Module(new sram_128_21()).io
  val dir0  = RegInit(VecInit(Seq.fill(128)(0.U(1.W))))
  val dir1  = RegInit(VecInit(Seq.fill(128)(0.U(1.W))))
  val lru   = RegInit(VecInit(Seq.fill(128)(0.U(1.W))))
  val way0_bank = VecInit(Seq.fill(8)(Module(new sram_128_32()).io))
  val way1_bank = VecInit(Seq.fill(8)(Module(new sram_128_32()).io))

  val idle :: lookup1 :: lookup2 :: miss :: replace :: refill :: pass :: pass_wait :: nulls = Enum(10)
  val cstate       = RegInit(idle)
  val rbuf         = RegInit(Reg(new cpu_in))
  val reg_paddr    = RegInit(0.U(32.W))
  val reg_rbuf_num = RegInit(0.U(8.W))
  val reg_rdata    = RegInit(0.U(32.W))

  tagv0.clka   := clock
  tagv0.ena    := false.B
  tagv0.wea    := false.B
  tagv0.addra  := 0.U
  tagv0.dina   := 0.U

  tagv1.clka   := clock
  tagv1.ena    := false.B
  tagv1.wea    := false.B
  tagv1.addra  := 0.U
  tagv1.dina   := 0.U

  for(i <- 0 to 7) {
    way0_bank(i).clka   := clock
    way0_bank(i).ena    := false.B
    way0_bank(i).wea    := false.B
    way0_bank(i).addra  := 0.U
    way0_bank(i).dina   := 0.U

    way1_bank(i).clka   := clock
    way1_bank(i).ena    := false.B
    way1_bank(i).wea    := false.B
    way1_bank(i).addra  := 0.U
    way1_bank(i).dina   := 0.U
  }

  io.cout.addr_ok := false.B
  io.cout.data_ok := false.B
  io.cout.rdata   := 0.U

  io.aout.rd_req   := false.B
  io.aout.rd_len   := 0.U
  io.aout.rd_addr  := 0.U
  io.aout.wr_req   := false.B
  io.aout.wr_len   := 0.U
  io.aout.wr_addr  := 0.U
  io.aout.wr_wstrb := 0.U
  io.aout.wr_data  := 0.U

  switch(cstate){
    is(idle){
      when(io.cin.valid & ~io.cin.op){
        rbuf     := io.cin
        when(~io.cin.ucache) {
          cstate := lookup1
        }.otherwise {
          cstate := pass
        }
        reg_paddr :=  Cat(io.cin.tag, io.cin.index, io.cin.offset)
        io.cout.addr_ok := true.B
      }
    }
    is(lookup1){
      tagv0.addra := rbuf.index
      tagv1.addra := rbuf.index
      tagv0.ena   := true.B
      tagv1.ena   := true.B
      tagv0.wea   := false.B
      tagv1.wea   := false.B

      way0_bank(rbuf.offset(4,2)).addra := rbuf.index
      way1_bank(rbuf.offset(4,2)).addra := rbuf.index
      way0_bank(rbuf.offset(4,2)).ena   := true.B
      way1_bank(rbuf.offset(4,2)).ena   := true.B
      way0_bank(rbuf.offset(4,2)).wea   := false.B
      way1_bank(rbuf.offset(4,2)).wea   := false.B

      cstate := lookup2
    }
    is(lookup2){
      val data0 = way0_bank(rbuf.offset(4,2)).douta
      val data1 = way1_bank(rbuf.offset(4,2)).douta

      val ptag     = Cat(reg_paddr(31, 12), "b1".U(1.W))
      val hit_tag0 = (ptag === tagv0.douta)
      val hit_tag1 = (ptag === tagv1.douta)
      val hit_tag  = (hit_tag0 | hit_tag1)
    
      when(~hit_tag){
        cstate := miss
      }.otherwise{
        lru(rbuf.index) := Mux(hit_tag0, 1.U, 0.U)
        io.cout.rdata   := Mux(hit_tag0, data0, data1)
        io.cout.data_ok := true.B
        cstate          := idle
      }
    }
    is(miss){
      io.aout.rd_req  := true.B
      when(io.ain.rd_rdy)
      {
        io.aout.rd_addr := Cat(reg_paddr(31, 5), 0.U(5.W))
        io.aout.rd_len  := 7.U
        reg_rbuf_num    := 0.U
        when(lru(rbuf.index) === 0.U) {
          tagv0.addra  := rbuf.index
          tagv0.ena    := true.B
          tagv0.wea    := true.B
          tagv0.dina   := Cat(reg_paddr(31, 12), "b1".U(1.W))
        }.otherwise{
          tagv1.addra  := rbuf.index
          tagv1.ena    := true.B
          tagv1.wea    := true.B
          tagv1.dina   := Cat(reg_paddr(31, 12), "b1".U(1.W))
        }
        cstate          := replace
      }
    }
    is(replace)
    {
      when(io.ain.rd_valid) {
        when(lru(rbuf.index) === 0.U){
          way0_bank(reg_rbuf_num).addra  := rbuf.index
          way0_bank(reg_rbuf_num).ena    := true.B
          way0_bank(reg_rbuf_num).wea    := true.B
          way0_bank(reg_rbuf_num).dina   := io.ain.ret_rdata
          when(reg_rbuf_num === rbuf.offset(4,2)) {
            reg_rdata := io.ain.ret_rdata
          }
        }.otherwise{
          way1_bank(reg_rbuf_num).addra  := rbuf.index
          way1_bank(reg_rbuf_num).ena    := true.B
          way1_bank(reg_rbuf_num).wea    := true.B
          way1_bank(reg_rbuf_num).dina   := io.ain.ret_rdata
          when(reg_rbuf_num === rbuf.offset(4,2)) {
            reg_rdata := io.ain.ret_rdata
          }
        }
        reg_rbuf_num := reg_rbuf_num + 1.U
        when(io.ain.ret_last){
          lru(rbuf.index) := ~lru(rbuf.index)
          cstate := refill
        }
      }
    }
    is(refill)
    {
      io.cout.rdata := reg_rdata
      io.cout.data_ok := true.B
      cstate := idle
    }
    is(pass) 
    {
      io.aout.rd_req  := true.B
      when(io.ain.rd_rdy) {
        io.aout.rd_addr := reg_paddr
        io.aout.rd_len  := 0.U
        cstate         := pass_wait
      }
    }
    is(pass_wait)
    {
      when(io.ain.rd_valid) {
        io.cout.data_ok := true.B
        io.cout.rdata   := io.ain.ret_rdata
        cstate          := idle
      }
    }
  }
}