import chisel3._
import chisel3.util._

class axi extends Module {
  class data_in extends Bundle{
    val arid    = UInt(4.W)
    val araddr  = UInt(32.W)
    val arvalid = Bool()
    val awid    = UInt(4.W)
    val awaddr  = UInt(32.W)
    val awvalid = Bool()
    val rlen    = UInt(8.W)
    val wdata   = UInt(256.W)
    val wlen    = UInt(8.W)
    val wen     = UInt(4.W)
  }
  class data_out extends Bundle{
    val arready = Bool()
    val awready = Bool()
    val rid     = UInt(4.W)
    val rready  = Bool()
    val rlast   = Bool()
    val rdata   = UInt(32.W)
    val wready  = Bool()
  }
  val io = IO (new Bundle {
    val data_in   = Input(new data_in())
    val data_out  = Output(new data_out())
    val in        = Input(new master_in())
    val out       = Output(new master_out())
  })
  val rbuf = RegInit(Reg(new data_in()))
  val wbuf = RegInit(Reg(new data_in()))
  
  val ridle :: sdraddr :: rddata :: nullr = Enum(4)
  val widle :: sdwaddr :: wrdata :: wrend :: nullw = Enum(5)
  val rstate        = RegInit(ridle)
  val wstate        = RegInit(widle)

  val reg_write_num = RegInit(0.U(8.W))
  val reg_read_num  = RegInit(0.U(8.W))
  val reg_wdata     = RegInit(VecInit(Seq.fill(8)(0.U(32.W))))
 
  io.data_out.arready := (rstate === ridle)
  io.data_out.awready := (wstate === widle)
  io.data_out.rready  := false.B
  io.data_out.wready  := false.B
  io.data_out.rid     := 0.U
  io.data_out.rdata   := 0.U
  io.data_out.rlast   := false.B

  io.out.arid    := 0.U
  io.out.araddr  := 0.U
  io.out.arlen   := 0.U
  io.out.arsize  := 0.U
  io.out.arburst := 0.U
  io.out.arlock  := 0.U
  io.out.arcache := 0.U
  io.out.arprot  := 0.U
  io.out.arvalid := false.B

  io.out.rready  := false.B

  io.out.awid    := 0.U
  io.out.awaddr  := 0.U
  io.out.awlen   := 0.U
  io.out.awsize  := 0.U
  io.out.awburst := 0.U
  io.out.awlock  := 0.U
  io.out.awcache := 0.U
  io.out.awprot  := 0.U
  io.out.awvalid := false.B

  io.out.wid     := 0.U
  io.out.wdata   := 0.U
  io.out.wstrb   := 0.U
  io.out.wlast   := false.B
  io.out.wvalid  := false.B

  io.out.bready  := false.B
  
  switch(rstate){
    is(ridle)
    {
      when(io.data_in.arvalid === true.B)
      { 
        rbuf   := io.data_in
        rstate := sdraddr
      }
    }
    is(sdraddr){
      io.out.arvalid  := true.B
      io.out.araddr   := rbuf.araddr
      io.out.arid     := rbuf.arid
      io.out.arlen    := rbuf.rlen
      when(io.in.arready)
      {
        reg_read_num    := 0.U
        rstate          := rddata 
      }
    }
    is(rddata)
    {
      io.out.rready := true.B
      when(io.in.rvalid)
      {
        io.data_out.rready := true.B
        io.data_out.rid    := io.in.rid
        io.data_out.rdata  := io.in.rdata
        reg_read_num       := reg_read_num + 1.U
        when(io.in.rlast | reg_read_num === rbuf.rlen)
        {
          io.data_out.rlast := true.B
          rstate := ridle
        }
      }
    }
  }

  switch(wstate)
  {
    is(widle)
    {
      when(io.data_in.awvalid === true.B)
      {
        wbuf := io.data_in
        for(i <- 0 to 7) {
          reg_wdata(i) := io.data_in.wdata(i * 32 + 31, i * 32);
        }
        wstate             := sdwaddr
      }
    }
    is(sdwaddr)
    {
      io.out.awvalid := true.B
      io.out.awaddr  := wbuf.awaddr
      io.out.awid    := wbuf.awid
      io.out.awlen   := wbuf.wlen
      when(io.in.awready)
      {
        reg_write_num   := 0.U
        wstate          := wrdata
      }
    }
    is(wrdata)
    {
      io.out.wvalid := true.B
      io.out.wlast  := (reg_write_num === wbuf.wlen)
      io.out.wdata  := reg_wdata(reg_write_num)
      io.out.wid    := wbuf.awid
      io.out.wstrb  := wbuf.wen
      when(io.in.wready)
      {
        when(reg_write_num === wbuf.wlen) {
          wstate       := wrend
        }
        reg_write_num  := reg_write_num + 1.U
      }
    }
    is(wrend)
    {
      io.out.bready := true.B
      when(io.in.bvalid)
      {
        io.data_out.wready := true.B
        wstate             := widle
      }
    }
  }

  io.out.arburst := "b01".U
  io.out.arlock  := "b00".U
  io.out.arcache := "b0000".U
  io.out.arprot  := "b000".U
  io.out.arsize  := "b010".U

  io.out.awburst := "b01".U
  io.out.awlock  := "b00".U
  io.out.awcache := "b0000".U
  io.out.awprot  := "b000".U
  io.out.awsize  := "b010".U
}