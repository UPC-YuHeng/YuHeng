#include "cpu.h"

#ifdef CONFIG_ITRACE
#if defined(__GNUC__) && !defined(__clang__)
#pragma GCC diagnostic push
#pragma GCC diagnostic ignored "-Wmaybe-uninitialized"
#endif

#include "llvm/MC/MCAsmInfo.h"
#include "llvm/MC/MCContext.h"
#include "llvm/MC/MCDisassembler/MCDisassembler.h"
#include "llvm/MC/MCInstPrinter.h"
#include "llvm/Support/TargetRegistry.h"
#include "llvm/Support/TargetSelect.h"

#if defined(__GNUC__) && !defined(__clang__)
#pragma GCC diagnostic pop
#endif

using namespace llvm;

static llvm::MCDisassembler *gDisassembler = nullptr;
static llvm::MCSubtargetInfo *gSTI = nullptr;
static llvm::MCInstPrinter *gIP = nullptr;

extern "C" void init_disasm(const char *triple)
{
  llvm::InitializeAllTargetInfos();
  llvm::InitializeAllTargetMCs();
  llvm::InitializeAllAsmParsers();
  llvm::InitializeAllDisassemblers();

  std::string errstr;
  std::string gTriple(triple);

  llvm::MCInstrInfo *gMII = nullptr;
  llvm::MCRegisterInfo *gMRI = nullptr;
  auto target = llvm::TargetRegistry::lookupTarget(gTriple, errstr);
  if (!target) {
    llvm::errs() << "Can't find target for " << gTriple << ": " << errstr << "\n";
    assert(0);
  }

  MCTargetOptions MCOptions;
  gSTI = target->createMCSubtargetInfo(gTriple, "", "");
  std::string isa = target->getName();
  if (isa == "riscv32" || isa == "riscv64") {
    gSTI->ApplyFeatureFlag("+m");
    gSTI->ApplyFeatureFlag("+a");
    gSTI->ApplyFeatureFlag("+c");
    gSTI->ApplyFeatureFlag("+f");
    gSTI->ApplyFeatureFlag("+d");
  }
  gMII = target->createMCInstrInfo();
  gMRI = target->createMCRegInfo(gTriple);
  auto AsmInfo = target->createMCAsmInfo(*gMRI, gTriple, MCOptions);
#if LLVM_VERSION_MAJOR >= 13
   auto llvmTripleTwine = Twine(triple);
   auto llvmtriple = llvm::Triple(llvmTripleTwine);
   auto Ctx = new llvm::MCContext(llvmtriple,AsmInfo, gMRI, nullptr);
#else
   auto Ctx = new llvm::MCContext(AsmInfo, gMRI, nullptr);
#endif
  gDisassembler = target->createMCDisassembler(*gSTI, *Ctx);
  gIP = target->createMCInstPrinter(llvm::Triple(gTriple),
      AsmInfo->getAssemblerDialect(), *AsmInfo, *gMII, *gMRI);
  gIP->setPrintImmHex(true);
#if LLVM_VERSION_MAJOR >= 11
  gIP->setPrintBranchImmAsAddress(true);
#endif
}

extern "C" void disassemble(char *str, uint64_t pc, uint8_t *code, int nbyte)
{
  MCInst inst;
  llvm::ArrayRef<uint8_t> arr(code, nbyte);
  uint64_t dummy_size = 0;
  gDisassembler->getInstruction(inst, dummy_size, arr, pc, llvm::nulls());

  std::string s;
  raw_string_ostream os(s);
  gIP->printInst(&inst, pc, "", *gSTI, os);

  int skip = s.find_first_not_of('\t');
  const char *p = s.c_str() + skip;
  strcpy(str, p);
}

// Using IRingBuf as Itrace
// The following content are copied from nemu, until itracer().

#define SIZE_RINGBUF 16
#define LEN_RINGBUF 256
int ring_pos = SIZE_RINGBUF - 1;
char ringbuf[SIZE_RINGBUF][LEN_RINGBUF];

void itrace_record(uint64_t pc)
{
  if (pc < MEM_BASE) return;
  // ring_pos
  if (ring_pos == SIZE_RINGBUF - 1) ring_pos = 0;
  else ring_pos = ring_pos + 1;
  // fetch_inst
  uint8_t *pt = cpu2mem(pc);
  uint8_t *inst = (uint8_t*)malloc(sizeof(uint8_t) * 4);
  for (int i = 0; i < 4; ++i) {
    inst[i] = (*pt++);
  }
  // prepare buffer
  char *p = ringbuf[ring_pos];
  p += sprintf(p, "0x%016lx:", pc);
  for (int i = 0; i < 4; ++i) {
    p += sprintf(p, " %02x", inst[i]);
  }
  p += sprintf(p, "\t");
  // disasm
  disassemble(p, pc, inst, 4);
}

void itrace_output()
{
  printf("========== Itrace Ringbuf ==========\n");
  for (int i = 0; i < SIZE_RINGBUF; ++i) {
    if (ring_pos == i) printf("--->");
    else printf("    ");
    printf("%s\n", ringbuf[i]);
  }
  printf("====================================\n");
}
#endif