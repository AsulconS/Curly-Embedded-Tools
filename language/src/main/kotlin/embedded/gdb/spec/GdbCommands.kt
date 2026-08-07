package com.asulcons.embedded.gdb.spec

/**
 * GDB's command vocabulary, trimmed to what a `.gdbinit` for a QEMU + `gdb-multiarch` session actually
 * uses, plus the abbreviations everybody types.
 */
object GdbCommands {

    /** Top-level commands and the abbreviations GDB accepts for them. */
    val COMMANDS: Map<String, String> = mapOf(
        "add-symbol-file" to "Load extra symbols from a file at a given text address.",
        "advance" to "Continue until a location is reached, without recursing.",
        "append" to "Append target memory or an expression's value to a file.",
        "apropos" to "Search command documentation for a regular expression.",
        "attach" to "Attach to a running process.",
        "awatch" to "Set a watchpoint that triggers on read or write.",
        "b" to "Set a breakpoint. Short for `break`.",
        "backtrace" to "Print the call stack.",
        "break" to "Set a breakpoint at a location.",
        "bt" to "Print the call stack. Short for `backtrace`.",
        "c" to "Continue execution. Short for `continue`.",
        "call" to "Call a function in the inferior and print its result.",
        "catch" to "Set a catchpoint for an event such as a syscall or signal.",
        "cd" to "Change GDB's working directory.",
        "clear" to "Delete breakpoints at a location.",
        "condition" to "Attach a condition to a breakpoint.",
        "continue" to "Resume the program.",
        "core-file" to "Read a core dump.",
        "d" to "Delete breakpoints. Short for `delete`.",
        "delete" to "Delete breakpoints, watchpoints or display expressions.",
        "detach" to "Detach from the target, leaving it running.",
        "directory" to "Add a directory to the source search path.",
        "disable" to "Disable breakpoints.",
        "disassemble" to "Disassemble a function or address range.",
        "disconnect" to "Disconnect from the remote target.",
        "display" to "Print an expression every time the program stops.",
        "down" to "Move down the stack toward the callee.",
        "dprintf" to "Set a breakpoint that prints and continues.",
        "dump" to "Write target memory to a file.",
        "echo" to "Print text; `\\n` produces a newline.",
        "enable" to "Enable breakpoints.",
        "eval" to "Format a string and execute it as a command.",
        "file" to "Choose the executable whose symbols GDB should use.",
        "finish" to "Run until the selected frame returns.",
        "flash-erase" to "Erase the target's flash memory.",
        "frame" to "Select a stack frame.",
        "handle" to "Change how GDB reacts to a signal.",
        "help" to "Describe a command.",
        "i" to "Show information. Short for `info`.",
        "info" to "Show information about the program or GDB's own state.",
        "init-if-undefined" to "Set a convenience variable only if it is not already set.",
        "inspect" to "Print an expression. A synonym for `print`.",
        "jump" to "Continue execution at a different address.",
        "kill" to "Kill the program being debugged.",
        "l" to "List source lines. Short for `list`.",
        "layout" to "Choose a TUI window layout, e.g. `layout asm` or `layout regs`.",
        "list" to "List source lines.",
        "load" to "Download the executable's sections to the remote target.",
        "macro" to "Inspect or define C preprocessor macros.",
        "maintenance" to "GDB maintenance commands.",
        "make" to "Run `make`.",
        "monitor" to "Send a command straight to the remote stub, e.g. `monitor reset`.",
        "n" to "Step over one source line. Short for `next`.",
        "next" to "Step over one source line.",
        "nexti" to "Step over one machine instruction.",
        "ni" to "Step over one machine instruction. Short for `nexti`.",
        "output" to "Print an expression without a newline or value-history entry.",
        "p" to "Print an expression. Short for `print`.",
        "path" to "Add a directory to the object-file search path.",
        "print" to "Print an expression; `/x` and friends choose the format.",
        "printf" to "Print a formatted string, C style.",
        "ptype" to "Print the definition of a type.",
        "pwd" to "Print GDB's working directory.",
        "quit" to "Leave GDB.",
        "r" to "Run the program. Short for `run`.",
        "rbreak" to "Set breakpoints on every function matching a regular expression.",
        "remote" to "Remote-protocol subcommands.",
        "return" to "Pop the selected frame without executing it.",
        "reverse-continue" to "Continue backwards (record/replay targets).",
        "reverse-step" to "Step backwards one source line.",
        "run" to "Start the program.",
        "s" to "Step one source line. Short for `step`.",
        "search" to "Search source lines for a regular expression.",
        "select-frame" to "Select a frame without printing it.",
        "set" to "Change a GDB parameter or an expression's value.",
        "shell" to "Run a shell command.",
        "show" to "Display the value of a GDB parameter.",
        "si" to "Step one machine instruction. Short for `stepi`.",
        "signal" to "Continue, delivering a signal.",
        "source" to "Execute the commands in a file.",
        "start" to "Run the program, stopping at `main`.",
        "starti" to "Run the program, stopping at the first instruction.",
        "step" to "Step one source line, entering calls.",
        "stepi" to "Step one machine instruction.",
        "symbol-file" to "Load symbols from a file without changing the executable.",
        "target" to "Connect to a target, e.g. `target remote :1234`.",
        "tbreak" to "Set a breakpoint that is deleted once hit.",
        "thread" to "Select or inspect a thread.",
        "tui" to "Text UI subcommands, e.g. `tui enable`.",
        "u" to "Continue until past the current line. Short for `until`.",
        "undisplay" to "Remove a display expression.",
        "until" to "Continue until a location past the current line.",
        "up" to "Move up the stack toward the caller.",
        "watch" to "Set a watchpoint that triggers when an expression changes.",
        "whatis" to "Print an expression's type.",
        "where" to "Print the call stack. A synonym for `backtrace`.",
        "x" to "Examine memory, e.g. `x/16xw ${'$'}sp`.",
        // Script-only commands, which the parser also treats as block keywords.
        "define" to "Define a new command, closed by `end`.",
        "document" to "Attach help text to a user-defined command, closed by `end`.",
        "python" to "Run a block of Python, closed by `end`.",
        "py" to "Run a block of Python, closed by `end`.",
        "commands" to "Attach commands to a breakpoint, closed by `end`.",
        "end" to "Close the innermost block.",
        "if" to "Conditional block, closed by `end`.",
        "else" to "Alternative branch of an `if` block.",
        "while" to "Loop block, closed by `end`.",
        "loop_break" to "Leave the innermost `while` loop.",
        "loop_continue" to "Start the next iteration of the innermost `while` loop.",
        "!" to "Run a shell command.",
    )

