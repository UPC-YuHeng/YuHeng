import chisel3._
import chisel3.util._

object InstructionList {
  // Arithmetic Operation
  val ADD     = BitPat("b000000 ????? ????? ????? 00000 100000")
  val ADDI    = BitPat("b001000 ????? ????? ????? ????? ??????")
  val ADDU    = BitPat("b000000 ????? ????? ????? 00000 100001")
  val ADDIU   = BitPat("b001001 ????? ????? ????? ????? ??????")
  val SUB     = BitPat("b000000 ????? ????? ????? 00000 100010")
  val SUBU    = BitPat("b000000 ????? ????? ????? 00000 100011")
  val SLT     = BitPat("b000000 ????? ????? ????? 00000 101010")
  val SLTI    = BitPat("b001010 ????? ????? ????? ????? ??????")
  val SLTU    = BitPat("b000000 ????? ????? ????? 00000 101011")
  val SLTIU   = BitPat("b001011 ????? ????? ????? ????? ??????")
  val DIV     = BitPat("b000000 ????? ????? 00000 00000 011010")
  val DIVU    = BitPat("b000000 ????? ????? 00000 00000 011011")
  val MULT    = BitPat("b000000 ????? ????? 00000 00000 011000")
  val MULTU   = BitPat("b000000 ????? ????? 00000 00000 011001")
  // Logical Operation
  val AND     = BitPat("b000000 ????? ????? ????? 00000 100100")
  val ANDI    = BitPat("b001100 ????? ????? ????? ????? ??????")
  val LUI     = BitPat("b001111 ????? ????? ????? ????? ??????")
  val NOR     = BitPat("b000000 ????? ????? ????? 00000 100111")
  val OR      = BitPat("b000000 ????? ????? ????? 00000 100101")
  val ORI     = BitPat("b001101 ????? ????? ????? ????? ??????")
  val XOR     = BitPat("b000000 ????? ????? ????? 00000 100110")
  val XORI    = BitPat("b001110 ????? ????? ????? ????? ??????")
  // Shift Operation
  val SLLV    = BitPat("b000000 ????? ????? ????? 00000 000100")
  val SLL     = BitPat("b000000 00000 ????? ????? ????? 000000")
  val SRAV    = BitPat("b000000 ????? ????? ????? 00000 000111")
  val SRA     = BitPat("b000000 00000 ????? ????? ????? 000011")
  val SRLV    = BitPat("b000000 ????? ????? ????? 00000 000110")
  val SRL     = BitPat("b000000 00000 ????? ????? ????? 000010")
  // Branch & Jump
  val BEQ     = BitPat("b000100 ????? ????? ????? ????? ??????")
  val BNE     = BitPat("b000101 ????? ????? ????? ????? ??????")
  val BGEZ    = BitPat("b000001 ????? 00001 ????? ????? ??????")
  val BGTZ    = BitPat("b000111 ????? 00000 ????? ????? ??????")
  val BLEZ    = BitPat("b000110 ????? 00000 ????? ????? ??????")
  val BLTZ    = BitPat("b000001 ????? 00000 ????? ????? ??????")
  val BGEZAL  = BitPat("b000001 ????? 10001 ????? ????? ??????")
  val BLTZAL  = BitPat("b000001 ????? 10000 ????? ????? ??????")
  val J       = BitPat("b000010 ????? ????? ????? ????? ??????")
  val JAL     = BitPat("b000011 ????? ????? ????? ????? ??????")
  val JR      = BitPat("b000000 ????? 00000 00000 00000 001000")
  val JALR    = BitPat("b000000 ????? 00000 ????? 00000 001001")
  // Data Transfer
  val MFHI    = BitPat("b000000 00000 00000 ????? 00000 010000")
  val MFLO    = BitPat("b000000 00000 00000 ????? 00000 010010")
  val MTHI    = BitPat("b000000 ????? 00000 00000 00000 010001")
  val MTLO    = BitPat("b000000 ????? 00000 00000 00000 010011")
  // Trap
  val BREAK   = BitPat("b000000 ????? ????? ????? ????? 001101")
  val SYSCALL = BitPat("b000000 ????? ????? ????? ????? 001100")
  // Memory Access
  val LB      = BitPat("b100000 ????? ????? ????? ????? ??????")
  val LBU     = BitPat("b100100 ????? ????? ????? ????? ??????")
  val LH      = BitPat("b100001 ????? ????? ????? ????? ??????")
  val LHU     = BitPat("b100101 ????? ????? ????? ????? ??????")
  val LW      = BitPat("b100011 ????? ????? ????? ????? ??????")
  val SB      = BitPat("b101000 ????? ????? ????? ????? ??????")
  val SH      = BitPat("b101001 ????? ????? ????? ????? ??????")
  val SW      = BitPat("b101011 ????? ????? ????? ????? ??????")
  // Privileged
  val ERET    = BitPat("b010000 10000 00000 00000 00000 011000")
  val MFC0    = BitPat("b010000 00000 ????? ????? 00000 000???")
  val MTC0    = BitPat("b010000 00100 ????? ????? 00000 000???")
  val MUL     = BitPat("b011100 ????? ????? ????? 00000 000010")
  //TLB
  val TLBR    = BitPat("b010000 10000 00000 00000 00000 000001")
  val TLBWI   = BitPat("b010000 10000 00000 00000 00000 000010")
  val TLBP    = BitPat("b010000 10000 00000 00000 00000 001000")
}

