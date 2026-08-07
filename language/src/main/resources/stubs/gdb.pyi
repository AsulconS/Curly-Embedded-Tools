"""
Type stubs for GDB's built-in `gdb` module.

`gdb` is not installable: it exists only inside the Python interpreter that GDB embeds, so no plugin
and no interpreter can ever resolve `import gdb` on its own. Dropping this file next to a script that
uses it — or into any directory marked as a Sources Root — is what makes the import resolve and gives
completion on `gdb.*`.

Covers the API a `.gdbinit` realistically touches, not the whole surface. Signatures follow the GDB
Python API reference; add to it as needed.
"""

from typing import Any, Callable, Iterator, Sequence

VERSION: str
HOST_CONFIG: str
TARGET_CONFIG: str
PYTHONDIR: str

# --- Output streams -------------------------------------------------------------------------------
STDOUT: int
STDERR: int
STDLOG: int

# --- Command categories and completion classes ----------------------------------------------------
COMMAND_NONE: int
COMMAND_RUNNING: int
COMMAND_DATA: int
COMMAND_STACK: int
COMMAND_FILES: int
COMMAND_SUPPORT: int
COMMAND_STATUS: int
COMMAND_BREAKPOINTS: int
COMMAND_TRACEPOINTS: int
COMMAND_USER: int
COMMAND_OBSCURE: int
COMMAND_MAINTENANCE: int

COMPLETE_NONE: int
COMPLETE_FILENAME: int
COMPLETE_LOCATION: int
COMPLETE_COMMAND: int
COMPLETE_SYMBOL: int
COMPLETE_EXPRESSION: int

# --- Parameter types ------------------------------------------------------------------------------
PARAM_BOOLEAN: int
PARAM_AUTO_BOOLEAN: int
PARAM_UINTEGER: int
PARAM_INTEGER: int
PARAM_STRING: int
PARAM_STRING_NOESCAPE: int
PARAM_OPTIONAL_FILENAME: int
PARAM_FILENAME: int
PARAM_ZINTEGER: int
PARAM_ZUINTEGER: int
PARAM_ZUINTEGER_UNLIMITED: int
PARAM_ENUM: int

# --- Breakpoint and watchpoint kinds --------------------------------------------------------------
BP_NONE: int
BP_BREAKPOINT: int
BP_HARDWARE_BREAKPOINT: int
BP_WATCHPOINT: int
BP_HARDWARE_WATCHPOINT: int
BP_READ_WATCHPOINT: int
BP_ACCESS_WATCHPOINT: int
BP_CATCHPOINT: int

WP_READ: int
WP_WRITE: int
WP_ACCESS: int

# --- Frame kinds ----------------------------------------------------------------------------------
NORMAL_FRAME: int
DUMMY_FRAME: int
INLINE_FRAME: int
TAILCALL_FRAME: int
SIGTRAMP_FRAME: int
ARCH_FRAME: int
SENTINEL_FRAME: int

FRAME_UNWIND_NO_REASON: int
FRAME_UNWIND_NULL_ID: int
FRAME_UNWIND_OUTERMOST: int
FRAME_UNWIND_UNAVAILABLE: int
FRAME_UNWIND_INNER_ID: int
FRAME_UNWIND_SAME_ID: int
FRAME_UNWIND_NO_SAVED_PC: int
FRAME_UNWIND_MEMORY_ERROR: int

# --- Symbol domains and type codes ----------------------------------------------------------------
SYMBOL_UNDEF_DOMAIN: int
SYMBOL_VAR_DOMAIN: int
SYMBOL_STRUCT_DOMAIN: int
SYMBOL_LABEL_DOMAIN: int
SYMBOL_MODULE_DOMAIN: int
SYMBOL_COMMON_BLOCK_DOMAIN: int

TYPE_CODE_PTR: int
TYPE_CODE_ARRAY: int
TYPE_CODE_STRUCT: int
TYPE_CODE_UNION: int
TYPE_CODE_ENUM: int
TYPE_CODE_FLAGS: int
TYPE_CODE_FUNC: int
TYPE_CODE_INT: int
TYPE_CODE_FLT: int
TYPE_CODE_VOID: int
TYPE_CODE_BOOL: int
TYPE_CODE_CHAR: int
TYPE_CODE_TYPEDEF: int
TYPE_CODE_REF: int

# --- Errors ---------------------------------------------------------------------------------------
class error(RuntimeError): ...
class GdbError(Exception): ...
class MemoryError(error): ...

