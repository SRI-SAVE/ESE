# Exercise Solution Editor (ESE)

The ESE enables the content author to view previously demonstrated action traces and then add
annotations to those action traces in order to define allowed generalizations.

This is the top level of the ESE source tree.

## Build system

To build, run "gradlew assemble" and look for build products in training/aa/build and pal-ui/ese/build.

**Note: The jar files for Lumen and LAPDOG are not included in this repository (see below).
Those would be located in the libs directory. This software will not build without them.**

## Missing libraries

Some libraries have been removed from this distribution:

* SRI libraries, not funded by this project:
  * LAPDOG (required by ESE)
  * Lumen (required by ESE)

## Acknowledgment of support

This work is supported by the Advanced Distributed Learning Initiative (ADL) under Contract
No. W911QY-14-C-0023. The United States Government retains unlimited rights (See
DFARS 252.227-7013 and 7014) to the software and technical data developed under the contract that
are not otherwise designated as "special works" by the Contracting Officer, with "special works"
as defined by DFARS 252.227-7020.

## License

Copyright 2016 SRI International, unless otherwise stated. Licensed under the
[Apache 2.0 License](http://www.apache.org/licenses/LICENSE-2.0).