object ALUOperationList {
  val alu_adds  = 0.U
  val alu_addu  = 1.U
  val alu_subs  = 2.U
  val alu_subu  = 3.U
  val alu_mults = 4.U
  val alu_multu = 5.U
  val alu_divs  = 6.U
  val alu_divu  = 7.U
  val alu_and   = 8.U
  val alu_xor   = 9.U
  val alu_nor   = 10.U
  val alu_or    = 11.U
  val alu_sftrs = 12.U
  val alu_sftru = 13.U
  val alu_sftl  = 14.U
  val alu_nop   = 15.U
}

object CP0RegisterList {
  val index    = 0.U
  val entrylo0 = 2.U
  val entrylo1 = 3.U
  val entryhi  = 10.U
  val badvaddr = 8.U
  val count    = 9.U
  val status   = 12.U
  val cause    = 13.U
  val epc      = 14.U
}

/******************** contr ********************/

class inst_contr extends Bundle {
  // alu
  val alu_op    = UInt(4.W)
  // mem
  val mem_read  = Bool()
  val mem_write = Bool()
  val mem_mask  = UInt(2.W)
  // reg
  val reg_write = Bool()
  val hi_write  = Bool()
  val lo_write  = Bool()
  val hi_read   = Bool()
  val lo_read   = Bool()
  val hilo_src  = Bool()
  // jump & branch
  val jump      = Bool()
  val jaddr     = UInt(32.W)
  val branch    = Bool()
  val cmp       = UInt(3.W)
  val baddr     = UInt(32.W)
  val link      = Bool()
  // signed / unsigned
  val signed    = Bool()
  // cp0
  val cp0_read  = Bool()
  val cp0_write = Bool()
  // tlb
  val tlbr      = Bool()
  val tlbwi     = Bool()
  val tlbp      = Bool()
}

class conflict_data extends Bundle {
  // conflict
  val rs        = UInt(5.W)
  val rt        = UInt(5.W)
  val rd        = UInt(5.W)
}

/******************** intr ********************/

class inst_intr extends Bundle {
  val instrd    = Bool()
  val datard    = Bool()
  val datawt    = Bool()
  val vaddr     = UInt(32.W)
  val syscall   = Bool()
  val breakpt   = Bool()
  val reserved  = Bool()
  val eret      = Bool()
  val exceed    = Bool()
  val tlbs      = Bool()
  val tlbd      = Bool()
  val tlbl      = Bool()
  val refill    = Bool()
  val tlb_vaddr = UInt(32.W)
  val tlb_vpn2  = UInt(19.W)
}

/******************** reg ********************/

class regread_in extends Bundle {
  val rs = UInt(5.W)
  val rt = UInt(5.W)
}
class regread_out extends Bundle {
  val rs = UInt(32.W)
  val rt = UInt(32.W)
}

/******************** amu ********************/

