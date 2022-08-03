import chisel3._
import chisel3.util._

class mult extends Module {
  class mult_in extends Bundle {
    val valid  = Bool()
    val x = UInt(32.W)
    val y = UInt(32.W)
    val signed = Bool()
  }
  class mult_out extends Bundle {
    val valid  = Bool()
    val h = UInt(32.W)
    val l = UInt(32.W)
  }

  val io = IO(new Bundle {
    val in  = Input(new mult_in())
    val out = Output(new mult_out())
  })
  val idle :: startmul :: multiply :: endmul :: nullm = Enum(5)

  val mstate    = RegInit(idle)
  val srca      = RegInit(0.U(68.W))
  val srcb      = RegInit(0.U(34.W))
  val out       = RegInit(Reg(new mult_out()))
  val src_p_reg = RegInit(0.U(1156.W))
  val src_c_reg = RegInit(0.U(17.W))

  io.out.valid  := (mstate === endmul)
  io.out.h      := out.h
  io.out.l      := out.l

  switch(mstate){
    is(idle){
      when(io.in.valid)
      {
        mstate := startmul
        srca := Mux(io.in.signed, Cat(Fill(36, io.in.x(31)), io.in.x), io.in.x)
        srcb := Mux(io.in.signed, Cat(Fill(2, io.in.y(31)), io.in.y), io.in.y)
      }
    }
    is(startmul){
      val booth   = VecInit(Seq.fill(17)(Module(new booth()).io))
      val src_p   = Wire(Vec(17, UInt(68.W)))
      val src_c   = Wire(Vec(17, UInt(1.W)))
      val src_p_t = Wire(Vec(1156, UInt(1.W)))

      mstate     := multiply
      booth(0).y := Cat(srcb(1), srcb(0), 0.U(1.W))
      booth(0).x := srca
      src_p(0)   := booth(0).p
      src_c(0)   := booth(0).c

      for(j <- 1 to 16)
      {
        booth(j).y := srcb(2 * j + 1, 2 * j - 1)
        booth(j).x := srca << (j + j)
        src_p(j)   := booth(j).p
        src_c(j)   := booth(j).c
      }

      for(i <- 0 to 67)
      {
        for(j <- 0 to 16)
        {
          src_p_t(i * 17 + j) := src_p(j)(i)
        }
      }

      src_p_reg := src_p_t.asUInt
      src_c_reg := src_c.asUInt
    }
    is(multiply){
      val switch_mul = Module(new switch_mul())
      val walloc = VecInit(Seq.fill(68)(Module(new walloc_17bits()).io))
      val add_a   = Wire(Vec(68, UInt(1.W)))
      val add_b   = Wire(Vec(68, UInt(1.W)))
      val ans     = Wire(UInt(64.W))

      switch_mul.io.src_p := src_p_reg
      switch_mul.io.src_c := src_c_reg
      walloc(0).src_in    := switch_mul.io.out_in(0)
      walloc(0).cin       := switch_mul.io.out_s(13, 0)
      add_b(0)            := walloc(0).s
      add_a(1)            := walloc(0).cout

      walloc(67).src_in := switch_mul.io.out_in(67)
      walloc(67).cin    := walloc(0).cout_group
      add_b(67)         := walloc(67).s

      add_a(0) := switch_mul.io.out_s(14)
      for(j <- 1 to 66)
      {
        walloc(j).src_in := switch_mul.io.out_in(j)
        walloc(j).cin    := walloc(j - 1).cout_group
        add_a(j + 1)     := walloc(j).cout
        add_b(j)         := walloc(j).s
      }

      ans    := (add_a.asUInt + add_b.asUInt + switch_mul.io.out_s(15) + switch_mul.io.out_s(16))(63,0)
      out.h  := ans(63, 32)
      out.l  := ans(31, 0)
      mstate := endmul
    }
    is(endmul){
      mstate := idle
    }
  }
}

class switch_mul extends Module {
  val io = IO(new Bundle {
    val src_p  = Input(UInt(1156.W))
    val src_c  = Input(UInt(17.W))
    val out_in = Output(Vec(68, UInt(17.W)))
    val out_s  = Output(UInt(17.W))
  })
  for(j <- 0 to 67)
    io.out_in(j) := io.src_p(17 * j + 16, 17 * j)
  io.out_s := io.src_c
}