    /** Second words that complete a command; used to validate and complete `set foo`, `info bar`, … */
    val SUBCOMMANDS: Map<String, List<String>> = mapOf(
        "set" to listOf(
            "architecture", "args", "auto-load", "backtrace", "breakpoint", "charset", "confirm",
            "disassemble-next-line", "disassembly-flavor", "endian", "environment", "follow-fork-mode",
            "height", "history", "language", "listsize", "logging", "osabi", "output-radix", "pagination",
            "print", "prompt", "radix", "remote", "remotetimeout", "scheduler-locking", "solib-search-path",
            "step-mode", "substitute-path", "sysroot", "tui", "var", "variable", "verbose", "width", "write",
        ),
        "show" to listOf(
            "architecture", "args", "convenience", "directories", "endian", "environment", "language",
            "osabi", "paths", "print", "radix", "remote", "version",
        ),
        "info" to listOf(
            "address", "all-registers", "args", "breakpoints", "display", "files", "frame", "functions",
            "line", "locals", "mem", "proc", "registers", "scope", "sharedlibrary", "signals", "source",
            "sources", "stack", "symbol", "target", "threads", "types", "variables", "watchpoints",
        ),
        "target" to listOf(
            "core", "exec", "extended-remote", "native", "record", "remote", "sim", "tfile",
        ),
        "layout" to listOf("asm", "next", "prev", "regs", "split", "src"),
        "tui" to listOf("disable", "enable", "focus", "new-layout", "reg"),
        "maintenance" to listOf("info", "packet", "print", "set", "show"),
        "thread" to listOf("apply", "find", "name"),
        "record" to listOf("btrace", "delete", "full", "stop"),
    )

    /** `$`-prefixed names that mean something to GDB on an AArch64 target. */
    val CONVENIENCE_VARIABLES: List<String> = buildList {
        add("\$pc")
        add("\$sp")
        add("\$fp")
        add("\$lr")
        add("\$cpsr")
        add("\$pstate")
        add("\$_exitcode")
        add("\$_siginfo")
        add("\$_thread")
        add("\$_gthread")
        add("\$_inferior")
        add("\$_")
        add("\$__")
        for (index in 0..30) {
            add("\$x$index")
            add("\$w$index")
        }
        for (index in 0..31) {
            add("\$v$index")
            add("\$d$index")
            add("\$s$index")
            add("\$q$index")
        }
    }

    /** Format letters accepted after `/` by `x`, `print` and `display`. */
    val FORMAT_LETTERS: String = "oxdutfaicsz" + "bhwg"

    private val ALL_NAMES: Set<String> = COMMANDS.keys

    fun isKnownCommand(name: String): Boolean = name in ALL_NAMES

    fun describe(name: String): String? = COMMANDS[name]

    fun subcommandsOf(name: String): List<String> = SUBCOMMANDS[name].orEmpty()
}