class ifu_info extends Bundle {
  // pc (to be fetched from)
  val addr   = UInt(32.W)
}
class amu_contr extends Bundle {
  val jump      = Bool()
  val jaddr     = UInt(32.W)
  val branch    = Bool()
  val baddr     = UInt(32.W)
}
class amu_intr extends Bundle {
  val intr   = Bool()
  val eret   = Bool()
  val eaddr  = UInt(32.W)
  val refill = Bool()
}
class amu_in extends Bundle {
  val contr  = new amu_contr()
  val intr   = new amu_intr()
}
class amu_out extends Bundle {
  val data   = new ifu_info()
}

/******************** ifu ********************/

class idu_info extends Bundle {
  // pc (used by baddr, jaddr, linkpc, and intr)
  val pc   = UInt(32.W)
  // inst (for decoding)
  val inst = UInt(32.W)
}
class ifu_in extends Bundle {
  val data = new ifu_info()
}
class ifu_out extends Bundle {
  val data = new idu_info()
  val intr = new inst_intr()
}

/******************** idu ********************/

class exu_info extends Bundle {
  // pc (used for laddr and intr)
  val pc    = UInt(32.W)
  // alu (operation numbers)
  val srca  = UInt(32.W)
  val srcb  = UInt(32.W)
  // reg
  val data  = UInt(32.W)
}
class idu_in extends Bundle {
  val data  = new idu_info()
  val intr  = new inst_intr()
}
class idu_out extends Bundle {
  val data  = new exu_info()
  val contr = new inst_contr()
  val conf  = new conflict_data()
  val intr  = new inst_intr()
}

/******************** exu ********************/

class mem_info extends Bundle {
  // pc (used for laddr and intr)
  val pc   = UInt(32.W)
  // dest
  val dest = UInt(32.W)
  val data = UInt(32.W)
  // reg
  val hi   = UInt(32.W)
  val lo   = UInt(32.W)
}
class exu_in extends Bundle {
  val data  = new exu_info()
  val contr = new inst_contr()
  val conf  = new conflict_data()
  val intr  = new inst_intr()
}
class exu_out extends Bundle {
  val data  = new mem_info()
  val contr = new inst_contr()
  val conf  = new conflict_data()
  val intr  = new inst_intr()
}

/******************** mem ********************/

class reg_info extends Bundle {
  // pc (used for laddr and intr)
  val pc   = UInt(32.W)
  // reg
  val addr = UInt(5.W)
  val data = UInt(32.W)
  val hi   = UInt(32.W)
  val lo   = UInt(32.W)
}
class mem_in extends Bundle {
  val data  = new mem_info()
  val contr = new inst_contr()
  val conf  = new conflict_data()
  val intr  = new inst_intr()
}
class mem_out extends Bundle {
  val data  = new reg_info()
  val contr = new inst_contr()
  val conf  = new conflict_data()
  val intr  = new inst_intr()
}

/******************** reg ********************/

class reg_in extends Bundle {
  val data  = new reg_info()
  val contr = new inst_contr()
  val conf  = new conflict_data()
  val intr  = new inst_intr()
}

/******************** cp0 ********************/

class cp0_contr extends Bundle {
  val write = Bool()
  val tlbr  = Bool()  
  val tlbp  = Bool()  
  val tlbwi = Bool() 
}
class cp0_intr extends Bundle {
  val intr      = Bool()
  val eint      = UInt(6.W)
  val branch    = Bool()
  val addrrd    = Bool()
  val addrwt    = Bool()
  val exceed    = Bool()
  val syscall   = Bool()
  val breakpt   = Bool()
  val reserved  = Bool()
  val eret      = Bool()
  val tlbl      = Bool()
  val tlbs      = Bool()
  val tlbd      = Bool()
  val epc       = UInt(32.W)
  val vaddr     = UInt(32.W)
  val tlb_vaddr = UInt(32.W)
  val tlb_vpn2  = UInt(19.W)
}
class cp0_status extends Bundle {
  val epc      = UInt(32.W)
  val cp0_intr = Bool()
  val exl      = Bool()
  val ie       = Bool()
}

/******************** intr ********************/

