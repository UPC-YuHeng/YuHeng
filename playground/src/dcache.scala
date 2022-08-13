import chisel3._
import chisel3.util._

class dcache extends Module {

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

  val idle :: lookup :: miss :: replace :: refill :: refill_end :: pass :: pass_wait :: nulls = Enum(9)
  
  val cstate       = RegInit(idle)
  val rbuf         = RegInit(Reg(new cpu_in()))
  val reg_paddr    = RegInit(0.U(32.W))
  val reg_rbuf_num = RegInit(0.U(4.W))

  val reg_rdata    = RegInit(0.U(32.W))

  val tag0 = RegInit(0.U(21.W))
  val tag1 = RegInit(0.U(21.W))
  
  tagv0.clka   := clock
  // tagv0.rst    := reset
  tagv0.ena    := false.B
  tagv0.wea    := false.B
  tagv0.addra  := 0.U
  tagv0.dina   := 0.U

  tagv1.clka   := clock
  // tagv1.rst    := reset
  tagv1.ena    := false.B
  tagv1.wea    := false.B
  tagv1.addra  := 0.U
  tagv1.dina   := 0.U


  for(i <- 0 to 7) {
    way0_bank(i).clka   := clock
    // way0_bank(i).rst    := reset
    way0_bank(i).ena    := false.B
    way0_bank(i).wea    := false.B
    way0_bank(i).addra  := 0.U
    way0_bank(i).dina   := 0.U

    way1_bank(i).clka   := clock
    // way1_bank(i).rst    := reset
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
  io.aout.rd_size  := 2.U
  io.aout.wr_req   := false.B
  io.aout.wr_len   := 0.U
  io.aout.wr_addr  := 0.U
  io.aout.wr_wstrb := 0.U
  io.aout.wr_data  := 0.U

  switch(cstate){
    is(idle){
      when(io.cin.valid) {
        rbuf := io.cin
        when(~io.cin.ucache) {
          tagv0.addra := io.cin.index
          tagv1.addra := io.cin.index
          tagv0.ena   := true.B
          tagv1.ena   := true.B
          tagv0.wea   := false.B
          tagv1.wea   := false.B

          way0_bank(io.cin.offset(4,2)).addra := io.cin.index
          way1_bank(io.cin.offset(4,2)).addra := io.cin.index
          way0_bank(io.cin.offset(4,2)).ena   := true.B
          way1_bank(io.cin.offset(4,2)).ena   := true.B
          way0_bank(io.cin.offset(4,2)).wea   := false.B
          way1_bank(io.cin.offset(4,2)).wea   := false.B
          cstate := lookup
        }.otherwise {
          cstate := pass
        }
        reg_paddr := Cat(io.cin.tag, io.cin.index, io.cin.offset)
        io.cout.addr_ok := true.B
      }
    }
    is(lookup){
      tag0 := tagv0.douta
      tag1 := tagv1.douta
      val data0 = way0_bank(rbuf.offset(4,2)).douta
      val data1 = way1_bank(rbuf.offset(4,2)).douta

      val ptag     = Cat(reg_paddr(31, 12), "b1".U(1.W))
      val hit_tag0 = (ptag === tagv0.douta)
      val hit_tag1 = (ptag === tagv1.douta)
      val hit_tag  = (hit_tag0 | hit_tag1)
    
      when(~hit_tag) {
        val req = Mux(lru(rbuf.index).asBool(), dir1(rbuf.index), dir0(rbuf.index))
        when(req.asBool()) {
          for(i <- 0 to 7) {
            way0_bank(i).addra := rbuf.index
            way0_bank(i).ena   := true.B
            way0_bank(i).wea   := false.B
            way1_bank(i).addra := rbuf.index
            way1_bank(i).ena   := true.B
            way1_bank(i).wea   := false.B
          }
          cstate := miss
        }.otherwise {
          cstate := replace
        }
      }.otherwise {
        lru(rbuf.index)   := Mux(hit_tag0, 1.U, 0.U)
        when(~rbuf.op) {
          val rdata = MuxLookup(reg_paddr(1, 0), 0.U, Array(
            0.U -> Mux(hit_tag0, data0, data1)(31, 0),
            1.U -> Mux(hit_tag0, data0, data1)(31, 8),
            2.U -> Mux(hit_tag0, data0, data1)(31, 16),
            3.U -> Mux(hit_tag0, data0, data1)(31, 24)
          ))
          io.cout.rdata   := rdata
          io.cout.data_ok := true.B
          cstate          := idle
        }.otherwise{
          when(hit_tag0) {
            val data_w0 = Cat(Mux(rbuf.wstrb(3), rbuf.wdata(31, 24), data0(31, 24)),
              Mux(rbuf.wstrb(2), rbuf.wdata(23, 16), data0(23, 16)),
              Mux(rbuf.wstrb(1), rbuf.wdata(15, 8),  data0(15, 8)),
              Mux(rbuf.wstrb(0), rbuf.wdata(7, 0),   data0(7, 0))
            )
            
            way0_bank(rbuf.offset(4,2)).addra := rbuf.index
            way0_bank(rbuf.offset(4,2)).ena   := true.B
            way0_bank(rbuf.offset(4,2)).wea   := true.B
            way0_bank(rbuf.offset(4,2)).dina  := data_w0

            dir0(rbuf.index) := 1.U
          }.otherwise {
            val data_w1 = Cat(Mux(rbuf.wstrb(3), rbuf.wdata(31, 24), data1(31, 24)),
              Mux(rbuf.wstrb(2), rbuf.wdata(23, 16), data1(23, 16)),
              Mux(rbuf.wstrb(1), rbuf.wdata(15, 8),  data1(15, 8)),
              Mux(rbuf.wstrb(0), rbuf.wdata(7, 0),   data1(7, 0))
            )

            way1_bank(rbuf.offset(4,2)).addra := rbuf.index
            way1_bank(rbuf.offset(4,2)).ena   := true.B
            way1_bank(rbuf.offset(4,2)).wea   := true.B
            way1_bank(rbuf.offset(4,2)).dina  := data_w1

            dir1(rbuf.index) := 1.U
          }
          io.cout.data_ok := true.B
          cstate := idle
        }
      }
    }
    is(miss)
    {
      val data_out0 = Cat(way0_bank(7).douta, way0_bank(6).douta, way0_bank(5).douta, way0_bank(4).douta, way0_bank(3).douta, way0_bank(2).douta, way0_bank(1).douta, way0_bank(0).douta)
      val data_out1 = Cat(way1_bank(7).douta, way1_bank(6).douta, way1_bank(5).douta, way1_bank(4).douta, way1_bank(3).douta, way1_bank(2).douta, way1_bank(1).douta, way1_bank(0).douta)
      io.aout.wr_req := true.B
      when(io.ain.wr_rdy) {
        io.aout.wr_len   := 7.U
        io.aout.wr_wstrb := "hf".U
        io.aout.wr_addr  := Cat(Mux(lru(rbuf.index).asBool(), tag1(20, 1), tag0(20, 1)), rbuf.index, 0.U(5.W))
        io.aout.wr_data  := Mux(lru(rbuf.index).asBool(), data_out1, data_out0)
        cstate := replace
      }
    }
    is(replace) {
      io.aout.rd_req  := true.B
      when(io.ain.rd_rdy) {
        io.aout.rd_addr := Cat(reg_paddr(31, 5), 0.U(5.W))
        io.aout.rd_len  := 7.U
        reg_rbuf_num    := 0.U
        when(lru(rbuf.index) === 0.U) {
          tagv0.addra  := rbuf.index
          tagv0.ena    := true.B
          tagv0.wea    := true.B
          tagv0.dina   := Cat(reg_paddr(31, 12), "b1".U(1.W))
          dir0(rbuf.index) := 0.U
        }.otherwise {
          tagv1.addra  := rbuf.index
          tagv1.ena    := true.B
          tagv1.wea    := true.B
          tagv1.dina   := Cat(reg_paddr(31, 12), "b1".U(1.W))
          dir1(rbuf.index) := 0.U
        }
        cstate         := refill
      }
    }
    is(refill)
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
        when(io.ain.ret_last.asBool()){
          lru(rbuf.index) := ~lru(rbuf.index)
          when(~rbuf.op) {
            cstate := refill_end
          }.otherwise {
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
            cstate := lookup
          }
        }
      }
    }
    is(refill_end)
    {
      val rdata = MuxLookup(reg_paddr(1, 0), 0.U, Array(
        0.U -> reg_rdata(31, 0),
        1.U -> reg_rdata(31, 8),
        2.U -> reg_rdata(31, 16),
        3.U -> reg_rdata(31, 24)
      ))
      io.cout.rdata   := rdata
      io.cout.data_ok := true.B
      cstate := idle
    }
    is(pass) 
    {
      io.aout.rd_req  := ~rbuf.op
      io.aout.wr_req  := rbuf.op
      when((~rbuf.op) & io.ain.rd_rdy) {
        io.aout.rd_addr := reg_paddr
        io.aout.rd_len  := 0.U
        io.aout.rd_size := rbuf.rsize
        cstate          := pass_wait
      }
      when(rbuf.op & io.ain.wr_rdy) {
        io.aout.wr_addr  := reg_paddr
        io.aout.wr_len   := 0.U
        io.aout.wr_data  := Cat(0.U(224.W), rbuf.wdata)
        io.aout.wr_wstrb := rbuf.wstrb
        cstate           := pass_wait
      }
    }
    is(pass_wait)
    {
      when((~rbuf.op) & io.ain.rd_valid) {
        io.cout.data_ok := true.B
        io.cout.rdata   := MuxLookup(reg_paddr(1, 0), 0.U, Array(
          0.U -> io.ain.ret_rdata,
          1.U -> io.ain.ret_rdata(31, 8),
          2.U -> io.ain.ret_rdata(31, 16),
          3.U -> io.ain.ret_rdata(31, 24)
        ))
        cstate          := idle
      }
      when(rbuf.op & io.ain.wr_valid) {
        io.cout.data_ok := true.B
        cstate          := idle
      }
    }
  }
}