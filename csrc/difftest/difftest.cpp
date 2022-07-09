#include "cpu.h"
// =============== Difftest ===============
#ifdef CONFIG_DIFFTEST
// Definations of Ref
enum { DIFFTEST_TO_DUT, DIFFTEST_TO_REF };
void (*ref_difftest_memcpy)(uint32_t addr, void *buf, size_t n, bool direction) = NULL;
void (*ref_difftest_regcpy)(void *dut, bool direction) = NULL;
void (*ref_difftest_exec)(uint32_t n) = NULL;
void (*ref_difftest_raise_intr)(uint32_t NO) = NULL;
uint32_t last_nemu_pc;
void init_difftest()
{
  puts("difftest init start");
  char ref_so_file[] = "/home/wcxmips/ics2020/nemu/build/mips32-nemu-interpreter-so";

  void *handle;
  handle = dlopen(ref_so_file, RTLD_LAZY);
  assert(handle);

  ref_difftest_memcpy = (void (*)(uint32_t addr, void *buf, size_t n, bool direction))(dlsym(handle, "difftest_memcpy"));
  assert(ref_difftest_memcpy);

  ref_difftest_regcpy = (void (*)(void *dut, bool direction))(dlsym(handle, "difftest_regcpy"));
  assert(ref_difftest_regcpy);

  ref_difftest_exec = (void (*)(uint32_t n))(dlsym(handle, "difftest_exec"));
  assert(ref_difftest_exec);

  ref_difftest_raise_intr = (void (*)(uint32_t NO))(dlsym(handle, "difftest_raise_intr"));
  assert(ref_difftest_raise_intr);

  void (*ref_difftest_init)() = (void (*)())(dlsym(handle, "difftest_init"));
  assert(ref_difftest_init);
  puts("difftest init finish");

  uint32_t npc_pc = cpu_gpr[32];
  puts("difftest init finish");

  cpu_gpr[32] = 0xbfc00000;
  last_nemu_pc = cpu_gpr[32];

  ref_difftest_init();
  ref_difftest_memcpy(IMG_START, mem+IMG_START, img_size, DIFFTEST_TO_REF);
  ref_difftest_regcpy(cpu_gpr, DIFFTEST_TO_REF);
  cpu_gpr[32] = npc_pc;
  puts("difftest init finish");
}

void checkregs(uint32_t *ref_regs)
{
  printf("chect at dut_pc = %08x, ref_pc = %08x\n",cpu_gpr[32], ref_regs[32]);
  for (int i = 0; i < 32; ++i) {
    if (ref_regs[i] != cpu_gpr[i]) {
      printf("Error: Difftest failed at reg %d, pc = 0x%08x\n", i, cpu_gpr[32]);
      for (int j = 0; j <= 32; ++j) {
        if (cpu_gpr[j] != ref_regs[j]) printf(COLOR_RED);
        printf("reg %02d: dut = 0x%08x, ref = 0x%08x\n", j, cpu_gpr[j], ref_regs[j]);
        if (cpu_gpr[j] != ref_regs[j]) printf(COLOR_NONE);
      }
      debug_exit(1);
    }
  }
  if (last_nemu_pc != cpu_gpr[32]) {
    printf("Error: Difftest failed at pc, pc = 0x%08x\n", cpu_gpr[32]);
    for (int j = 0; j <= 32; ++j) {
      if (cpu_gpr[j] != ref_regs[j]) printf(COLOR_RED);
      printf("reg %02d: dut = 0x%08x, ref = 0x%08x\n", j, cpu_gpr[j], ref_regs[j]);
      if (cpu_gpr[j] != ref_regs[j]) printf(COLOR_NONE);
    }
    debug_exit(1);
  }
  last_nemu_pc = ref_regs[32];
}

uint32_t ref_regs[33];
void difftest_exec_once()
{
  ref_difftest_exec(1);
  // pc -> ref_regs[32]
  ref_difftest_regcpy(ref_regs, DIFFTEST_TO_DUT);
  checkregs(ref_regs);
}
#endif