class intr_reginfo extends Bundle {
  val rt   = UInt(5.W)
}
class intr_regdata extends Bundle {
  val rt   = UInt(32.W)
}
class intr_cp0info extends Bundle {
  val rd   = UInt(5.W)
  val sel  = UInt(3.W)
  val data = UInt(32.W)
}
class intr_cp0data extends Bundle {
  val data = UInt(32.W)
}

/******************** ram ********************/

class ram_in extends Bundle {
  val en    = Bool()
  val rsize = UInt(3.W)  // 0 -> 1 byte, 1 -> 2 bytes, 2 -> 4 bytes
  val wen   = UInt(4.W)
  val addr  = UInt(32.W)
  val wdata = UInt(32.W)
}

class ram_out extends Bundle {
  val valid = Bool()
  val rdata = UInt(32.W)
}

/******************** debug ********************/

class debug_io extends Bundle {
  val pc       = UInt(32.W)
  val rf_wen   = UInt(4.W)
  val rf_wnum  = UInt(5.W)
  val rf_wdata = UInt(32.W)
}

/******************* Cache ********************/

class axi_in extends Bundle{
  val rd_rdy    = Bool()
  val rd_valid  = Bool()
  val ret_last  = Bool()
  val ret_rdata = UInt(32.W)

  val wr_valid  = Bool()
  val wr_rdy    = Bool()
}

class axi_out extends Bundle{
  val rd_req    =  Bool()
  val rd_len    =  UInt(5.W)
  val rd_addr   =  UInt(32.W)
  val rd_size   =  UInt(3.W)
  val wr_req    =  Bool()
  val wr_len    =  UInt(5.W)
  val wr_addr   =  UInt(32.W)
  val wr_wstrb  =  UInt(4.W)
  val wr_data   =  UInt(256.W)
}


class cpu_in extends Bundle{
  val ucache  =  Bool()
  val valid   =  Bool()
  val op      =  Bool()
  val index   =  UInt(7.W)
  val tag     =  UInt(20.W)
  val offset  =  UInt(5.W)
  val wstrb   =  UInt(4.W)
  val wdata   =  UInt(32.W)
  val rsize   =  UInt(5.W)
}

class cpu_out extends Bundle{
  val addr_ok =  Bool()
  val data_ok =  Bool()
  val rdata   =  UInt(32.W)
}


/******************* TLB ********************/

class tlb_p extends Bundle {
  val vaddr   = UInt(32.W)
  val rd      = Bool()
  val intr_en = Bool()
  val vpn2    = UInt(19.W)
  val odd     = UInt(1.W)
  val asid    = UInt(8.W)
}

class tlb_pout extends Bundle {
  val found = Bool()
  val c     = UInt(3.W)
  val v     = UInt(1.W)
  val d     = UInt(1.W)
  val pfn   = UInt(20.W)
  val index = UInt(5.W)
}

class tlb_r extends Bundle {
  val index = UInt(5.W)
}

class tlb_w extends Bundle {
  val we    = Bool()
  val index = UInt(5.W)
}

class tlb_rw extends Bundle {
  val vpn2 = UInt(19.W)
  val asid = UInt(8.W)
  val c0   = UInt(3.W)
  val v0   = UInt(1.W)
  val d0   = UInt(1.W)
  val g0   = UInt(1.W)
  val pfn0 = UInt(20.W)
  val c1   = UInt(3.W)
  val v1   = UInt(1.W)
  val d1   = UInt(1.W)
  val g1   = UInt(1.W)
  val pfn1 = UInt(20.W)
}

class tlb_intr extends Bundle {
  val intr   = Bool()
  val tlbs   = Bool()
  val tlbd   = Bool()
  val tlbl   = Bool()
  val refill = Bool()
  val vaddr  = UInt(32.W)
  val vpn2   = UInt(19.W)
}

class tlb_contr extends Bundle {
  val tlb_p  = new tlb_p()
  val tlb_r  = new tlb_r()
  val tlb_w  = new tlb_w()
  val tlb_rw = new tlb_rw()
}

class tlb_data extends Bundle {
  val tlb_rw   = new tlb_rw()
  val tlb_pout = new tlb_pout()
}
