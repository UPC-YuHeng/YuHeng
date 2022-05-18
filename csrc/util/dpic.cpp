#include "cpu.h"

// =============== Memory ===============
long long img_size = 0;
uint8_t mem[MEM_SIZE];
// Memory transfer
uint8_t* cpu2mem(ll addr) { return mem + (addr - MEM_BASE); }

// =============== DPI-C ===============

// Memory Read
extern "C" void pmem_read(int raddr, int *rdata)
{
  if (raddr < MEM_BASE) return;
  uint8_t *pt = cpu2mem(raddr) + 7;
  ll ret = 0;
  for (int i = 0; i < 8; ++i) {
    ret = (ret << 8) | (*pt--);
  }
  *rdata = ret;
}

// Memory Write
extern "C" void pmem_write(int waddr, int wdata, char mask)
{
  if (waddr < MEM_BASE) return;
  uint8_t *pt = cpu2mem(waddr);
  for (int i = 0; i < 8; ++i) {
    if (mask & 1) *pt = (wdata & 0xff);
    wdata >>= 8, mask >>= 1, pt++;
  }
}

// Get Registers
uint64_t *cpu_gpr = NULL;
extern "C" void get_regs(const svOpenArrayHandle r)
{
  cpu_gpr = (uint64_t *)(((VerilatedDpiOpenVar*)r) -> datap());
}

// Ebreak
void ebreak()
{
  debug_exit(cpu_gpr[10]);
}