# --- Core objects ---------------------------------------------------------------------------------
class Value:
    address: "Value"
    is_optimized_out: bool
    type: "Type"
    dynamic_type: "Type"
    is_lazy: bool

    def __init__(self, value: Any, type: "Type" = ...) -> None: ...
    def cast(self, type: "Type") -> "Value": ...
    def dereference(self) -> "Value": ...
    def referenced_value(self) -> "Value": ...
    def dynamic_cast(self, type: "Type") -> "Value": ...
    def reinterpret_cast(self, type: "Type") -> "Value": ...
    def string(self, encoding: str = ..., errors: str = ..., length: int = ...) -> str: ...
    def lazy_string(self, encoding: str = ..., length: int = ...) -> "LazyString": ...
    def fetch_lazy(self) -> None: ...
    def format_string(self, **kwargs: Any) -> str: ...
    def __getitem__(self, key: Any) -> "Value": ...
    def __int__(self) -> int: ...
    def __float__(self) -> float: ...

class Type:
    alignof: int
    code: int
    name: str | None
    sizeof: int
    tag: str | None

    def fields(self) -> list["Field"]: ...
    def array(self, n1: int, n2: int = ...) -> "Type": ...
    def const(self) -> "Type": ...
    def volatile(self) -> "Type": ...
    def unqualified(self) -> "Type": ...
    def strip_typedefs(self) -> "Type": ...
    def target(self) -> "Type": ...
    def pointer(self) -> "Type": ...
    def reference(self) -> "Type": ...
    def template_argument(self, n: int, block: "Block" = ...) -> "Type": ...

class Field:
    bitpos: int
    bitsize: int
    name: str | None
    type: Type
    artificial: bool
    is_base_class: bool
    parent_type: Type

class LazyString:
    address: int
    length: int
    encoding: str
    type: Type
    def value(self) -> Value: ...

class Symbol:
    name: str
    linkage_name: str
    print_name: str
    addr_class: int
    type: Type | None
    symtab: "Symtab"
    line: int
    is_argument: bool
    is_constant: bool
    is_function: bool
    is_variable: bool
    def value(self, frame: "Frame" = ...) -> Value: ...

class Symtab:
    filename: str
    objfile: "Objfile"
    producer: str
    def fullname(self) -> str: ...
    def is_valid(self) -> bool: ...
    def global_block(self) -> "Block": ...
    def static_block(self) -> "Block": ...
    def linetable(self) -> Any: ...

class Symtab_and_line:
    symtab: Symtab
    pc: int
    last: int
    line: int
    def is_valid(self) -> bool: ...

class Block:
    start: int
    end: int
    function: Symbol | None
    superblock: "Block | None"
    global_block: "Block"
    static_block: "Block"
    is_global: bool
    is_static: bool
    def is_valid(self) -> bool: ...
    def __iter__(self) -> Iterator[Symbol]: ...

class Frame:
    def is_valid(self) -> bool: ...
    def name(self) -> str | None: ...
    def architecture(self) -> "Architecture": ...
    def type(self) -> int: ...
    def unwind_stop_reason(self) -> int: ...
    def pc(self) -> int: ...
    def block(self) -> Block: ...
    def function(self) -> Symbol | None: ...
    def older(self) -> "Frame | None": ...
    def newer(self) -> "Frame | None": ...
    def find_sal(self) -> Symtab_and_line: ...
    def read_register(self, register: str) -> Value: ...
    def read_var(self, variable: str | Symbol, block: Block = ...) -> Value: ...
    def select(self) -> None: ...

class Architecture:
    def name(self) -> str: ...
    def disassemble(self, start_pc: int, end_pc: int = ..., count: int = ...) -> list[dict[str, Any]]: ...
    def registers(self, reggroup: str = ...) -> Any: ...

class Inferior:
    num: int
    pid: int
    was_attached: bool
    progspace: "Progspace"
    def is_valid(self) -> bool: ...
    def threads(self) -> tuple["InferiorThread", ...]: ...
    def architecture(self) -> Architecture: ...
    def read_memory(self, address: int, length: int) -> memoryview: ...
    def write_memory(self, address: int, buffer: Any, length: int = ...) -> None: ...
    def search_memory(self, address: int, length: int, pattern: Any) -> int | None: ...

class InferiorThread:
    name: str | None
    num: int
    global_num: int
    ptid: tuple[int, int, int]
    inferior: Inferior
    def is_valid(self) -> bool: ...
    def switch(self) -> None: ...
    def is_stopped(self) -> bool: ...
    def is_running(self) -> bool: ...
    def is_exited(self) -> bool: ...

class Objfile:
    filename: str | None
    username: str | None
    owner: "Objfile | None"
    build_id: str | None
    progspace: "Progspace"
    pretty_printers: list[Any]
    def is_valid(self) -> bool: ...
    def add_separate_debug_file(self, filename: str) -> None: ...
    def lookup_global_symbol(self, name: str, domain: int = ...) -> Symbol | None: ...
    def lookup_static_symbol(self, name: str, domain: int = ...) -> Symbol | None: ...

