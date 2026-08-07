# Attach to a QEMU instance started with:
#   qemu-system-aarch64 -machine virt -cpu cortex-a53 -kernel build/kernel.elf -s -S
#
# Try Ctrl+Space after `set `, after `info `, and after a `$` — the register list is AArch64's.
#
# The `python … end` body below is real Python: with a Python plugin installed you get its
# highlighting, completion and error checking inside it. Note also that `contineu` near the bottom
# is no longer coloured like a command, because it is not one.
#
# Two things affect what the Python analysis can resolve in there:
#   * `import gdb` — a banner at the top of this editor sets it up in one click, and Alt+Enter on the
#     import offers the same thing. It writes `gdb.pyi` here and marks this folder as a source root;
#     both are needed, because `gdb` only exists inside GDB's embedded interpreter and because an
#     injected fragment does not see plain sibling files.
#   * builtins such as `super` and `print` — these need a Python interpreter on the module. In IDEA
#     and RustRover that means File > Project Structure > Modules > + > Python (the Settings page for
#     it only appears once a module has the Python facet). Without one, everything reads unresolved.

set architecture aarch64
set disassemble-next-line on
set print pretty on

target extended-remote :1234
file build/kernel.elf
load

define regs
    info registers x0 x1 x2 x3
    printf "pc = %#lx  sp = %#lx\n", $pc, $sp
    x/4i $pc
end

document regs
Show the argument registers and the next four instructions.
end

define bss
    if $_exitcode == 0
        x/16xg &__bss_start
    else
        echo inferior already exited\n
    end
end

python
import gdb
# This body is Python, not GDB syntax — the plugin leaves it alone.
class Reset(gdb.Command):
    def __init__(self):
        super().__init__("reset", gdb.COMMAND_USER)
end

break _start
continue

# ==========================================================================
# Deliberate mistakes. Every line below should be reported.
# ==========================================================================

contineu                    # unknown command -> Alt+Enter offers `continue`
end                         # there is no block open here

define never_closed
    stepi
