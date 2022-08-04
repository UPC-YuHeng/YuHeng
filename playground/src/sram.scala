import chisel3._
import chisel3.util._

class sram_128_21 extends HasBlackBoxInline {
  val io = IO(new Bundle {
    val clka  = Input(Clock())
    // val rst   = Input(Reset())
    val ena   = Input(Bool())
    val wea   = Input(Bool())
    val addra = Input(UInt(7.W))
    val dina  = Input(UInt(21.W))
    val douta = Output(UInt(21.W))
  })
  setInline("sram_128_21.v",
    s"""
    |module sram_128_21
    |#(parameter D_WIDTH = 21, A_WIDTH = 7)
    |(
    |  input clka,
    |  input rst,
    |  input ena,
    |  input wea,
    |  input [A_WIDTH-1 :0] addra,
    |  input [D_WIDTH-1 :0] dina,
    |  output reg [D_WIDTH-1 :0] douta
    |);
    | 
    |reg [D_WIDTH-1: 0] mem [2**A_WIDTH-1:0];
    |
    |integer i;
    |
    |always @(posedge clk)
    |  begin
    |    if(rst)
    |      begin
    |        for(i = 0; i < 2**A_WIDTH; i = i + 1)
    |            mem[i] <= {D_WIDTH{1'b0}};
    |      end
    |    else if(ena & wea)
    |        mem[addr] <= dina;
    |  end
    |
    |always @(posedge clk)
    |  begin
    |    if(ena & (~wea))
    |      douta <= mem[addr];
    |    else
    |      douta <= {D_WIDTH {1'0}};
    |  end
    |endmodule
    """.stripMargin)
}

class sram_128_32 extends HasBlackBoxInline {
  val io = IO(new Bundle {
    val clka  = Input(Clock())
    // val rst   = Input(Reset())
    val ena   = Input(Bool())
    val wea   = Input(Bool())
    val addra = Input(UInt(7.W))
    val dina  = Input(UInt(32.W))
    val douta = Output(UInt(32.W))
  })
  setInline("sram_128_32.v",
    s"""
    |module sram_128_32
    |#(parameter D_WIDTH = 32, A_WIDTH = 7)
    |(
    |  input clka,
    |  input rst,
    |  input ena,
    |  input wea,
    |  input [A_WIDTH-1 :0] addra,
    |  input [D_WIDTH-1 :0] dina,
    |  output reg [D_WIDTH-1 :0] douta
    |);
    | 
    |reg [D_WIDTH-1: 0] mem [2**A_WIDTH-1:0];
    |
    |integer i;
    |
    |always @(posedge clk)
    |  begin
    |    if(rst)
    |      begin
    |        for(i = 0; i < 2**A_WIDTH; i = i + 1)
    |            mem[i] <= {D_WIDTH{1'b0}};
    |      end
    |    else if(ena & wea)
    |        mem[addr] <= dina;
    |  end
    |
    |always @(posedge clk)
    |  begin
    |    if(ena & (~wea))
    |      douta <= mem[addr];
    |    else
    |      douta <= {D_WIDTH {1'0}};
    |  end
    |endmodule
    """.stripMargin)
}