class Progspace:
    filename: str | None
    pretty_printers: list[Any]
    def objfiles(self) -> list[Objfile]: ...
    def block_for_pc(self, pc: int) -> Block | None: ...
    def find_pc_line(self, pc: int) -> Symtab_and_line: ...
    def is_valid(self) -> bool: ...

# --- Extensible bases: subclass these in a `python … end` block ------------------------------------
class Command:
    def __init__(
        self,
        name: str,
        command_class: int,
        completer_class: int = ...,
        prefix: bool = ...,
    ) -> None: ...
    def invoke(self, argument: str, from_tty: bool) -> None: ...
    def complete(self, text: str, word: str) -> int | Sequence[str]: ...
    def dont_repeat(self) -> None: ...

class Parameter:
    value: Any
    set_doc: str
    show_doc: str
    def __init__(
        self,
        name: str,
        command_class: int,
        parameter_class: int,
        enum_sequence: Sequence[str] = ...,
    ) -> None: ...
    def get_set_string(self) -> str: ...
    def get_show_string(self, svalue: str) -> str: ...

class Function:
    def __init__(self, name: str) -> None: ...
    def invoke(self, *args: Value) -> Value: ...

class Breakpoint:
    enabled: bool
    silent: bool
    pending: bool
    thread: int | None
    task: int | None
    ignore_count: int
    number: int
    type: int
    visible: bool
    temporary: bool
    hit_count: int
    location: str | None
    expression: str | None
    condition: str | None
    commands: str | None

    def __init__(
        self,
        spec: str = ...,
        type: int = ...,
        wp_class: int = ...,
        internal: bool = ...,
        temporary: bool = ...,
        qualified: bool = ...,
    ) -> None: ...
    def stop(self) -> bool: ...
    def is_valid(self) -> bool: ...
    def delete(self) -> None: ...

class FinishBreakpoint(Breakpoint):
    return_value: Value | None
    def __init__(self, frame: Frame = ..., internal: bool = ...) -> None: ...
    def out_of_scope(self) -> None: ...

# --- Events ---------------------------------------------------------------------------------------
class EventRegistry:
    def connect(self, object: Callable[..., Any]) -> None: ...
    def disconnect(self, object: Callable[..., Any]) -> None: ...

class _Events:
    cont: EventRegistry
    exited: EventRegistry
    stop: EventRegistry
    new_objfile: EventRegistry
    free_objfile: EventRegistry
    clear_objfiles: EventRegistry
    new_inferior: EventRegistry
    inferior_deleted: EventRegistry
    new_thread: EventRegistry
    inferior_call: EventRegistry
    memory_changed: EventRegistry
    register_changed: EventRegistry
    breakpoint_created: EventRegistry
    breakpoint_modified: EventRegistry
    breakpoint_deleted: EventRegistry
    before_prompt: EventRegistry

events: _Events

# --- Module-level functions -----------------------------------------------------------------------
def execute(command: str, from_tty: bool = ..., to_string: bool = ...) -> str | None: ...
def breakpoints() -> tuple[Breakpoint, ...]: ...
def parameter(parameter: str) -> Any: ...
def set_parameter(name: str, value: Any) -> None: ...
def history(number: int) -> Value: ...
def convenience_variable(name: str) -> Value | None: ...
def set_convenience_variable(name: str, value: Any) -> None: ...
def parse_and_eval(expression: str) -> Value: ...
def find_pc_line(pc: int) -> Symtab_and_line: ...
def post_event(event: Callable[[], Any]) -> None: ...
def write(string: str, stream: int = ...) -> None: ...
def flush(stream: int = ...) -> None: ...
def target_charset() -> str: ...
def target_wide_charset() -> str: ...
def solib_name(address: int) -> str | None: ...
def decode_line(expression: str = ...) -> tuple[str | None, tuple[Symtab_and_line, ...] | None]: ...
def lookup_type(name: str, block: Block = ...) -> Type: ...
def lookup_symbol(name: str, block: Block = ..., domain: int = ...) -> tuple[Symbol | None, bool]: ...
def lookup_global_symbol(name: str, domain: int = ...) -> Symbol | None: ...
def lookup_static_symbol(name: str, domain: int = ...) -> Symbol | None: ...
def lookup_objfile(name: str, by_build_id: bool = ...) -> Objfile: ...
def block_for_pc(pc: int) -> Block | None: ...
def current_objfile() -> Objfile | None: ...
def objfiles() -> list[Objfile]: ...
def progspaces() -> list[Progspace]: ...
def current_progspace() -> Progspace | None: ...
def selected_frame() -> Frame: ...
def newest_frame() -> Frame: ...
def selected_thread() -> InferiorThread: ...
def selected_inferior() -> Inferior: ...
def inferiors() -> tuple[Inferior, ...]: ...
def string_to_argv(argstr: str) -> list[str]: ...

prompt_hook: Callable[[str], str] | None
