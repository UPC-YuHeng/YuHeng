package yuheng.debug

import chisel3._
import chisel3.util._

class mem extends HasBlackBoxInline {
  val io = IO(new Bundle {
    val ren   = Input(Bool())
    val wen   = Input(Bool())
    val raddr = Input(UInt(32.W))
    val rdata = Output(UInt(32.W))
    val waddr = Input(UInt(32.W))
    val wdata = Input(UInt(32.W))
    val mask  = Input(UInt(4.W))
  })
  setInline("mem.v",
    s"""
    |import "DPI-C" function void pmem_read(
    |  input int raddr, output int rdata);
    |import "DPI-C" function void pmem_write(
    |  input int waddr, input int wdata, input byte mask);
    |module mem(ren, wen, raddr, rdata, waddr, wdata, mask);
    |  input             ren;
    |  input             wen;
    |  input  reg [31:0] raddr;
    |  output reg [31:0] rdata;
    |  input  reg [31:0] waddr;
    |  input  reg [31:0] wdata;
    |  input  reg [3:0]  mask;
    |  always @(*) begin
    |    if (ren) pmem_read(raddr, rdata);
    |    else rdata = 32'b0;
    |    if (wen) pmem_write(waddr, wdata, mask);
    |  end
    |endmodule
    """.stripMargin)
}

class traceregs extends HasBlackBoxInline {
  val io = IO(new Bundle {
    val pc = Input(UInt(32.W))
    val rf = Input(Vec(32, UInt(32.W)))
  })
  setInline("traceregs.v",
    s"""
    |import "DPI-C" function void get_regs(input logic [63:0] regs[]);
    |module traceregs(
    |  input [31:0] pc,
    |  input [31:0] rf_0,
    |  input [31:0] rf_1,
    |  input [31:0] rf_2,
    |  input [31:0] rf_3,
    |  input [31:0] rf_4,
    |  input [31:0] rf_5,
    |  input [31:0] rf_6,
    |  input [31:0] rf_7,
    |  input [31:0] rf_8,
    |  input [31:0] rf_9,
    |  input [31:0] rf_10,
    |  input [31:0] rf_11,
    |  input [31:0] rf_12,
    |  input [31:0] rf_13,
    |  input [31:0] rf_14,
    |  input [31:0] rf_15,
    |  input [31:0] rf_16,
    |  input [31:0] rf_17,
    |  input [31:0] rf_18,
    |  input [31:0] rf_19,
    |  input [31:0] rf_20,
    |  input [31:0] rf_21,
    |  input [31:0] rf_22,
    |  input [31:0] rf_23,
    |  input [31:0] rf_24,
    |  input [31:0] rf_25,
    |  input [31:0] rf_26,
    |  input [31:0] rf_27,
    |  input [31:0] rf_28,
    |  input [31:0] rf_29,
    |  input [31:0] rf_30,
    |  input [31:0] rf_31
    |);
    |  wire [31:0] regs [33];
    |  assign regs[0] = rf_0;
    |  assign regs[1] = rf_1;
    |  assign regs[2] = rf_2;
    |  assign regs[3] = rf_3;
    |  assign regs[4] = rf_4;
    |  assign regs[5] = rf_5;
    |  assign regs[6] = rf_6;
    |  assign regs[7] = rf_7;
    |  assign regs[8] = rf_8;
    |  assign regs[9] = rf_9;
    |  assign regs[10] = rf_10;
    |  assign regs[11] = rf_11;
    |  assign regs[12] = rf_12;
    |  assign regs[13] = rf_13;
    |  assign regs[14] = rf_14;
    |  assign regs[15] = rf_15;
    |  assign regs[16] = rf_16;
    |  assign regs[17] = rf_17;
    |  assign regs[18] = rf_18;
    |  assign regs[19] = rf_19;
    |  assign regs[20] = rf_20;
    |  assign regs[21] = rf_21;
    |  assign regs[22] = rf_22;
    |  assign regs[23] = rf_23;
    |  assign regs[24] = rf_24;
    |  assign regs[25] = rf_25;
    |  assign regs[26] = rf_26;
    |  assign regs[27] = rf_27;
    |  assign regs[28] = rf_28;
    |  assign regs[29] = rf_29;
    |  assign regs[30] = rf_30;
    |  assign regs[31] = rf_31;
    |  assign regs[32] = pc;
    |  initial get_regs(regs);
    |endmodule
    """.stripMargin)
}