class walloc_17bits extends HasBlackBoxInline {
  val io = IO(new Bundle {
    val src_in     = Input(UInt(17.W))
    val cin        = Input(UInt(14.W))
    val cout_group = Output(UInt(14.W))
    val cout       = Output(UInt(1.W))
    val s          = Output(UInt(1.W))
  })
  setInline("walloc_17bits.v",
    s"""
    |module walloc_17bits(
    |    input [16:0] src_in,
    |    input [13:0]  cin,
    |    output [13:0] cout_group,
    |    output      cout,s
    |);
    |wire [13:0] c;
    |///////////////first////////////////
    |wire [4:0] first_s;
    |csa csa0 (.in (src_in[16:14]), .cout (c[4]), .s (first_s[4]) );
    |csa csa1 (.in (src_in[13:11]), .cout (c[3]), .s (first_s[3]) );
    |csa csa2 (.in (src_in[10:08]), .cout (c[2]), .s (first_s[2]) );
    |csa csa3 (.in (src_in[07:05]), .cout (c[1]), .s (first_s[1]) );
    |csa csa4 (.in (src_in[04:02]), .cout (c[0]), .s (first_s[0]) );
    |
    |///////////////secnod//////////////
    |wire [3:0] secnod_s;
    |csa csa5 (.in ({first_s[4:2]}             ), .cout (c[8]), .s (secnod_s[3]));
    |csa csa6 (.in ({first_s[1:0],src_in[1]}   ), .cout (c[7]), .s (secnod_s[2]));
    |csa csa7 (.in ({src_in[0],cin[4:3]}       ), .cout (c[6]), .s (secnod_s[1]));
    |csa csa8 (.in ({cin[2:0]}                 ), .cout (c[5]), .s (secnod_s[0]));
    |
    |//////////////thrid////////////////
    |wire [1:0] thrid_s;
    |csa csa9 (.in (secnod_s[3:1]          ), .cout (c[10]), .s (thrid_s[1]));
    |csa csaA (.in ({secnod_s[0],cin[6:5]} ), .cout (c[09]), .s (thrid_s[0]));
    |
    |//////////////fourth////////////////
    |wire [1:0] fourth_s;
    |
    |csa csaB (.in ({thrid_s[1:0],cin[10]} ),  .cout (c[12]), .s (fourth_s[1]));
    |csa csaC (.in ({cin[9:7]             }),  .cout (c[11]), .s (fourth_s[0]));
    |
    |//////////////fifth/////////////////
    |wire fifth_s;
    |
    |csa csaD (.in ({fourth_s[1:0],cin[11]}),  .cout (c[13]), .s (fifth_s));
    |
    |///////////////sixth///////////////
    |csa csaE (.in ({fifth_s,cin[13:12]}   ),  .cout (cout),  .s  (s));
    |
    |///////////////output///////////////
    |assign cout_group = c;
    |endmodule
    |module csa(
    |  input [2:0] in,
    |  output cout,s
    |
    |);
    |wire a,b,cin;
    |assign a=in[2];
    |assign b=in[1];
    |assign cin=in[0];
    |assign s = a ^ b ^ cin;
    |assign cout = a & b | b & cin | a & cin;
    |endmodule
    """.stripMargin)
}

class booth extends Module {
  val io = IO(new Bundle{
    val y = Input(UInt(3.W))
    val x = Input(UInt(68.W))
    val c = Output(UInt(1.W))
    val p = Output(UInt(68.W))
  })
  
  io.p := MuxLookup(io.y, 0.U, Array(
    0.U -> 0.U,
    1.U -> io.x,
    2.U -> io.x,
    3.U -> (io.x << 1),
    4.U -> ~(io.x << 1),
    5.U -> ~io.x, 
    6.U -> ~io.x,
    7.U -> 0.U
  ))
  io.c := MuxLookup(io.y, 0.U, Array(
    4.U -> 1.U,
    5.U -> 1.U,
    6.U -> 1.U
  ))
}
