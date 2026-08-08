<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Curly Embedded Tools Changelog

## [Unreleased]

## [1.0.0]

### Added

- ARMv8 (AArch64) assembly support for `.s`, `.S`, `.asm` and `.a64`: lexing, parsing, syntax
  highlighting, formatting, completion, inspections and intentions.
- GDB script support for `.gdbinit`, `gdbinit`, `*.gdb` and `*.gdbinit`: command completion, block
  structure checking, folding, and a shipped `gdb` Python stub for injected Python blocks.
- GNU linker script support for `.ld`, `.lds` and `.ldscript`: section and region analysis, with
  navigation from `> REGION` to its `MEMORY` declaration.
